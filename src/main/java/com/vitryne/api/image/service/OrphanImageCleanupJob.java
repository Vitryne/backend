package com.vitryne.api.image.service;

import com.vitryne.api.image.entity.UploadedImage;
import com.vitryne.api.image.repository.UploadedImageRepository;
import com.vitryne.api.image.storage.ImageStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Apaga as imagens livres há mais tempo que o prazo: enviadas e nunca usadas
 * em produto, removidas na edição ou de produtos excluídos.
 */
@Slf4j
@Component
public class OrphanImageCleanupJob {

    static final int BATCH_SIZE = 100;

    private final UploadedImageRepository imageRepository;
    private final ImageStorage storage;
    private final TransactionTemplate transaction;
    private final Duration retention;

    public OrphanImageCleanupJob(
            UploadedImageRepository imageRepository,
            ImageStorage storage,
            TransactionTemplate transaction,
            @Value("${vitryne.images.orphan-retention:24h}") Duration retention
    ) {
        this.imageRepository = imageRepository;
        this.storage = storage;
        this.transaction = transaction;
        this.retention = retention;
    }

    @Scheduled(
            fixedDelayString = "${vitryne.images.orphan-cleanup.interval:PT1H}",
            initialDelayString = "${vitryne.images.orphan-cleanup.interval:PT1H}"
    )
    public void run() {
        if (!storage.isConfigured()) {
            return;
        }
        int purged = purgeOrphans(Instant.now());
        if (purged > 0) {
            log.info("{} imagem(ns) sem uso apagada(s)", purged);
        }
    }

    /**
     * Um lote por execução. Apaga do armazenamento antes do banco: se o
     * armazenamento falhar, o registro continua e a imagem é tentada de novo
     * na próxima execução.
     *
     * @return quantidade de imagens apagadas
     */
    public int purgeOrphans(Instant now) {
        List<UploadedImage> orphans = imageRepository.findOrphansBefore(
                now.minus(retention), PageRequest.of(0, BATCH_SIZE)
        );

        int purged = 0;
        for (UploadedImage image : orphans) {
            try {
                storage.delete(image.getPublicId());
            } catch (RuntimeException exception) {
                log.warn("Falha ao apagar a imagem {}; nova tentativa na próxima execução",
                        image.getPublicId(), exception);
                continue;
            }

            Integer deleted = transaction.execute(status -> imageRepository.deleteIfOrphan(image.getId()));
            if (deleted != null && deleted == 1) {
                purged++;
            } else {
                log.error("A imagem {} passou a ser usada durante a limpeza e foi apagada do armazenamento",
                        image.getPublicId());
            }
        }
        return purged;
    }
}
