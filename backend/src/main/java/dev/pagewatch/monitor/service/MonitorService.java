package dev.pagewatch.monitor.service;

import dev.pagewatch.monitor.model.Models.*;
import java.util.List;
import java.util.UUID;

public interface MonitorService {
    List<Monitor> list();
    Monitor create(CreateMonitor request);
    Detail detail(UUID id);
    Monitor update(UUID id, UpdateMonitor request);
    void delete(UUID id);
    void read(UUID id);
    boolean check(UUID id);
    int checkAll();
    SnapshotDetail snapshot(UUID id, UUID snapshotId);
}
