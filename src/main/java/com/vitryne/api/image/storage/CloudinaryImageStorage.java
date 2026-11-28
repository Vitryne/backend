package com.vitryne.api.image.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.vitryne.api.image.exception.ImageStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Cloudinary configurado pela variável CLOUDINARY_URL.
 * Sem a variável a aplicação sobe normalmente e só o upload fica indisponível.
 */
@Component
public class CloudinaryImageStorage implements ImageStorage {

    private final Cloudinary cloudinary;

    public CloudinaryImageStorage(@Value("${vitryne.images.cloudinary-url:}") String cloudinaryUrl) {
        this.cloudinary = cloudinaryUrl == null || cloudinaryUrl.isBlank()
                ? null
                : new Cloudinary(cloudinaryUrl.trim());
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
