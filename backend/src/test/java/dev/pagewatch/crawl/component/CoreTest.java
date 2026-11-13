package dev.pagewatch.crawl.component;

import dev.pagewatch.common.web.ApiException;
import dev.pagewatch.monitor.component.DiffService;
import dev.pagewatch.monitor.model.Models.DiffLine;

import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import static org.assertj.core.api.Assertions.*;

class CoreTest {
    private final CrawlService crawler = new CrawlService(new UrlPolicy(), new DemoSource());
    @Test void extractsMeaningfulTextAndIgnoresNoise() {
        var result=crawler.extract("<title>发布日志</title><nav>导航</nav><main><h1>更新</h1><p> 新增   内容 </p><span class='clock'>12:01</span><script>alert(1)</script></main><footer>页脚</footer>","https://example.org","", ".clock");
        assertThat(result.title()).isEqualTo("发布日志");
        assertThat(result.content()).isEqualTo("更新\n新增 内容");
        assertThat(result.lines()).isEqualTo(2);
    }
    @Test void selectorDoesNotDuplicateNestedContent() {
        var result=crawler.extract("<main><div><p>只出现一次</p></div></main>","https://example.org","main, p","");
        assertThat(result.content()).isEqualTo("只出现一次");
    }
    @Test void rejectsInvalidAndEmptySelectors() {
        assertThatThrownBy(()->crawler.extract("<p>正文</p>","https://example.org",".missing","")).isInstanceOf(ApiException.class).hasMessageContaining("没有匹配");
        assertThatThrownBy(()->crawler.extract("<p>正文</p>","https://example.org","[","")).isInstanceOf(ApiException.class).hasMessageContaining("语法");
        assertThatThrownBy(()->crawler.extract("<script>app()</script>","https://example.org","","")).isInstanceOf(ApiException.class).hasMessageContaining("没有提取");
    }
    @Test void rejectsPrivateAndNonHttpTargets() {
        var policy=new UrlPolicy();
        for(String url:List.of("file:///etc/passwd","http://127.0.0.1","http://10.0.0.1","http://169.254.169.254","http://[::1]","http://100.64.0.1","http://127.0.0.1:8080","https://user:pass@example.org"))
            assertThatThrownBy(()->policy.validate(url)).isInstanceOf(ApiException.class);
    }
    @Test void ipv6PolicyRejectsLocalAndReservedAddresses() throws Exception {
        assertThat(UrlPolicy.isPrivate(InetAddress.getByName("fc00::1"))).isTrue();
        assertThat(UrlPolicy.isPrivate(InetAddress.getByName("2001:db8::1"))).isTrue();
        assertThat(UrlPolicy.isPrivate(InetAddress.getByName("2606:4700:4700::1111"))).isFalse();
    }
    @Test void diffPreservesBothVersionsIncludingChineseText() {
        var lines=new DiffService().compare("保持\n旧内容\n末尾", "保持\n新内容\n新增一行\n末尾");
        assertThat(lines.stream().filter(l->!l.kind().equals("add")).map(DiffLine::text)).containsExactly("保持","旧内容","末尾");
        assertThat(lines.stream().filter(l->!l.kind().equals("remove")).map(DiffLine::text)).containsExactly("保持","新内容","新增一行","末尾");
        assertThat(lines.stream().filter(l->l.kind().equals("add"))).hasSize(2);
        assertThat(lines.stream().filter(l->l.kind().equals("remove"))).hasSize(1);
    }
    @Test void bodyLimitCancelsOversizedStream() {
        var subscriber=new CrawlService.LimitedBodySubscriber(4);
        boolean[] cancelled={false};
        subscriber.onSubscribe(new Flow.Subscription() { public void request(long n) {} public void cancel() { cancelled[0]=true; } });
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[5])));
        assertThat(cancelled[0]).isTrue();
        assertThat(subscriber.getBody().toCompletableFuture()).isCompletedExceptionally();
    }
    @Test void plainTextKeepsAngleBracketsAndLineBreaks() throws Exception {
        String text="version <2.0>\nhello & goodbye";
        var doc=crawler.parseBody(text.getBytes(java.nio.charset.StandardCharsets.UTF_8),"text/plain; charset=UTF-8","https://example.org");
        var result=crawler.extract(doc.outerHtml(),"https://example.org","","");
        assertThat(result.content()).isEqualTo(text);
    }
}
