package dev.pagewatch.crawl.component;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DemoSource {
    public static final String URL = "https://demo.pagewatch.test/changelog";
    private final AtomicInteger version = new AtomicInteger(1);
    public int advance() { return version.incrementAndGet(); }
    public String html() {
        int v = version.get();
        return "<html><head><title>PageWatch 更新日志 · 演示网页</title></head><body><nav>导航</nav><main><h1>PageWatch 更新日志</h1>"
                + "<p>关注网页里真正重要的变化。</p><h2>v0." + v + ".0</h2>"
                + (v > 1 ? "<p>新增第 " + v + " 次演示更新：支持网页正文差异高亮。</p><p>优化了检查进度和历史版本体验。</p>" : "<p>首次发布：网页抓取、正文预览和定时检查。</p>")
                + "<h2>我们的计划</h2><p>让每一次网页更新都有迹可循。</p></main><footer>页脚</footer></body></html>";
    }
}
