package com.vitryne.api.image.storage;

/**
 * Armazenamento externo das imagens. O disco do servidor não serve: o Render
 * apaga o sistema de arquivos a cada redeploy ou hibernação (ADR-057).
 */
public interface ImageStorage {

    boolean isConfigured();

    /** @param folder pasta dentro do armazenamento, sem barra no fim */
    StoredImage upload(byte[] content, String folder);

    void delete(String publicId);
}
