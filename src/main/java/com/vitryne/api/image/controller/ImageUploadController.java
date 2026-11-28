package com.vitryne.api.image.controller;

import com.vitryne.api.image.dto.ImageUploadResponseDTO;
import com.vitryne.api.image.service.ImageService;
import com.vitryne.api.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/uploads")
@PreAuthorize("hasRole('STORE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Upload de imagens",
        description = """
                Envio das fotos dos produtos para o armazenamento externo.
                Exige usuário autenticado com perfil STORE.
                """
)
public class ImageUploadController {

    private final ImageService imageService;

    @Operation(
            operationId = "uploadProductImages",
            summary = "Enviar fotos de produto",
            description = """
                    Recebe de 1 a 8 imagens (JPG, PNG ou WEBP, até 5 MB cada)
                    em multipart/form-data, no campo files, e devolve a URL
                    pública de cada uma, na ordem enviada.

                    Use as URLs no photoUrls do cadastro ou da edição do produto.
                    O produto aceita só URLs enviadas por esta rota pela própria
                    loja, até 8 fotos, e a primeira é a capa.

                    O tipo é conferido pelo conteúdo do arquivo, não pela extensão.
                    Se um arquivo for inválido, nenhum é enviado.

                    Imagem enviada e não usada em produto é apagada depois de 24 horas.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Imagens enviadas",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ImageUploadResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Nenhum arquivo, mais de 8, arquivo vazio, acima de 5 MB
                            ou que não seja JPG, PNG ou WEBP. O campo detail traz a
                            mensagem para exibir no portal.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Usuário sem perfil STORE", content = @Content),
            @ApiResponse(
                    responseCode = "502",
                    description = "Falha no armazenamento externo; nenhuma imagem fica salva",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Armazenamento de imagens não configurado no servidor",
                    content = @Content
            )
    })
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponseDTO> uploadProductImages(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt,

            @Parameter(description = "Imagens do produto, na ordem de exibição (1 a 8)")
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return ResponseEntity.ok(new ImageUploadResponseDTO(
                imageService.uploadProductImages(files, AuthenticatedUser.id(jwt))
        ));
    }
}
