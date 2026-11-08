package com.vitryne.api.payment.entity;

import com.vitryne.api.payment.enums.PaymentMethod;
import com.vitryne.api.payment.enums.PaymentResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Registro de um pagamento simulado. Recusados também são gravados,
 * mas apenas aprovados ficam vinculados a um pedido.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "amount", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, updatable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, updatable = false, length = 20)
    private PaymentResult result;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Payment(Long userId, BigDecimal amount, PaymentMethod method, PaymentResult result) {
        if (userId == null) {
            throw new IllegalArgumentException("O usuário do pagamento é obrigatório");
        }

        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("O valor do pagamento não pode ser negativo");
        }

        if (method == null) {
            throw new IllegalArgumentException("A forma de pagamento é obrigatória");
        }

        if (result == null) {
            throw new IllegalArgumentException("O resultado do pagamento é obrigatório");
        }

        this.userId = userId;
        this.amount = amount;
        this.method = method;
        this.result = result;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isApproved() {
        return result == PaymentResult.APPROVED;
    }
}
