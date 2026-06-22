package com.rental.bookingstate;

import com.rental.entity.Booking;
import com.rental.entity.Payment;
import com.rental.entity.Vehicle;
import com.rental.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CompletedBookingStateHandler implements BookingStateHandler {

    private final PaymentRepository paymentRepository;

    @Override
    public boolean supports(Booking.Status status) {
        return status == Booking.Status.Completed;
    }

    @Override
    public void apply(Booking booking, BookingStateContext context) {
        Vehicle vehicle = booking.getVehicle();
        vehicle.setStatus(Vehicle.Status.Available);

        if (context.mileage() != null && context.mileage() > vehicle.getMileage()) {
            vehicle.setMileage(context.mileage());
        }

        if (vehicle.getMileage() - vehicle.getLastMaintenanceMileage() >= 5000) {
            vehicle.setStatus(Vehicle.Status.Maintenance);
        }

        booking.setReturnMileage(context.mileage());
        booking.setReturnNote(context.note());
        createRemainderPaymentIfNeeded(booking, context.returnPaymentMethod());
    }

    private void createRemainderPaymentIfNeeded(Booking booking, String returnPaymentMethod) {
        if (returnPaymentMethod == null || returnPaymentMethod.isBlank()) {
            return;
        }

        BigDecimal total = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
        List<Payment> existingPayments = paymentRepository.findByBookingBookingId(booking.getBookingId());
        BigDecimal paidAmount = existingPayments.stream()
                .filter(payment -> payment.getStatus() == Payment.Status.Completed
                        || payment.getStatus() == Payment.Status.Pending)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainder = total.subtract(paidAmount);
        if (remainder.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(remainder);
        payment.setPaymentMethod(resolvePaymentMethod(returnPaymentMethod));
        payment.setStatus(Payment.Status.Completed);
        paymentRepository.save(payment);
    }

    private Payment.Method resolvePaymentMethod(String returnPaymentMethod) {
        if ("Chuyển khoản".equalsIgnoreCase(returnPaymentMethod)) {
            return Payment.Method.BankTransfer;
        }
        if ("Thẻ tín dụng".equalsIgnoreCase(returnPaymentMethod)) {
            return Payment.Method.CreditCard;
        }
        return Payment.Method.Cash;
    }
}
