package dev.pagewatch.monitor.controller;

import dev.pagewatch.common.event.EventHub;
import dev.pagewatch.crawl.component.CrawlService;
import dev.pagewatch.crawl.component.DemoSource;
import dev.pagewatch.monitor.service.MonitorService;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.*;
import static dev.pagewatch.monitor.model.Models.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final MonitorService monitors;
    private final CrawlService crawler;
    private final EventHub events;
    private final DemoSource demo;
    public ApiController(MonitorService monitors,CrawlService crawler,EventHub events,DemoSource demo) {
        this.monitors=monitors; this.crawler=crawler; this.events=events; this.demo=demo;
    }
    @GetMapping("/health") public Map<String,String> health() { return Map.of("status","UP","application","PageWatch"); }
    @GetMapping("/monitors") public List<Monitor> list() { return monitors.list(); }
    @PostMapping("/preview") public Preview preview(@Valid @RequestBody PreviewRequest request) { return crawler.fetch(request); }
    @PostMapping("/monitors") @ResponseStatus(HttpStatus.CREATED)
    public Monitor create(@Valid @RequestBody CreateMonitor request) { return monitors.create(request); }
    @GetMapping("/monitors/{id}") public Detail detail(@PathVariable UUID id) { return monitors.detail(id); }
    @PatchMapping("/monitors/{id}") public Monitor update(@PathVariable UUID id,@Valid @RequestBody UpdateMonitor request) { return monitors.update(id,request); }
    @DeleteMapping("/monitors/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { monitors.delete(id); }
    @PostMapping("/monitors/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable UUID id) { monitors.read(id); }
    @PostMapping("/monitors/{id}/check") @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String,Boolean> check(@PathVariable UUID id) { return Map.of("accepted",monitors.check(id)); }
    @PostMapping("/checks") @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String,Integer> checkAll() { return Map.of("accepted",monitors.checkAll()); }
    @GetMapping("/monitors/{id}/snapshots/{snapshotId}")
    public SnapshotDetail snapshot(@PathVariable UUID id,@PathVariable UUID snapshotId) { return monitors.snapshot(id,snapshotId); }
    @GetMapping(value="/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE) public SseEmitter subscribe() { return events.subscribe(); }
    @PostMapping("/demo/advance") public Map<String,Integer> advance() { return Map.of("version",demo.advance()); }
}
