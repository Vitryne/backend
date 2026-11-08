package com.vitryne.api.payment.service;

import com.vitryne.api.payment.entity.Payment;
import com.vitryne.api.payment.enums.PaymentMethod;
import com.vitryne.api.payment.enums.PaymentResult;
import com.vitryne.api.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSimulatorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentSimulator simulator;

    @Test
    void semResultadoInformadoDeveAprovarERegistrarValor() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment payment = simulator.process(10L, new BigDecimal("59.70"), PaymentMethod.PIX, null);

        assertThat(payment.getResult()).isEqualTo(PaymentResult.APPROVED);
        assertThat(payment.isApproved()).isTrue();
        assertThat(payment.getAmount()).isEqualByComparingTo("59.70");
        assertThat(payment.getUserId()).isEqualTo(10L);
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.PIX);
    }

    @Test
    void deveRespeitarResultadoSimuladoRecusado() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment payment = simulator.process(
                10L, new BigDecimal("10.00"), PaymentMethod.CREDIT_CARD, PaymentResult.DECLINED
        );

        assertThat(payment.getResult()).isEqualTo(PaymentResult.DECLINED);
        assertThat(payment.isApproved()).isFalse();
    }
}
