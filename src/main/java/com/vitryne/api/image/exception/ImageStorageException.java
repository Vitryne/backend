package com.vitryne.api.image.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Falha do armazenamento externo, não do arquivo enviado. */
@Getter
public class ImageStorageException extends RuntimeException {

    private final HttpStatus status;

    private ImageStorageException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public static ImageStorageException notConfigured() {
        return new ImageStorageException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "O armazenamento de imagens não está configurado no servidor",
                null
        );
    }

    public static ImageStorageException uploadFailed(Throwable cause) {
        return new ImageStorageException(
                HttpStatus.BAD_GATEWAY,
                "Não foi possível enviar as imagens agora. Tente novamente.",
                cause
        );
    }

    public static ImageStorageException deleteFailed(String publicId, Throwable cause) {
        return new ImageStorageException(
                HttpStatus.BAD_GATEWAY,
                "Não foi possível apagar a imagem " + publicId,
                cause
        );
    }
}
