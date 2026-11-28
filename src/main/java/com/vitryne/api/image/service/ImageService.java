package com.vitryne.api.image.service;

import com.vitryne.api.image.entity.UploadedImage;
import com.vitryne.api.image.repository.UploadedImageRepository;
import com.vitryne.api.image.storage.ImageStorage;
import com.vitryne.api.image.storage.StoredImage;
import com.vitryne.api.image.exception.ImageStorageException;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.shared.exception.InvalidInputException;
import com.vitryne.api.store.entity.Store;
import com.vitryne.api.store.exception.StoreNotFoundException;
import com.vitryne.api.store.repository.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ImageService {

    public static final int MAX_FILES_PER_UPLOAD = 8;

    private final ImageStorage storage;
    private final UploadedImageRepository imageRepository;
    private final StoreRepository storeRepository;
    private final String baseFolder;

    public ImageService(
            ImageStorage storage,
            UploadedImageRepository imageRepository,
            StoreRepository storeRepository,
            @Value("${vitryne.images.folder:vitryne}") String baseFolder
    ) {
        this.storage = storage;
        this.imageRepository = imageRepository;
        this.storeRepository = storeRepository;
        this.baseFolder = baseFolder == null || baseFolder.isBlank() ? "vitryne" : baseFolder.trim();
    }

    /**
     * Envia as imagens da loja e devolve as URLs na ordem recebida.
     * Tudo ou nada: um arquivo inválido barra o envio de todos, e uma falha
     * no meio do envio apaga o que já tinha subido.
     */
    public List<String> uploadProductImages(List<MultipartFile> files, Long ownerId) {
        if (files == null || files.isEmpty()) {
            throw new InvalidInputException("Envie ao menos uma imagem no campo files");
        }

        if (files.size() > MAX_FILES_PER_UPLOAD) {
            throw new InvalidInputException(
                    "Envie no máximo " + MAX_FILES_PER_UPLOAD + " imagens por vez"
            );
        }

        if (!storage.isConfigured()) {
            throw ImageStorageException.notConfigured();
        }

        Store store = storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new StoreNotFoundException(ownerId));

        List<byte[]> contents = new ArrayList<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            contents.add(ImageFileValidator.readValidImage(files.get(i), i + 1));
        }

        String folder = baseFolder + "/stores/" + store.getId() + "/products";
        List<StoredImage> stored = new ArrayList<>(contents.size());

        try {
            for (byte[] content : contents) {
                stored.add(storage.upload(content, folder));
            }

            Instant now = Instant.now();
            imageRepository.saveAll(stored.stream()
                    .map(image -> new UploadedImage(store, image.publicId(), image.url(), now))
                    .toList());
        } catch (RuntimeException exception) {
            discard(stored);
            throw exception;
        }

        return stored.stream().map(StoredImage::url).toList();
    }

    /**
     * Liga ao produto as imagens da lista e libera as que saíram dela.
     * Só aceita imagens enviadas pela loja do produto e que não estejam em
     * outro produto. URLs que o produto já tinha antes do upload existir
     * (fora do armazenamento) podem ser mantidas, mas não adicionadas.
     */
    @Transactional
    public void syncProductPhotos(Product product, List<String> urls, Collection<String> previousUrls) {
        Map<String, UploadedImage> byUrl = imageRepository.findAllByUrlIn(urls).stream()
                .collect(Collectors.toMap(UploadedImage::getUrl, Function.identity()));

        for (String url : urls) {
            UploadedImage image = byUrl.get(url);

            if (image == null) {
                if (previousUrls.contains(url)) {
                    continue;
                }
                throw notUploadedByStore(url);
            }

            if (!image.belongsTo(product.getStore())) {
                throw notUploadedByStore(url);
            }

            if (image.isUsedByOtherThan(product)) {
                throw new InvalidInputException(
                        "A imagem " + url + " já é usada em outro produto"
                );
            }

            image.attachTo(product);
        }

        Set<String> kept = new HashSet<>(urls);
        Instant now = Instant.now();
        imageRepository.findAllByProductId(product.getId()).stream()
                .filter(image -> !kept.contains(image.getUrl()))
                .forEach(image -> image.release(now));
    }

    /** Antes de excluir produtos: as fotos ficam livres e entram na limpeza. */
    @Transactional
    public void releaseFromProducts(Collection<Long> productIds) {
        if (!productIds.isEmpty()) {
            imageRepository.releaseFromProducts(productIds, Instant.now());
        }
    }

    private void discard(List<StoredImage> stored) {
        for (StoredImage image : stored) {
            try {
                storage.delete(image.publicId());
            } catch (RuntimeException exception) {
                log.warn("Imagem {} ficou no armazenamento após falha no envio", image.publicId(), exception);
            }
        }
    }

    private static InvalidInputException notUploadedByStore(String url) {
        return new InvalidInputException(
                "A imagem " + url + " não foi enviada por esta loja. "
                        + "Envie as fotos pela rota POST /uploads/images e use as URLs devolvidas."
        );
    }
}
