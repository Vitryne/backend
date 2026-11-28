package com.vitryne.api.order.controller;

import com.vitryne.api.order.dto.CancelOrderRequestDTO;
import com.vitryne.api.order.dto.CheckoutRequestDTO;
import com.vitryne.api.order.dto.CheckoutResponseDTO;
import com.vitryne.api.order.dto.CheckoutReviewResponseDTO;
import com.vitryne.api.order.dto.OrderResponseDTO;
import com.vitryne.api.order.service.OrderService;
import com.vitryne.api.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

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
                    - shippingAmount: frete fixo por pedido (R$ 10,90 no MVP), já somado ao totalAmount.

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

                    Aprovado: retorna 201, baixa o estoque, cria o pedido AWAITING_ACCEPTANCE (com frete
                    e cópia do endereço de entrega) e esvazia o carrinho. A loja então aceita ou recusa.
                    Recusado: retorna 402 com o pagamento registrado; order é nulo e carrinho e estoque
                    não são alterados.

                    Idempotência: envie o header Idempotency-Key (um UUID novo por confirmação) e reenvie
                    a mesma chave em caso de timeout ou erro de rede. A repetição com os mesmos dados
                    devolve o mesmo status e corpo da primeira resposta, com replayed = true e o header
                    Idempotent-Replayed, sem novo pagamento, pedido ou baixa de estoque. A chave vale só
                    para o cliente autenticado; reutilizá-la com outros dados retorna 422.
                    Uma chave só é consumida quando o pagamento é registrado: após um 409, a mesma chave
                    pode ser usada na nova tentativa.
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
            @ApiResponse(responseCode = "400",
                    description = "Dados de entrada inválidos ou Idempotency-Key ausente/inválida",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Carrinho vazio, problema bloqueante ou valores alterados desde a revisão",
                    content = @Content),
            @ApiResponse(responseCode = "422",
                    description = "Idempotency-Key já usada por este cliente com outros dados",
                    content = @Content)
    })
    public ResponseEntity<CheckoutResponseDTO> checkout(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                    in = ParameterIn.HEADER,
                    name = IDEMPOTENCY_KEY_HEADER,
                    required = true,
                    description = "Identificador único da confirmação (recomendado: UUID). "
                            + "8 a 100 caracteres: letras, números, - ou _.",
                    example = "3f1c9a2e-5b7d-4e8f-9a10-2b3c4d5e6f70"
            )
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody CheckoutRequestDTO request
    ) {
        CheckoutResponseDTO response =
                orderService.checkout(AuthenticatedUser.id(jwt), idempotencyKey, request);
        HttpStatus status = response.order() != null ? HttpStatus.CREATED : HttpStatus.PAYMENT_REQUIRED;
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (response.replayed()) {
            builder.header("Idempotent-Replayed", "true");
        }
        return builder.body(response);
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

    @PostMapping("/{orderId}/cancel")
    @Operation(
            operationId = "cancelMyOrder",
            summary = "Cancelar um pedido meu",
            description = """
                    Cancela o pedido enquanto ele estiver AWAITING_ACCEPTANCE, CONFIRMED ou PREPARING.
                    A partir de READY_FOR_PICKUP o cancelamento é recusado com 422.
                    As quantidades voltam ao estoque e o pedido registra quem cancelou, o motivo e a data.
                    O estorno do pagamento simulado não é feito nesta versão.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CancelOrderRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    {"reason": "CHANGED_MIND"}
                                    """)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Motivo ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado para este cliente",
                    content = @Content),
            @ApiResponse(responseCode = "422",
                    description = "O pedido já está pronto para coleta, em entrega, entregue ou cancelado",
                    content = @Content)
    })
    public ResponseEntity<OrderResponseDTO> cancelMine(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do pedido", example = "1") @PathVariable("orderId") Long orderId,
            @Valid @RequestBody CancelOrderRequestDTO request
    ) {
        return ResponseEntity.ok(orderService.cancelByClient(AuthenticatedUser.id(jwt), orderId, request));
    }
}
