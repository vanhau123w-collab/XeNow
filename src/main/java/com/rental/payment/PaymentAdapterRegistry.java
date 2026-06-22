package com.rental.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentAdapterRegistry {

    private final List<PaymentAdapter> adapters;

    public PaymentAdapter getAdapter(String method) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(method))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Phương thức thanh toán không được hỗ trợ: " + method));
    }
}
