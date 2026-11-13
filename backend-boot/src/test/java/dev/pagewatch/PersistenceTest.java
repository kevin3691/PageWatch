package dev.pagewatch;

import dev.pagewatch.common.web.ApiException;
import dev.pagewatch.crawl.component.DemoSource;
import dev.pagewatch.monitor.repository.impl.MonitorStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static dev.pagewatch.monitor.model.Models.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE,properties={
        "spring.datasource.url=${TEST_DATABASE_URL:jdbc:postgresql://127.0.0.1:55432/pagewatch_test}",
        "spring.datasource.username=${TEST_DATABASE_USER:pagewatch}","spring.datasource.password=${TEST_DATABASE_PASSWORD:pagewatch}",
        "pagewatch.schedule-delay-ms=3600000"})
@Transactional
class PersistenceTest {
    @Autowired MonitorStore store;
    @Autowired JdbcClient db;
    @BeforeEach void assertIsolatedTestDatabase() {
        assertThat(db.sql("select current_database()").query(String.class).single()).endsWith("_test");
    }
    private Monitor create() { return store.create(new CreateMonitor("测试关注",DemoSource.URL,"main","",60)); }
    private Preview content(String text) { return new Preview("标题",text,text.length(),(int)text.lines().count(),DemoSource.URL); }
    @Test void savesOnlyChangedVersionsAndKeepsUnreadUntilAcknowledged() {
        UUID id=create().id();
        store.recordResult(id,content("第一行\n第二行"),null,12);
        assertThat(store.get(id).unread()).isFalse();
        store.recordResult(id,content("第一行\n第二行"),null,13);
        assertThat(store.snapshots(id)).hasSize(1);
        assertThat(store.checks(id).getFirst().outcome()).isEqualTo("UNCHANGED");
        store.recordResult(id,content("第一行\n新内容"),null,14);
        assertThat(store.snapshots(id)).hasSize(2);
        assertThat(store.get(id).unread()).isTrue();
        var changed=store.snapshots(id).getFirst();
        assertThat(changed.previousId()).isNotNull();
        assertThat(changed.addedCount()).isEqualTo(1);
        assertThat(changed.removedCount()).isEqualTo(1);
        store.recordResult(id,content("第一行\n新内容"),null,15);
        assertThat(store.get(id).unread()).isTrue();
        store.read(id); assertThat(store.get(id).unread()).isFalse();
    }
    @Test void failedCheckPreservesLastGoodSnapshotAndRecovers() {
        UUID id=create().id(); store.recordResult(id,content("有效内容"),null,1);
        store.recordResult(id,null,"HTTP 503",10);
        assertThat(store.get(id).lastError()).isEqualTo("HTTP 503");
        assertThat(store.snapshots(id)).hasSize(1);
        assertThat(store.checks(id).getFirst().outcome()).isEqualTo("FAILED");
        store.recordResult(id,content("有效内容"),null,1);
        assertThat(store.get(id).lastError()).isNull();
        assertThat(store.snapshots(id)).hasSize(1);
    }
    @Test void pauseExcludesDueChecksAndDeleteCascades() {
        var m=create(); store.recordResult(m.id(),content("历史"),null,1);
        store.update(m.id(),new UpdateMonitor("新名字",15,false));
        db.sql("update monitor set next_check_at=now() - interval '1 hour' where id=?").param(m.id()).update();
        assertThat(store.due()).noneMatch(d->d.id().equals(m.id()));
        store.update(m.id(),new UpdateMonitor("新名字",15,true));
        assertThat(store.due()).anyMatch(d->d.id().equals(m.id()));
        store.delete(m.id());
        assertThat(store.snapshots(m.id())).isEmpty(); assertThat(store.checks(m.id())).isEmpty();
        assertThatThrownBy(()->store.get(m.id())).isInstanceOf(ApiException.class);
    }
    @Test void snapshotCannotBeReadThroughAnotherMonitor() {
        var a=create(); var b=create(); store.recordResult(a.id(),content("私有快照"),null,1);
        UUID snapshotId=store.snapshots(a.id()).getFirst().id();
        assertThatThrownBy(()->store.snapshot(b.id(),snapshotId)).isInstanceOf(ApiException.class);
    }
}
