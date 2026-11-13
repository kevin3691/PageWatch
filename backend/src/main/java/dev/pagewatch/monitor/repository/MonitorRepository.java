package dev.pagewatch.monitor.repository;

import dev.pagewatch.monitor.model.Models.*;
import java.util.List;
import java.util.UUID;

public interface MonitorRepository {
    List<Monitor> list();
    Monitor get(UUID id);
    List<Monitor> due();
    Monitor create(CreateMonitor request);
    Monitor update(UUID id, UpdateMonitor request);
    void delete(UUID id);
    void read(UUID id);
    List<Snapshot> snapshots(UUID id);
    StoredSnapshot snapshot(UUID monitorId, UUID id);
    List<CheckRun> checks(UUID id);
    void recordResult(UUID id, Preview preview, String error, long duration);
}
