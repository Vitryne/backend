package com.vitryne.api.order.entity;

import com.vitryne.api.address.entity.Address;
import com.vitryne.api.order.enums.OrderStatus;
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Cria um pedido confirmado. Exige pagamento aprovado, pois no MVP
     * não há status de pedido aguardando pagamento.
     */
    public static Order confirm(
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
        order.status = OrderStatus.CONFIRMED;
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

    public void changeStatus(OrderStatus next) {
        if (status != OrderStatus.CONFIRMED || next != OrderStatus.PREPARING) {
            throw new InvalidOrderStatusTransitionException(status, next);
        }

        this.status = next;
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
