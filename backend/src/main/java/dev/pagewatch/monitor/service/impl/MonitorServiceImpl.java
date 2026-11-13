package dev.pagewatch.monitor.service.impl;

import dev.pagewatch.common.event.EventHub;
import dev.pagewatch.crawl.component.CrawlService;
import dev.pagewatch.monitor.component.DiffService;
import dev.pagewatch.monitor.model.Models.*;
import dev.pagewatch.monitor.repository.MonitorRepository;
import dev.pagewatch.monitor.service.CheckService;
import dev.pagewatch.monitor.service.MonitorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class MonitorServiceImpl implements MonitorService {
    private final MonitorRepository store;
    private final CheckService checks;
    private final CrawlService crawler;
    private final DiffService diff;
    private final EventHub events;

    public MonitorServiceImpl(MonitorRepository store, CheckService checks, CrawlService crawler, DiffService diff, EventHub events) {
        this.store=store; this.checks=checks; this.crawler=crawler; this.diff=diff; this.events=events;
    }
    public List<Monitor> list() { return store.list().stream().map(checks::view).toList(); }
    @Transactional
    public Monitor create(CreateMonitor request) {
        // Validate the extraction before saving; a failed preview never creates a broken subscription.
        long start=System.nanoTime();
        Preview preview=crawler.fetch(new PreviewRequest(request.url(),request.selector(),request.ignoreSelector()));
        var monitor=store.create(request);
        store.recordResult(monitor.id(),preview,null,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
        events.refresh(monitor.id());
        return checks.view(store.get(monitor.id()));
    }
    public Detail detail(UUID id) { return new Detail(checks.view(store.get(id)),store.snapshots(id),store.checks(id)); }
    public Monitor update(UUID id, UpdateMonitor request) {
        var monitor=store.update(id,request); events.refresh(id); return checks.view(monitor);
    }
    public void delete(UUID id) { store.delete(id); events.refresh(id); }
    public void read(UUID id) { store.read(id); events.refresh(id); }
    public boolean check(UUID id) { return checks.submit(id); }
    public int checkAll() {
        int count=0; for(var monitor:store.list()) if(monitor.active() && checks.submit(monitor.id())) count++;
        return count;
    }
    public SnapshotDetail snapshot(UUID id, UUID snapshotId) {
        var current=store.snapshot(id,snapshotId);
        String previous=current.snapshot().previousId()==null ? "" : store.snapshot(id,current.snapshot().previousId()).content();
        var lines=previous.isEmpty() ? List.<DiffLine>of() : diff.compare(previous,current.content());
        return new SnapshotDetail(current.snapshot(),current.content(),previous,lines);
    }
}
