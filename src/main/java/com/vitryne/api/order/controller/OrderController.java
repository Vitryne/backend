package com.vitryne.api.order.controller;

import com.vitryne.api.order.dto.CheckoutRequestDTO;
import com.vitryne.api.order.dto.CheckoutResponseDTO;
import com.vitryne.api.order.dto.CheckoutReviewResponseDTO;
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

    @GetMapping("/checkout/review")
    @Operation(
            operationId = "reviewMyCheckout",
            summary = "Revisar a compra antes de confirmar",
            description = """
                    Valida o carrinho e o endereço de entrega cadastrado e calcula os valores no backend.
                    Não cria pedido, não baixa estoque e não altera o carrinho.

                    - items[].status: OK, PRICE_CHANGED, INSUFFICIENT_QUANTITY, OUT_OF_STOCK ou PRODUCT_INACTIVE.
                    - issues: alterações e problemas encontrados. PRICE_CHANGED apenas informa
                      (o total já usa o preço atual); os demais têm blocking = true.
                    - canConfirm: false quando há problema bloqueante; nesse caso reviewToken é nulo.
                    - shippingAmount: frete, zero no MVP.

                    Envie o reviewToken em POST /orders/checkout. Se os valores mudarem depois da
                    revisão, o checkout retorna 409 e a revisão deve ser refeita.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Revisão calculada", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "409", description = "Carrinho vazio", content = @Content)
    })
    public ResponseEntity<CheckoutReviewResponseDTO> review(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(orderService.review(AuthenticatedUser.id(jwt)));
    }

    @PostMapping("/checkout")
    @Operation(
            operationId = "checkoutMyCart",
            summary = "Finalizar compra com pagamento simulado",
            description = """
                    Converte o carrinho em pedido usando os valores de GET /orders/checkout/review.
                    Envie o reviewToken da revisão: se itens, preços, frete ou endereço mudaram desde
                    então, retorna 409 sem cobrar e a revisão deve ser refeita.
                    O pagamento é simulado: envie simulatedResult = DECLINED para testar a recusa
                    (padrão APPROVED). O pagamento é registrado com valor e resultado em ambos os casos.

                    Aprovado: retorna 201, baixa o estoque, cria o pedido CONFIRMED (com frete e cópia
                    do endereço de entrega) e esvazia o carrinho.
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
                                            {"reviewToken": "9f2c4e...", "paymentMethod": "CREDIT_CARD", "simulatedResult": "APPROVED"}
                                            """),
                                    @ExampleObject(name = "Recusado", value = """
                                            {"reviewToken": "9f2c4e...", "paymentMethod": "PIX", "simulatedResult": "DECLINED"}
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
                    description = "Carrinho vazio, problema bloqueante ou valores alterados desde a revisão",
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
