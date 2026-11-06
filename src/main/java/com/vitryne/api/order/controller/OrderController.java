package com.vitryne.api.order.controller;

import com.vitryne.api.order.dto.CheckoutRequestDTO;
import com.vitryne.api.order.dto.CheckoutResponseDTO;
import com.vitryne.api.order.dto.OrderResponseDTO;
import com.vitryne.api.order.service.OrderService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/orders")
@PreAuthorize("hasRole('CONSUMER')")
@Tag(
        name = "Pedidos",
        description = "Checkout e consulta dos pedidos do cliente autenticado."
)
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "O usuário autenticado não possui o papel CONSUMER",
                content = @Content)
})
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @Operation(
            operationId = "checkoutMyCart",
            summary = "Finalizar compra com pagamento simulado",
            description = """
                    Converte o carrinho em pedido. O valor é recalculado pelo backend com o preço atual.
                    O pagamento é simulado: envie simulatedResult = DECLINED para testar a recusa
                    (padrão APPROVED). O pagamento é registrado com valor e resultado em ambos os casos.

                    Aprovado: retorna 201, baixa o estoque, cria o pedido CONFIRMED e esvazia o carrinho.
                    Recusado: retorna 402 com o pagamento registrado; order é nulo e carrinho e estoque
                    não são alterados.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CheckoutRequestDTO.class),
                            examples = {
                                    @ExampleObject(name = "Aprovado", value = """
                                            {"paymentMethod": "CREDIT_CARD", "simulatedResult": "APPROVED"}
                                            """),
                                    @ExampleObject(name = "Recusado", value = """
                                            {"paymentMethod": "PIX", "simulatedResult": "DECLINED"}
                                            """)
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pagamento aprovado e pedido criado",
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "402", description = "Pagamento recusado; nenhum pedido criado",
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Carrinho vazio, produto inativo ou estoque insuficiente",
                    content = @Content)
    })
    public ResponseEntity<CheckoutResponseDTO> checkout(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CheckoutRequestDTO request
    ) {
        CheckoutResponseDTO response = orderService.checkout(AuthenticatedUser.id(jwt), request);
        HttpStatus status = response.order() != null ? HttpStatus.CREATED : HttpStatus.PAYMENT_REQUIRED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping
    @Operation(
            operationId = "listMyOrders",
            summary = "Listar meus pedidos",
            description = "Retorna os pedidos do cliente, do mais recente para o mais antigo."
    )
    @ApiResponse(responseCode = "200", description = "Pedidos do cliente", useReturnTypeSchema = true)
    public ResponseEntity<List<OrderResponseDTO>> listMine(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(orderService.listByCustomer(AuthenticatedUser.id(jwt)));
    }

    @GetMapping("/{orderId}")
    @Operation(operationId = "getMyOrder", summary = "Consultar um pedido meu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado para este cliente",
                    content = @Content)
    })
    public ResponseEntity<OrderResponseDTO> findMine(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do pedido", example = "1") @PathVariable("orderId") Long orderId
    ) {
        return ResponseEntity.ok(orderService.findByCustomer(AuthenticatedUser.id(jwt), orderId));
    }
}
