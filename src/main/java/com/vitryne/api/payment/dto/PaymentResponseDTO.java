package com.vitryne.api.payment.dto;

import com.vitryne.api.payment.entity.Payment;
import com.vitryne.api.payment.enums.PaymentMethod;
import com.vitryne.api.payment.enums.PaymentResult;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponseDTO(
        Long id,
        BigDecimal amount,
        PaymentMethod method,
        PaymentResult result,
        Instant createdAt
) {
    public static PaymentResponseDTO from(Payment payment) {
        return new PaymentResponseDTO(
                payment.getId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getResult(),
                payment.getCreatedAt()
        );
    }
}
