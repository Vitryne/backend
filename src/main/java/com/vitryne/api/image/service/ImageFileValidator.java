package com.vitryne.api.image.service;

import com.vitryne.api.shared.exception.InvalidInputException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Valida a imagem pelo conteúdo (assinatura do arquivo), e não pela extensão
 * ou pelo Content-Type, que o cliente escolhe livremente.
 */
final class ImageFileValidator {

    static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

    private ImageFileValidator() {
    }

    /** @return o conteúdo do arquivo já validado */
    static byte[] readValidImage(MultipartFile file, int position) {
        String name = displayName(file, position);

        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("O arquivo " + name + " está vazio");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new InvalidInputException(
                    "O arquivo " + name + " passa de 5 MB. Envie uma imagem menor."
            );
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new InvalidInputException("Não foi possível ler o arquivo " + name);
        }

        if (!isSupportedImage(content)) {
            throw new InvalidInputException(
                    "O arquivo " + name + " não é uma imagem JPG, PNG ou WEBP"
            );
        }

        return content;
    }

    static boolean isSupportedImage(byte[] content) {
        return startsWith(content, 0, JPEG)
                || startsWith(content, 0, PNG)
                || (startsWith(content, 0, RIFF) && startsWith(content, 8, WEBP));
    }

    private static boolean startsWith(byte[] content, int offset, byte[] signature) {
        if (content == null || content.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[offset + i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static String displayName(MultipartFile file, int position) {
        String original = file == null ? null : file.getOriginalFilename();
        return original == null || original.isBlank()
                ? "na posição " + position
                : "\"" + original + "\"";
    }
}
