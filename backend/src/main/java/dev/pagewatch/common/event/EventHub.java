package dev.pagewatch.common.event;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventHub {
    private final Set<SseEmitter> clients = ConcurrentHashMap.newKeySet();
    @org.springframework.context.event.EventListener(org.springframework.context.event.ContextClosedEvent.class)
    public void closeConnections() {
        clients.forEach(SseEmitter::complete);
        clients.clear();
    }
    public SseEmitter subscribe() {
        var emitter=new SseEmitter(30*60*1000L);
        clients.add(emitter);
        emitter.onCompletion(()->clients.remove(emitter));
        emitter.onTimeout(()->{ clients.remove(emitter); emitter.complete(); });
        emitter.onError(error->clients.remove(emitter));
        send(emitter,"connected",Map.of("ready",true));
        return emitter;
    }
    public void refresh(UUID id) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCommit() { broadcast(id); }
            });
        } else broadcast(id);
    }
    private void broadcast(UUID id) { clients.forEach(e->send(e,"refresh",Map.of("id",id.toString()))); }
    @Scheduled(fixedDelay=20000)
    public void heartbeat() { clients.forEach(e->send(e,"ping",Map.of("time",System.currentTimeMillis()))); }
    private void send(SseEmitter emitter,String event,Object data) {
        try { emitter.send(SseEmitter.event().name(event).data(data)); }
        catch (Exception e) { clients.remove(emitter); emitter.complete(); }
    }
}
