package com.vitryne.api.payment.repository;

import com.vitryne.api.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
