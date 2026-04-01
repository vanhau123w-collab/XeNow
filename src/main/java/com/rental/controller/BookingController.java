package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.BookingDTO;
import com.rental.entity.Booking;
import com.rental.entity.Customer;
import com.rental.dto.BookingDTO;
import com.rental.dto.BookingRequestDTO;
import com.rental.service.BookingService;
import com.rental.service.CustomerService;
import com.rental.service.VehicleService;
import com.rental.entity.DriverLicense;
import com.rental.entity.Vehicle;
import com.rental.repository.DriverLicenseRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.math.BigDecimal;
import jakarta.servlet.http.HttpServletRequest;
import com.rental.service.VnPayService;
import com.rental.repository.PaymentRepository;
import com.rental.entity.Payment;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final DriverLicenseRepository driverLicenseRepository;
    private final VnPayService vnPayService;
    private final PaymentRepository paymentRepository;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDTO>> getBookingById(@PathVariable Integer id) {
        Booking booking = bookingService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(booking), "Lấy thông tin đơn đặt xe thành công"));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<Page<BookingDTO>>> myBookings(
            @PageableDefault(size = 50) Pageable pageable,
            Authentication authentication) {
        Customer customer = customerService.findByIdentifier(authentication.getName());
        Page<BookingDTO> bookings = bookingService.getBookingsByCustomer(customer.getCustomerId(), pageable)
                .map(this::convertToDTO);
        return ResponseEntity.ok(ApiResponse.success(bookings, "Lấy danh sách lịch thuê của bạn thành công"));
    }


    @SuppressWarnings("null")
    @PostMapping("/create/{vehicleId}")
    public ResponseEntity<ApiResponse<BookingDTO>> createBooking(
            @PathVariable Integer vehicleId,
            @RequestBody BookingRequestDTO request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            Customer customer = customerService.findByIdentifier(authentication.getName());
            Vehicle vehicle = vehicleService.getById(vehicleId);
            
            // --- VALIDATION RULE CHO GPLX ---
            if (request.getDriverLicenseId() == null) {
                throw new RuntimeException("Vui lòng chọn Giấy phép lái xe");
            }
            
            DriverLicense dl = driverLicenseRepository.findById(request.getDriverLicenseId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin Giấy phép lái xe"));

            if (!dl.getCustomer().getUserId().equals(customer.getUserId())) {
                throw new RuntimeException("Giấy phép lái xe không hợp lệ");
            }

            if (dl.getExpiryDate() != null && dl.getExpiryDate().isBefore(LocalDate.now())) {
                throw new RuntimeException("Giấy phép lái xe đã hết hạn");
            }

            String licenseClass = dl.getLicenseClass() != null ? dl.getLicenseClass().toUpperCase() : "";
            LocalDate issueDate = dl.getIssueDate();
            if (issueDate == null) {
                throw new RuntimeException("Giấy phép lái xe thiếu ngày cấp");
            }
            
            boolean isOldLaw = issueDate.isBefore(LocalDate.of(2025, 1, 1));
            boolean isValid = false;
            
            String vType = vehicle.getType() != null ? vehicle.getType() : "Xe Ô Tô";
            if (vType.toLowerCase().contains("xe số") || vType.toLowerCase().contains("tay ga")) {
                int capacity = vehicle.getEngineCapacity() != null ? vehicle.getEngineCapacity() : 110;
                if (isOldLaw) {
                    if (capacity < 175) {
                        isValid = licenseClass.equals("A1") || licenseClass.equals("A2");
                    } else {
                        isValid = licenseClass.equals("A2");
                    }
                } else {
                    if (capacity <= 125) {
                        isValid = licenseClass.equals("A1") || licenseClass.equals("A");
                    } else {
                        isValid = licenseClass.equals("A");
                    }
                }
            } else {
                // Ô Tô
                int seats = vehicle.getSeats() != null ? vehicle.getSeats() : 4;
                if (seats <= 9) {
                    if (isOldLaw) {
                        isValid = licenseClass.equals("B1") || licenseClass.equals("B2") || 
                                  licenseClass.equals("C") || licenseClass.equals("D") || licenseClass.equals("E");
                    } else {
                        isValid = licenseClass.equals("B") || licenseClass.equals("C1") || 
                                  licenseClass.equals("C") || licenseClass.equals("D") || licenseClass.equals("D1") || licenseClass.equals("D2");
                    }
                } else if (seats <= 30) {
                    if (isOldLaw) {
                        isValid = licenseClass.equals("D") || licenseClass.equals("E");
                    } else {
                        isValid = licenseClass.equals("D1") || licenseClass.equals("D2") || licenseClass.equals("D");
                    }
                } else {
                    isValid = licenseClass.equals("E"); // Mới không có E, nhưng fallback
                }
            }

            if (!isValid) {
                String errorMsg = String.format("GPLX hạng %s không đủ điều kiện thuê phương tiện này theo quy định (cấp %s). Vui lòng chọn GPLX phù hợp.", 
                                                dl.getLicenseClass(), isOldLaw ? "trước năm 2025" : "từ năm 2025");
                throw new RuntimeException(errorMsg);
            }
            // --- END VALIDATION ---
            
            Booking booking = new Booking();
            booking.setCustomer(customer);
            booking.setVehicle(vehicle);
            booking.setStartDate(request.getStartDate());
            booking.setEndDate(request.getEndDate());
            
            booking.setPickupAddress(request.getPickupAddress());
            booking.setReturnAddress(request.getReturnAddress());
            
            Booking saved = bookingService.createBooking(booking);
            BookingDTO dto = convertToDTO(saved);
            
            String paymentUrl = null;
            String vietQrUrl = null;
            
            if (request.getPaymentAmount() != null && request.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
                Payment payment = new Payment();
                payment.setBooking(saved);
                payment.setAmount(request.getPaymentAmount());
                
                String pm = request.getPaymentMethod();
                if ("Thẻ tín dụng".equalsIgnoreCase(pm) || "VNPAY".equalsIgnoreCase(pm)) {
                    payment.setPaymentMethod(Payment.Method.CreditCard);
                    payment.setStatus(Payment.Status.Pending);
                    paymentRepository.save(payment);
                    
                    // Optimize length to ensure QR works on all banking apps (limit ~40-50 chars)
                    String shortName = vehicle.getModelName();
                    if (shortName != null && shortName.length() > 20) {
                        shortName = shortName.substring(0, 20).trim();
                    }
                    // Remove accents for VietQR description safety using a simple regex replace or Normalizer
                    String rawInfo = "Xe " + shortName + " " + (vehicle.getLicensePlate() != null ? vehicle.getLicensePlate().toUpperCase() : "");
                    String paymentInfo = java.text.Normalizer.normalize(rawInfo, java.text.Normalizer.Form.NFD)
                                            .replaceAll("\\p{M}", "").replaceAll("[^a-zA-Z0-9 ]", "");
                                            
                    String orderInfo = paymentInfo;
                    paymentUrl = vnPayService.createPaymentUrl(httpRequest, request.getPaymentAmount(), orderInfo, String.valueOf(payment.getPaymentId()));
                } else if ("Chuyển khoản VietQR".equalsIgnoreCase(pm) || "Chuyển khoản".equalsIgnoreCase(pm)) {
                    payment.setPaymentMethod(Payment.Method.BankTransfer);
                    payment.setStatus(Payment.Status.Pending);
                    paymentRepository.save(payment);
                    
                    String bankBin = "970436"; // Vietcombank Sandbox
                    String accountNo = "1040489156"; 
                    String template = "compact";
                    
                    String shortName = vehicle.getModelName();
                    if (shortName != null && shortName.length() > 20) {
                        shortName = shortName.substring(0, 20).trim();
                    }
                    String rawInfo = "Thanh toan dat xe " + shortName + " " + (vehicle.getLicensePlate() != null ? vehicle.getLicensePlate().toUpperCase() : "");
                    String addInfo = java.text.Normalizer.normalize(rawInfo, java.text.Normalizer.Form.NFD)
                                            .replaceAll("\\p{M}", "").replaceAll("[^a-zA-Z0-9 ]", "");

                    String encodedAddInfo = "";
                    try {
                        encodedAddInfo = java.net.URLEncoder.encode(addInfo, "UTF-8").replace("+", "%20");
                    } catch (Exception e) {
                        encodedAddInfo = addInfo.replace(" ", "%20");
                    }
                    vietQrUrl = String.format("https://img.vietqr.io/image/%s-%s-%s.png?amount=%s&addInfo=%s&accountName=Nguyen%%20Nhat%%20Thien", 
                                              bankBin, accountNo, template, request.getPaymentAmount().toString(), encodedAddInfo);
                } else if ("Tiền mặt".equalsIgnoreCase(pm)) {
                    payment.setPaymentMethod(Payment.Method.Cash);
                    payment.setStatus(Payment.Status.Pending);
                    paymentRepository.save(payment);
                }
            }

            dto.setPaymentUrl(paymentUrl);
            dto.setVietQrUrl(vietQrUrl);
            
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(saved.getBookingId())
                    .toUri();
            
            return ResponseEntity.created(location)
                    .body(ApiResponse.created(dto, "Đặt xe thành công!"));
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for internal context
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi tạo đơn đặt xe: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<ApiResponse<BookingDTO>> confirmPayment(@PathVariable Integer id) {
        try {
            Booking booking = bookingService.updateStatus(id, Booking.Status.Confirmed);
            return ResponseEntity.ok(ApiResponse.success(convertToDTO(booking), "Thanh toán thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Đặt xe thất bại: " + e.getMessage()));
        }
    }

    private BookingDTO convertToDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setBookingId(booking.getBookingId());
        dto.setVehicleId(booking.getVehicle().getVehicleId());
        dto.setVehicleModel(booking.getVehicle().getName());
        dto.setCustomerName(booking.getCustomer().getName());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setPickupLocationName(booking.getPickupAddress());
        dto.setReturnLocationName(booking.getReturnAddress());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setDepositAmount(booking.getDepositAmount());
        dto.setStatus(booking.getStatus());
        dto.setCreatedAt(booking.getCreatedAt());
        return dto;
    }
}
