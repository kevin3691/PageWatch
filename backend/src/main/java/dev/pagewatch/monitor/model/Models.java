package dev.pagewatch.monitor.model;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Models {
    private Models() {}

    public record PreviewRequest(@NotBlank @Size(max = 2048) String url,
                                 @Size(max = 500) String selector, @Size(max = 500) String ignoreSelector) {}
    public record CreateMonitor(@NotBlank @Size(max = 120) String name,
                                @NotBlank @Size(max = 2048) String url,
                                @Size(max = 500) String selector, @Size(max = 500) String ignoreSelector,
                                @Min(1) @Max(10080) int intervalMinutes) {}
    public record UpdateMonitor(@NotBlank @Size(max = 120) String name,
                                @Min(1) @Max(10080) int intervalMinutes, boolean active) {}
    public record Monitor(UUID id, String name, String url, String selector, String ignoreSelector,
                          int intervalMinutes, boolean active, boolean unread, Instant createdAt,
                          Instant lastCheckedAt, Instant nextCheckAt, Instant lastChangeAt,
                          String lastError, int snapshotCount, boolean checking) {}
    public record Snapshot(UUID id, UUID monitorId, UUID previousId, Instant createdAt,
                           int addedCount, int removedCount) {}
    public record StoredSnapshot(Snapshot snapshot, String content, String contentHash) {}
    public record CheckRun(UUID id, Instant checkedAt, String outcome, long durationMs, String error) {}
    public record Detail(Monitor monitor, List<Snapshot> snapshots, List<CheckRun> checks) {}
    public record DiffLine(String kind, String text, Integer oldLine, Integer newLine) {}
    public record SnapshotDetail(Snapshot snapshot, String content, String previousContent, List<DiffLine> diff) {}
    public record Preview(String title, String content, int characters, int lines, String resolvedUrl) {}
}
