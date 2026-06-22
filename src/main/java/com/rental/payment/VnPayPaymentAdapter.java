package com.rental.payment;

import com.rental.entity.Payment;
import com.rental.repository.PaymentRepository;
import com.rental.service.VnPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
@RequiredArgsConstructor
public class VnPayPaymentAdapter implements PaymentAdapter {

    private final PaymentRepository paymentRepository;
    private final VnPayService vnPayService;

    @Override
    public boolean supports(String method) {
        return "Thẻ tín dụng".equalsIgnoreCase(method) || "VNPAY".equalsIgnoreCase(method);
    }

    @Override
    public PaymentResult createPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setBooking(request.booking());
        payment.setAmount(request.amount());
        payment.setPaymentMethod(Payment.Method.CreditCard);
        payment.setStatus(Payment.Status.Pending);
        paymentRepository.save(payment);

        String paymentInfo = normalizePaymentInfo("Thanh toan xe "
                + shortVehicleName(request.vehicle().getModelName())
                + " "
                + safePlate(request.vehicle().getLicensePlate()));

        String paymentUrl = vnPayService.createPaymentUrl(
                request.httpRequest(),
                request.amount(),
                paymentInfo,
                String.valueOf(payment.getPaymentId()));

        return new PaymentResult(paymentUrl, null);
    }

    private String shortVehicleName(String modelName) {
        if (modelName == null) return "";
        return modelName.length() > 20 ? modelName.substring(0, 20).trim() : modelName;
    }

    private String safePlate(String licensePlate) {
        return licensePlate != null ? licensePlate.toUpperCase() : "";
    }

    private String normalizePaymentInfo(String rawInfo) {
        return Normalizer.normalize(rawInfo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9 ]", "");
    }
}
