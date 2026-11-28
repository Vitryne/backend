package com.vitryne.api.image.service;

import com.vitryne.api.shared.exception.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageFileValidatorTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P'};

    @Test
    void aceitaJpgPngEWebpPeloConteudo() {
        assertThat(ImageFileValidator.isSupportedImage(JPEG)).isTrue();
        assertThat(ImageFileValidator.isSupportedImage(PNG)).isTrue();
        assertThat(ImageFileValidator.isSupportedImage(WEBP)).isTrue();
    }

    @Test
    void recusaOutrosFormatosMesmoComExtensaoDeImagem() {
        byte[] gif = "GIF89a....".getBytes(StandardCharsets.US_ASCII);
        byte[] riffSemWebp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'};
        MockMultipartFile texto = new MockMultipartFile(
                "files", "foto.png", "image/png", "não sou imagem".getBytes(StandardCharsets.UTF_8)
        );

        assertThat(ImageFileValidator.isSupportedImage(gif)).isFalse();
        assertThat(ImageFileValidator.isSupportedImage(riffSemWebp)).isFalse();
        assertThat(ImageFileValidator.isSupportedImage(new byte[]{(byte) 0xFF})).isFalse();

        assertThatThrownBy(() -> ImageFileValidator.readValidImage(texto, 1))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("O arquivo \"foto.png\" não é uma imagem JPG, PNG ou WEBP");
    }

    @Test
    void recusaArquivoVazioOuAcimaDe5Mb() {
        MockMultipartFile vazio = new MockMultipartFile("files", "", "image/jpeg", new byte[0]);
        byte[] grande = new byte[(int) ImageFileValidator.MAX_BYTES + 1];
        System.arraycopy(JPEG, 0, grande, 0, JPEG.length);
        MockMultipartFile acima = new MockMultipartFile("files", "grande.jpg", "image/jpeg", grande);

        assertThatThrownBy(() -> ImageFileValidator.readValidImage(vazio, 2))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("O arquivo na posição 2 está vazio");
        assertThatThrownBy(() -> ImageFileValidator.readValidImage(acima, 1))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("passa de 5 MB");
    }
}
