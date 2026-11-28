package com.vitryne.api.image.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "URLs públicas das imagens enviadas")
public record ImageUploadResponseDTO(
        @ArraySchema(
                arraySchema = @Schema(
                        description = """
                                Na mesma ordem dos arquivos enviados.
                                Use no photoUrls do cadastro ou da edição do produto;
                                a primeira URL do photoUrls é a capa.
                                """
                ),
                schema = @Schema(
                        example = "https://res.cloudinary.com/vitryne/image/upload/v1/vitryne/stores/3/products/abc123.jpg"
                )
        )
        List<String> urls
) {
}
