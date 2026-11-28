package com.vitryne.api.image.storage;

/** Imagem gravada no armazenamento: identificador para apagar e URL pública. */
public record StoredImage(String publicId, String url) {
}
