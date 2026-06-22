package com.rental.payment;

public interface PaymentAdapter {
    boolean supports(String method);

    PaymentResult createPayment(PaymentRequest request);
}
