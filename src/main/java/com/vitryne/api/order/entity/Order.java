package com.vitryne.api.order.entity;

import com.vitryne.api.address.entity.Address;
import com.vitryne.api.order.enums.CanceledBy;
import com.vitryne.api.order.enums.ClientCancellationReason;
import com.vitryne.api.order.enums.OrderStatus;
import com.vitryne.api.order.enums.StoreRejectionReason;
import com.vitryne.api.order.exception.InvalidOrderStatusTransitionException;
import com.vitryne.api.payment.entity.Payment;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.store.entity.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true, updatable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "shipping_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Cópia do endereço no momento da compra; não muda se o cadastro for alterado. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "zipCode", column = @Column(name = "delivery_zip_code", length = 8)),
            @AttributeOverride(name = "street", column = @Column(name = "delivery_street", length = 200)),
            @AttributeOverride(name = "number", column = @Column(name = "delivery_number", length = 20)),
            @AttributeOverride(name = "neighborhood", column = @Column(name = "delivery_neighborhood", length = 100)),
            @AttributeOverride(name = "complement", column = @Column(name = "delivery_complement", length = 150)),
            @AttributeOverride(name = "city", column = @Column(name = "delivery_city", length = 100)),
            @AttributeOverride(name = "state", column = @Column(name = "delivery_state", length = 2))
    })
    private Address deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "canceled_by", length = 10)
    private CanceledBy canceledBy;

    /** Código do motivo: StoreRejectionReason ou ClientCancellationReason, conforme canceledBy. */
    @Column(name = "cancellation_reason", length = 40)
    private String cancellationReason;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Cria o pedido aguardando o aceite da loja. Exige pagamento aprovado:
     * o pedido só existe depois que o pagamento foi aprovado.
     */
    public static Order place(
            Long userId,
            Store store,
            Payment payment,
            Address deliveryAddress,
            BigDecimal shippingAmount
    ) {
        if (userId == null || store == null) {
            throw new IllegalArgumentException("Usuário e loja são obrigatórios");
        }

        if (deliveryAddress == null) {
            throw new IllegalArgumentException("O endereço de entrega é obrigatório");
        }

        if (shippingAmount == null || shippingAmount.signum() < 0) {
            throw new IllegalArgumentException("O frete não pode ser negativo");
        }

        if (payment == null || !payment.isApproved()) {
            throw new IllegalArgumentException("O pedido exige um pagamento aprovado");
        }

        Order order = new Order();
        order.userId = userId;
        order.store = store;
        order.payment = payment;
        order.status = OrderStatus.AWAITING_ACCEPTANCE;
        order.deliveryAddress = Address.builder()
                .zipCode(deliveryAddress.getZipCode())
                .street(deliveryAddress.getStreet())
                .number(deliveryAddress.getNumber())
                .neighborhood(deliveryAddress.getNeighborhood())
                .complement(deliveryAddress.getComplement())
                .city(deliveryAddress.getCity())
                .state(deliveryAddress.getState())
                .build();
        order.shippingAmount = shippingAmount;
        order.totalAmount = shippingAmount;
        return order;
    }

    public OrderItem addItem(Stock stock, int quantity, BigDecimal unitPrice) {
        OrderItem item = new OrderItem(this, stock, quantity, unitPrice);
        items.add(item);
        totalAmount = totalAmount.add(item.getSubtotal());
        return item;
    }

    /**
     * Avança o pedido para a etapa seguinte do fluxo (inclui o aceite,
     * AWAITING_ACCEPTANCE → CONFIRMED). Pular, repetir ou voltar etapa é recusado.
     */
    public void advanceTo(OrderStatus next) {
        if (next == OrderStatus.CANCELED && !status.isFinal()) {
            throw new InvalidOrderStatusTransitionException(status.storeCanReject()
                    ? "Para cancelar um pedido aguardando aceite, use a recusa com a justificativa"
                    : "A loja não pode cancelar um pedido já aceito; o pedido está " + status);
        }

        if (next == null || next != status.next()) {
            throw new InvalidOrderStatusTransitionException(status, next);
        }

        this.status = next;
    }

    /** Recusa da loja; só antes do aceite. A devolução ao estoque fica com o serviço. */
    public void rejectByStore(StoreRejectionReason reason) {
        if (!status.storeCanReject()) {
            throw new InvalidOrderStatusTransitionException(
                    "A loja só pode recusar pedidos aguardando aceite; o pedido está " + status
            );
        }

        markCanceled(CanceledBy.STORE, reason);
    }

    /** Cancelamento pelo cliente; permitido até PREPARING. */
    public void cancelByClient(ClientCancellationReason reason) {
        if (!status.clientCanCancel()) {
            throw new InvalidOrderStatusTransitionException(
                    "O pedido não pode mais ser cancelado; ele está " + status
            );
        }

        markCanceled(CanceledBy.CLIENT, reason);
    }

    private void markCanceled(CanceledBy by, Enum<?> reason) {
        if (reason == null) {
            throw new IllegalArgumentException("O motivo do cancelamento é obrigatório");
        }

        this.status = OrderStatus.CANCELED;
        this.canceledBy = by;
        this.cancellationReason = reason.name();
        this.canceledAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
