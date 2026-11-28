package com.vitryne.api.order.entity;

import com.vitryne.api.payment.entity.Payment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Registro de uma confirmação de compra já processada, identificada pela
 * chave de idempotência enviada pelo cliente. Só é gravado quando o
 * pagamento é registrado (aprovado ou recusado); falhas antes disso não
 * consomem a chave.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "checkout_idempotency")
public class CheckoutIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
    private String idempotencyKey;

    /** SHA-256 do corpo da confirmação, para recusar a chave reutilizada com outros dados. */
    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true, updatable = false)
    private Payment payment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CheckoutIdempotency(Long userId, String idempotencyKey, String requestHash, Payment payment) {
        if (userId == null || idempotencyKey == null || requestHash == null || payment == null) {
            throw new IllegalArgumentException("Dados de idempotência incompletos");
        }

        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.payment = payment;
    }

    public boolean matches(String otherRequestHash) {
        return requestHash.equals(otherRequestHash);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
