package com.rental.payment;

public record PaymentResult(
        String paymentUrl,
        String vietQrUrl
) {
    public static PaymentResult empty() {
        return new PaymentResult(null, null);
    }
}
