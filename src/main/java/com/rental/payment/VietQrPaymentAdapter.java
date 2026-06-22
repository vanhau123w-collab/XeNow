package com.rental.payment;

import com.rental.entity.Payment;
import com.rental.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

@Component
@RequiredArgsConstructor
public class VietQrPaymentAdapter implements PaymentAdapter {

    private static final String BANK_BIN = "970436";
    private static final String ACCOUNT_NO = "1040489156";
    private static final String TEMPLATE = "compact";
    private static final String ACCOUNT_NAME = "Nguyen%20Nhat%20Thien";

    private final PaymentRepository paymentRepository;

    @Override
    public boolean supports(String method) {
        return "Chuyển khoản VietQR".equalsIgnoreCase(method) || "Chuyển khoản".equalsIgnoreCase(method);
    }

    @Override
    public PaymentResult createPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setBooking(request.booking());
        payment.setAmount(request.amount());
        payment.setPaymentMethod(Payment.Method.BankTransfer);
        payment.setStatus(Payment.Status.Pending);
        paymentRepository.save(payment);

        String addInfo = normalizePaymentInfo("Thanh toan dat xe "
                + shortVehicleName(request.vehicle().getModelName())
                + " "
                + safePlate(request.vehicle().getLicensePlate()));
        String encodedAddInfo = URLEncoder.encode(addInfo, StandardCharsets.UTF_8).replace("+", "%20");

        String vietQrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-%s.png?amount=%s&addInfo=%s&accountName=%s",
                BANK_BIN,
                ACCOUNT_NO,
                TEMPLATE,
                request.amount(),
                encodedAddInfo,
                ACCOUNT_NAME);

        return new PaymentResult(null, vietQrUrl);
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
