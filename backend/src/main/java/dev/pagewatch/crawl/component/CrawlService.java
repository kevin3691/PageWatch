package dev.pagewatch.crawl.component;

import dev.pagewatch.common.web.ApiException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Selector;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import static dev.pagewatch.monitor.model.Models.*;

@Component
public class CrawlService {
    private static final int MAX_BYTES = 2_000_000;
    private static final Set<String> BLOCKS = Set.of("p", "div", "section", "article", "main", "li", "ul", "ol", "h1", "h2", "h3", "h4", "h5", "h6", "tr", "blockquote", "pre", "br", "hr");
    private final UrlPolicy policy;
    private final DemoSource demo;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public CrawlService(UrlPolicy policy, DemoSource demo) { this.policy = policy; this.demo = demo; }

    public Preview fetch(PreviewRequest request) {
        if (DemoSource.URL.equals(request.url().strip()))
            return extract(demo.html(), request.url(), request.selector(), request.ignoreSelector());
        URI uri = policy.validate(request.url());
        try {
            for (int redirects = 0; redirects <= 5; redirects++) {
                var req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "PageWatch/0.1 (+personal webpage change monitor)")
                        .header("Accept", "text/html,application/xhtml+xml,text/plain;q=0.9")
                        .GET().build();
                // A bounded body subscriber also keeps a slow/oversized response inside the request timeout.
                var response = client.send(req, ignored -> new LimitedBodySubscriber(MAX_BYTES));
                int code = response.statusCode();
                if (code >= 300 && code < 400) {
                    String location = response.headers().firstValue("Location").orElseThrow(() -> ApiException.badRequest("网站重定向没有提供目标地址"));
                    uri = policy.validate(uri.resolve(location).toString());
                    continue;
                }
                if (code < 200 || code >= 300) throw ApiException.badRequest("网站返回 HTTP " + code + "，暂时无法读取正文");
                String type = response.headers().firstValue("Content-Type").orElse("text/html");
                if (!(type.contains("text/html") || type.contains("application/xhtml+xml") || type.contains("text/plain")))
                    throw ApiException.badRequest("这个链接不是 HTML 或文本网页");
                byte[] body = response.body();
                var doc = parseBody(body, type, uri.toString());
                return extractDocument(doc, uri.toString(), request.selector(), request.ignoreSelector());
            }
            throw ApiException.badRequest("网页重定向次数过多");
        } catch (ApiException e) { throw e; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw ApiException.badRequest("检查已取消"); }
        catch (java.net.http.HttpTimeoutException e) { throw ApiException.badRequest("网站响应超时，请稍后重试"); }
        catch (Exception e) { throw ApiException.badRequest("无法读取网页，请检查网站是否可访问（页面上限 2 MB）"); }
    }

    Document parseBody(byte[] body, String type, String url) throws java.io.IOException {
        if (type.toLowerCase(java.util.Locale.ROOT).contains("text/plain")) {
            Document doc = Document.createShell(url);
            String detected = charset(type);
            doc.body().appendElement("main").appendElement("pre").text(new String(body, detected == null ? StandardCharsets.UTF_8 : Charset.forName(detected)));
            return doc;
        }
        return Jsoup.parse(new java.io.ByteArrayInputStream(body), charset(type), url);
    }

    private String charset(String type) {
        var matcher = java.util.regex.Pattern.compile("charset=[\"']?([\\w-]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(type);
        if (matcher.find()) { try { return Charset.forName(matcher.group(1)).name(); } catch (Exception ignored) {} }
        return null; // jsoup detects BOM and meta charset; defaults to UTF-8.
    }

    Preview extract(String html, String url, String selector, String ignore) {
        return extractDocument(Jsoup.parse(html, url), url, selector, ignore);
    }

    private Preview extractDocument(Document doc, String url, String selector, String ignore) {
        String title = doc.title().isBlank() ? URI.create(url).getHost() : doc.title();
        try {
            doc.select("script,style,noscript,template,svg,nav,footer,header,[hidden],[aria-hidden=true]").remove();
            if (ignore != null && !ignore.isBlank()) doc.select(ignore).remove();
            var roots = selector == null || selector.isBlank() ? doc.select("main") : doc.select(selector);
            if ((selector == null || selector.isBlank()) && roots.isEmpty()) roots = doc.select("article");
            if ((selector == null || selector.isBlank()) && roots.isEmpty()) roots = doc.select("body");
            if (roots.isEmpty()) throw ApiException.badRequest("没有匹配到关注区域，请检查 CSS 选择器");
            StringBuilder text = new StringBuilder();
            for (Element root : roots) {
                // A selector matching both a parent and child should not duplicate the child text.
                if (root.parents().stream().anyMatch(roots::contains)) continue;
                root.traverse(new org.jsoup.select.NodeVisitor() {
                    public void head(Node node, int depth) {
                        if (node instanceof Element e && BLOCKS.contains(e.normalName())) text.append('\n');
                        if (node instanceof TextNode t) text.append(t.getWholeText()).append(' ');
                    }
                    public void tail(Node node, int depth) {
                        if (node instanceof Element e && BLOCKS.contains(e.normalName())) text.append('\n');
                    }
                });
                text.append('\n');
            }
            String content = text.toString().replace('\u00a0', ' ').lines().map(s -> s.replaceAll("\\s+", " ").strip())
                    .filter(s -> !s.isBlank()).collect(java.util.stream.Collectors.joining("\n"));
            if (content.isBlank()) throw ApiException.badRequest("没有提取到正文。网页可能需要 JavaScript 渲染，或关注区域为空");
            if (content.length() > 100_000 || content.lines().count() > 3000) throw ApiException.badRequest("正文过长，请使用 CSS 选择器缩小关注区域（最多 10 万字符 / 3000 行）");
            return new Preview(title, content, content.length(), (int) content.lines().count(), url);
        } catch (Selector.SelectorParseException e) { throw ApiException.badRequest("CSS 选择器语法不正确"); }
    }

    static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final java.util.concurrent.CompletableFuture<byte[]> result = new java.util.concurrent.CompletableFuture<>();
        private final java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        private final int limit;
        private java.util.concurrent.Flow.Subscription subscription;
        LimitedBodySubscriber(int limit) { this.limit = limit; }
        public java.util.concurrent.CompletionStage<byte[]> getBody() { return result; }
        public void onSubscribe(java.util.concurrent.Flow.Subscription s) { subscription = s; s.request(1); }
        public void onNext(java.util.List<java.nio.ByteBuffer> items) {
            for (var buffer : items) {
                if (output.size() + buffer.remaining() > limit) {
                    subscription.cancel(); result.completeExceptionally(new java.io.IOException("Response too large")); return;
                }
                byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes); output.writeBytes(bytes);
            }
            subscription.request(1);
        }
        public void onError(Throwable error) { result.completeExceptionally(error); }
        public void onComplete() { result.complete(output.toByteArray()); }
    }
}
