package dev.pagewatch.monitor.service;

import dev.pagewatch.common.web.ApiException;
import dev.pagewatch.crawl.component.CrawlService;
import dev.pagewatch.common.event.EventHub;
import dev.pagewatch.monitor.repository.MonitorRepository;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
import static dev.pagewatch.monitor.model.Models.*;

@Service
public class CheckService {
    private static final Logger log=LoggerFactory.getLogger(CheckService.class);
    private final MonitorRepository store;
    private final CrawlService crawler;
    private final EventHub events;
    private final Set<UUID> running=ConcurrentHashMap.newKeySet();
    private final ExecutorService executor=Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore permits=new Semaphore(4);
    public CheckService(MonitorRepository store,CrawlService crawler,EventHub events) { this.store=store; this.crawler=crawler; this.events=events; }
    public boolean checking(UUID id) { return running.contains(id); }
    public Monitor view(Monitor m) {
        return new Monitor(m.id(),m.name(),m.url(),m.selector(),m.ignoreSelector(),m.intervalMinutes(),m.active(),m.unread(),m.createdAt(),
                m.lastCheckedAt(),m.nextCheckAt(),m.lastChangeAt(),m.lastError(),m.snapshotCount(),checking(m.id()));
    }
    public boolean submit(UUID id) {
        Monitor monitor=store.get(id);
        if (!running.add(id)) return false;
        events.refresh(id);
        executor.submit(()->{
            boolean acquired=false;
            long start=System.nanoTime();
            try {
                permits.acquire(); acquired=true;
                Preview preview=null;
                String error=null;
                try { preview=crawler.fetch(new PreviewRequest(monitor.url(),monitor.selector(),monitor.ignoreSelector())); }
                catch (ApiException e) { error=e.getMessage(); }
                catch (Exception e) { error="检查失败，请稍后重试"; log.warn("Fetch failed for monitor {}",id,e); }
                store.recordResult(id,preview,error,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            catch (Exception e) { log.error("Cannot persist check for {}",id,e); }
            finally { if(acquired) permits.release(); running.remove(id); events.refresh(id); }
        });
        return true;
    }
    @Scheduled(fixedDelayString="${pagewatch.schedule-delay-ms}",initialDelay=5000)
    public void checkDue() {
        for (var monitor:store.due()) { try { submit(monitor.id()); } catch(ApiException ignored) {} }
    }
    @PreDestroy
    public void close() { executor.shutdownNow(); }
}
