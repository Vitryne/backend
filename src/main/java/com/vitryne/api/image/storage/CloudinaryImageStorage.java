package com.vitryne.api.image.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.vitryne.api.image.exception.ImageStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Cloudinary configurado pela variável CLOUDINARY_URL.
 * Sem a variável a aplicação sobe normalmente e só o upload fica indisponível.
 */
@Slf4j
@Component
public class CloudinaryImageStorage implements ImageStorage {

    private final Cloudinary cloudinary;

    public CloudinaryImageStorage(@Value("${vitryne.images.cloudinary-url:}") String cloudinaryUrl) {
        this.cloudinary = cloudinaryUrl == null || cloudinaryUrl.isBlank()
                ? null
                : connect(cloudinaryUrl.trim());
    }

    /**
     * URL mal formada não derruba a aplicação: só o upload fica indisponível (503),
     * como sem a variável. O log explica o formato sem expor o segredo.
     */
    private static Cloudinary connect(String cloudinaryUrl) {
        if (!cloudinaryUrl.matches("cloudinary://[^:@<>\\s]+:[^@<>\\s]+@[^@<>\\s/]+")) {
            log.error("CLOUDINARY_URL inválida; upload de imagens desativado. Use o formato "
                    + "cloudinary://<api_key>:<api_secret>@<cloud_name>, copiado de Settings → API Keys "
                    + "no console do Cloudinary.");
            return null;
        }
        return new Cloudinary(cloudinaryUrl);
    }

    @Override
    public boolean isConfigured() {
        return cloudinary != null;
    }

    @Override
    public StoredImage upload(byte[] content, String folder) {
        requireConfigured();
        try {
            Map<?, ?> result = cloudinary.uploader().upload(content, ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "overwrite", false
            ));
            return new StoredImage(
                    (String) result.get("public_id"),
                    (String) result.get("secure_url")
            );
        } catch (IOException | RuntimeException exception) {
            throw ImageStorageException.uploadFailed(exception);
        }
    }

    @Override
    public void delete(String publicId) {
        requireConfigured();
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true
            ));
        } catch (IOException | RuntimeException exception) {
            throw ImageStorageException.deleteFailed(publicId, exception);
        }
    }

    private void requireConfigured() {
        if (cloudinary == null) {
            throw ImageStorageException.notConfigured();
        }
    }
}
