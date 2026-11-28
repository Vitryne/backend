package com.vitryne.api.order.stream;

import com.vitryne.api.order.event.OrderChangedEvent;
import com.vitryne.api.store.entity.Store;
import com.vitryne.api.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStreamService {

    private static final long NO_TIMEOUT = 0L;

    private final StoreRepository storeRepository;

    private final Map<Long, List<SseEmitter>> emittersByStore = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long ownerId) {
        Store store = storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Loja não encontrada para o usuário autenticado"));
        Long storeId = store.getId();

        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        List<SseEmitter> emitters =
                emittersByStore.computeIfAbsent(storeId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        Runnable cleanup = () -> remove(storeId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException | IllegalStateException exception) {
            cleanup.run();
        }

        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderChanged(OrderChangedEvent event) {
        List<SseEmitter> emitters = emittersByStore.get(event.storeId());
        if (emitters == null) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.order().id() + ":" + event.order().updatedAt().toEpochMilli())
                        .name(event.type().eventName())
                        .data(event.order()));
            } catch (IOException | IllegalStateException exception) {
                remove(event.storeId(), emitter);
            }
        }
    }

    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emittersByStore.forEach((storeId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("keep-alive"));
                } catch (IOException | IllegalStateException exception) {
                    remove(storeId, emitter);
                }
            }
        });
    }

    private void remove(Long storeId, SseEmitter emitter) {
        emittersByStore.computeIfPresent(storeId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
