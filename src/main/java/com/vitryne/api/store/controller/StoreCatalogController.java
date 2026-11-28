package com.vitryne.api.store.controller;

import com.vitryne.api.shared.dto.PageResponseDTO;
import com.vitryne.api.store.dto.StoreCatalogItemDTO;
import com.vitryne.api.store.service.StoreCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/stores")
@Tag(
        name = "Lojas",
        description = "Vitrine pública das lojas, consumida pelo app. Não exige autenticação."
)
public class StoreCatalogController {

    private final StoreCatalogService storeCatalogService;

    @Operation(
            operationId = "listActiveStores",
            summary = "Listar lojas ativas",
            description = """
                    Lista paginada das lojas ativas para a seção "Lojas perto de você"
                    da Home, em ordem alfabética.

                    Aparecem só lojas ativas com pelo menos um produto ativo.
                    Não há cálculo de distância no MVP.

                    isOpen indica se a loja está aberta agora pelos períodos de
                    funcionamento, no horário de Brasília.

                    Não traz CNPJ, razão social, telefone, endereço nem dados bancários.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de lojas"),
            @ApiResponse(
                    responseCode = "400",
                    description = "page negativo ou size fora de 1 a 50",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<PageResponseDTO<StoreCatalogItemDTO>> listActiveStores(
            @Parameter(description = "Número da página, começando em 0", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Itens por página, de 1 a 50", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(storeCatalogService.listActiveStores(page, size));
    }
}
