package com.vitryne.api.payment.service;

import com.vitryne.api.payment.entity.Payment;
import com.vitryne.api.payment.enums.PaymentMethod;
import com.vitryne.api.payment.enums.PaymentResult;
import com.vitryne.api.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Simula o gateway de pagamento. O resultado é definido pelo cliente
 * (campo simulatedResult) e, quando omitido, o pagamento é aprovado.
 * Para integrar um gateway real, substitua apenas esta classe.
 */
@RequiredArgsConstructor
@Service
public class PaymentSimulator {

    private final PaymentRepository paymentRepository;

    public Payment process(
            Long userId,
            BigDecimal amount,
            PaymentMethod method,
            PaymentResult simulatedResult
    ) {
        PaymentResult result = simulatedResult == null
                ? PaymentResult.APPROVED
                : simulatedResult;

        return paymentRepository.save(new Payment(userId, amount, method, result));
    }
}
