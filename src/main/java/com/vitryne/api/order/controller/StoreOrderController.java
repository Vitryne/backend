package com.vitryne.api.order.controller;

import com.vitryne.api.order.dto.OrderResponseDTO;
import com.vitryne.api.order.dto.UpdateOrderStatusRequestDTO;
import com.vitryne.api.order.service.OrderService;
import com.vitryne.api.order.stream.OrderStreamService;
import com.vitryne.api.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/store/orders")
@PreAuthorize("hasRole('STORE')")
@Tag(
        name = "Pedidos da loja",
        description = "Consulta e andamento dos pedidos recebidos pela loja do usuário autenticado."
)
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "O usuário autenticado não possui o papel STORE",
                content = @Content)
})
public class StoreOrderController {

    private final OrderService orderService;
    private final OrderStreamService orderStreamService;

    @GetMapping
    @Operation(
            operationId = "listStoreOrders",
            summary = "Listar pedidos da loja",
            description = "Retorna os pedidos da loja, do mais recente para o mais antigo."
    )
    @ApiResponse(responseCode = "200", description = "Pedidos da loja", useReturnTypeSchema = true)
    public ResponseEntity<List<OrderResponseDTO>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(orderService.listByStoreOwner(AuthenticatedUser.id(jwt)));
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            operationId = "streamStoreOrders",
            summary = "Acompanhar pedidos da loja em tempo real (SSE)",
            description = "Stream text/event-stream. Eventos: `connected` (conexão aberta), "
                    + "`order_created` e `order_updated`, ambos com o pedido (OrderResponseDTO) em JSON."
    )
    public SseEmitter stream(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return orderStreamService.subscribe(AuthenticatedUser.id(jwt));
    }

    @GetMapping("/{orderId}")
    @Operation(operationId = "getStoreOrder", summary = "Consultar um pedido da loja")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado para esta loja",
                    content = @Content)
    })
    public ResponseEntity<OrderResponseDTO> find(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do pedido", example = "1") @PathVariable("orderId") Long orderId
    ) {
        return ResponseEntity.ok(orderService.findByStoreOwner(AuthenticatedUser.id(jwt), orderId));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(
            operationId = "updateStoreOrderStatus",
            summary = "Atualizar status do pedido",
            description = "No MVP, a única transição permitida é CONFIRMED → PREPARING.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateOrderStatusRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    {"status": "PREPARING"}
                                    """)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado para esta loja",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Transição de status não permitida",
                    content = @Content)
    })
    public ResponseEntity<OrderResponseDTO> updateStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do pedido", example = "1") @PathVariable("orderId") Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequestDTO request
    ) {
        return ResponseEntity.ok(
                orderService.updateStatus(AuthenticatedUser.id(jwt), orderId, request)
        );
    }
}
