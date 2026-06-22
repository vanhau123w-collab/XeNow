package com.rental.payment;

import com.rental.entity.Payment;
import com.rental.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashPaymentAdapter implements PaymentAdapter {

    private final PaymentRepository paymentRepository;

    @Override
    public boolean supports(String method) {
        return method == null || method.isBlank() || "Tiền mặt".equalsIgnoreCase(method);
    }

    @Override
    public PaymentResult createPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setBooking(request.booking());
        payment.setAmount(request.amount());
        payment.setPaymentMethod(Payment.Method.Cash);
        payment.setStatus(Payment.Status.Pending);
        paymentRepository.save(payment);
        return PaymentResult.empty();
    }
}
