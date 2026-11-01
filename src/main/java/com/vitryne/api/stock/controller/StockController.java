package com.vitryne.api.stock.controller;

import com.vitryne.api.product.dto.CreateStockRequestDTO;
import com.vitryne.api.stock.dto.AdjustStockRequestDTO;
import com.vitryne.api.stock.dto.ReplenishStockRequestDTO;
import com.vitryne.api.stock.dto.StockManagementResponseDTO;
import com.vitryne.api.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/product/manager/{productId}/stocks")
@PreAuthorize("hasRole('STORE')")
@Tag(
        name = "Gestão de estoque",
        description = "Gestão dos tamanhos e quantidades da própria loja"
)
public class StockController {

    private final StockService stockService;

    @Operation(
            summary = "Adicionar tamanho ao produto",
            description = "Cria um tamanho com quantidade inicial zero ou positiva."
    )
    @PostMapping
    public ResponseEntity<StockManagementResponseDTO> addStock(
            @PathVariable Long productId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "id") Long ownerId,

            @Valid @RequestBody CreateStockRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockService.addStock(
                        productId,
                        ownerId,
                        request
                ));
    }

    @Operation(
            summary = "Repor estoque",
            description = "Soma a quantidade informada ao estoque atual."
    )
    @PostMapping("/{stockId}/replenishments")
    public ResponseEntity<StockManagementResponseDTO> replenishStock(
            @PathVariable Long productId,
            @PathVariable Long stockId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "id") Long ownerId,

            @Valid @RequestBody ReplenishStockRequestDTO request
    ) {
        return ResponseEntity.ok(
                stockService.replenishStock(
                        productId,
                        stockId,
                        ownerId,
                        request
                )
        );
    }

    @Operation(
            summary = "Ajustar estoque",
            description = """
                    Define a quantidade total do tamanho.
                    Envie a versão recebida na consulta gerencial.
                    Retorna conflito se os dados estiverem desatualizados.
                    """
    )
    @PatchMapping("/{stockId}")
    public ResponseEntity<StockManagementResponseDTO> adjustStock(
            @PathVariable Long productId,
            @PathVariable Long stockId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "id") Long ownerId,

            @Valid @RequestBody AdjustStockRequestDTO request
    ) {
        return ResponseEntity.ok(
                stockService.adjustStock(
                        productId,
                        stockId,
                        ownerId,
                        request
                )
        );
    }
}