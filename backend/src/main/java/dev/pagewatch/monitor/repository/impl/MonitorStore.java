package dev.pagewatch.monitor.repository.impl;

import dev.pagewatch.common.web.ApiException;
import dev.pagewatch.monitor.component.DiffService;
import dev.pagewatch.monitor.repository.MonitorRepository;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import static dev.pagewatch.monitor.model.Models.*;

@Repository
public class MonitorStore implements MonitorRepository {
    private final JdbcClient db;
    private final TransactionTemplate tx;
    private final DiffService diff;
    private static final String SELECT = "select m.*, (select count(*) from snapshot s where s.monitor_id=m.id) as snapshot_count from monitor m ";
    private static Instant instant(ResultSet row, String name) throws SQLException {
        Timestamp value = row.getTimestamp(name); return value == null ? null : value.toInstant();
    }
    private static final RowMapper<Monitor> MONITOR = (r, n) -> new Monitor(r.getObject("id", UUID.class), r.getString("name"),
            r.getString("url"), r.getString("selector"), r.getString("ignore_selector"), r.getInt("interval_minutes"),
            r.getBoolean("active"), r.getBoolean("unread"), instant(r,"created_at"), instant(r,"last_checked_at"),
            instant(r,"next_check_at"), instant(r,"last_change_at"), r.getString("last_error"), r.getInt("snapshot_count"), false);
    private static final RowMapper<Snapshot> SNAPSHOT = (r,n) -> new Snapshot(r.getObject("id", UUID.class), r.getObject("monitor_id", UUID.class),
            r.getObject("previous_id", UUID.class), instant(r,"created_at"), r.getInt("added_count"), r.getInt("removed_count"));

    public MonitorStore(JdbcClient db, TransactionTemplate tx, DiffService diff) { this.db=db; this.tx=tx; this.diff=diff; }
    public List<Monitor> list() { return db.sql(SELECT + "order by m.created_at desc").query(MONITOR).list(); }
    public Monitor get(UUID id) { return db.sql(SELECT + "where m.id=?").param(id).query(MONITOR).optional().orElseThrow(ApiException::notFound); }
    public List<Monitor> due() { return db.sql(SELECT + "where m.active and m.next_check_at <= now() order by m.next_check_at limit 50").query(MONITOR).list(); }
    public Monitor create(CreateMonitor request) {
        UUID id=UUID.randomUUID();
        db.sql("insert into monitor(id,name,url,selector,ignore_selector,interval_minutes) values (?,?,?,?,?,?)")
                .params(id, request.name().strip(), request.url().strip(), clean(request.selector()), clean(request.ignoreSelector()), request.intervalMinutes()).update();
        return get(id);
    }
    private static String clean(String s) { return s == null ? "" : s.strip(); }
    public Monitor update(UUID id, UpdateMonitor request) {
        if (db.sql("update monitor set name=?,interval_minutes=?,active=?,next_check_at=case when not active and ? then now() else now() + (? * interval '1 minute') end where id=?")
                .params(request.name().strip(), request.intervalMinutes(), request.active(), request.active(), request.intervalMinutes(), id).update()==0) throw ApiException.notFound();
        return get(id);
    }
    public void delete(UUID id) {
        if (db.sql("delete from monitor where id=?").param(id).update()==0) throw ApiException.notFound();
    }
    public void read(UUID id) {
        if (db.sql("update monitor set unread=false where id=?").param(id).update()==0) throw ApiException.notFound();
    }
    public List<Snapshot> snapshots(UUID id) {
        return db.sql("select * from snapshot where monitor_id=? order by created_at desc,id desc").param(id).query(SNAPSHOT).list();
    }
    public StoredSnapshot snapshot(UUID monitorId, UUID id) {
        return db.sql("select * from snapshot where id=? and monitor_id=?").params(id,monitorId)
                .query((r,n) -> new StoredSnapshot(SNAPSHOT.mapRow(r,n),r.getString("content"),r.getString("content_hash"))).optional().orElseThrow(ApiException::notFound);
    }
    private Optional<StoredSnapshot> latest(UUID id) {
        return db.sql("select * from snapshot where monitor_id=? order by created_at desc,id desc limit 1").param(id)
                .query((r,n)->new StoredSnapshot(SNAPSHOT.mapRow(r,n),r.getString("content"),r.getString("content_hash"))).optional();
    }
    public List<CheckRun> checks(UUID id) {
        return db.sql("select * from check_run where monitor_id=? order by checked_at desc limit 30").param(id)
                .query((r,n)->new CheckRun(r.getObject("id",UUID.class),instant(r,"checked_at"),r.getString("outcome"),r.getLong("duration_ms"),r.getString("error"))).list();
    }
    public void recordResult(UUID id, Preview preview, String error, long duration) {
        tx.executeWithoutResult(status -> {
            // Serialize writes per monitor; deletion during a network request is harmless.
            if (db.sql("select id from monitor where id=? for update").param(id).query(UUID.class).optional().isEmpty()) return;
            String outcome="FAILED";
            boolean changed=false;
            if (error == null) {
                String hash=hash(preview.content());
                var previous=latest(id);
                changed=previous.isPresent() && !previous.get().contentHash().equals(hash);
                outcome=previous.isEmpty() ? "BASELINE" : changed ? "CHANGED" : "UNCHANGED";
                if (previous.isEmpty() || changed) {
                    var lines=previous.isEmpty() ? List.<DiffLine>of() : diff.compare(previous.get().content(),preview.content());
                    db.sql("insert into snapshot(id,monitor_id,previous_id,content,content_hash,added_count,removed_count) values (?,?,?,?,?,?,?)")
                            .param(UUID.randomUUID()).param(id).param(3,previous.map(p->p.snapshot().id()).orElse(null), Types.OTHER)
                            .param(preview.content()).param(hash).param((int)lines.stream().filter(l->l.kind().equals("add")).count())
                            .param((int)lines.stream().filter(l->l.kind().equals("remove")).count()).update();
                }
            }
            db.sql("insert into check_run(id,monitor_id,outcome,duration_ms,error) values (?,?,?,?,?)")
                    .param(UUID.randomUUID()).param(id).param(outcome).param(duration).param(5,error, Types.VARCHAR).update();
            db.sql("update monitor set last_checked_at=now(), next_check_at=now() + (interval_minutes * interval '1 minute'), last_error=?, unread=unread or ?, last_change_at=case when ? then now() else last_change_at end where id=?")
                    .param(1,error,Types.VARCHAR).param(changed).param(changed).param(id).update();
            db.sql("delete from check_run where monitor_id=? and id not in (select id from check_run where monitor_id=? order by checked_at desc limit 100)")
                    .params(id,id).update();
        });
    }
    private String hash(String content) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
