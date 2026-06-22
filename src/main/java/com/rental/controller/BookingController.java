package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.BookingDTO;
import com.rental.entity.Booking;
import com.rental.entity.Customer;
import com.rental.dto.BookingRequestDTO;
import com.rental.service.BookingService;
import com.rental.service.CustomerService;
import com.rental.service.VehicleService;
import com.rental.entity.DriverLicense;
import com.rental.entity.Vehicle;
import com.rental.license.LicenseValidationService;
import com.rental.payment.PaymentAdapter;
import com.rental.payment.PaymentAdapterRegistry;
import com.rental.payment.PaymentRequest;
import com.rental.payment.PaymentResult;
import com.rental.repository.DriverLicenseRepository;
import lombok.RequiredArgsConstructor;
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

/**
 * Controller xử lý các yêu cầu liên quan đến Đặt xe (Booking) dành cho đối tượng Khách hàng.
 * 
 * Class này đóng vai trò là tầng Giao diện API (Presentation Layer), tiếp nhận các yêu cầu
 * khởi tạo đơn đặt xe, truy vấn lịch sử thuê xe và xử lý các xác nhận thanh toán điện tử.
 * 
 * Các công nghệ tích hợp chính:
 * - Spring Security: Xác thực người dùng qua JWT.
 * - VNPAY: Cổng thanh toán điện tử thẻ nội địa/quốc tế.
 * - VietQR: Tự động sinh mã QR chuyển khoản ngân hàng theo tiêu chuẩn Napas247.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final CustomerService customerService;
    private final DriverLicenseRepository driverLicenseRepository;
    private final LicenseValidationService licenseValidationService;
    private final PaymentAdapterRegistry paymentAdapterRegistry;

    /**
     * Lấy thông tin chi tiết của một đơn đặt xe cụ thể dựa trên ID.
     * @param id Mã định danh duy nhất của đơn hàng (Primary Key).
     * @return Đối tượng ApiResponse chứa dữ liệu BookingDTO đã được chuẩn hóa.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDTO>> getBookingById(@PathVariable Integer id) {
        Booking booking = bookingService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(convertToDTO(booking), "Lấy thông tin đơn đặt xe thành công"));
    }

    /**
     * Truy vấn danh sách tất cả các đơn đặt xe thuộc quyền sở hữu của người dùng đang đăng nhập.
     * Kết quả được phân trang để tối ưu hóa hiệu năng truyền tải dữ liệu.
     * 
     * @param pageable Tham số phân trang (mặc định 50 phần tử/trang).
     * @param authentication Đối tượng chứa thông tin định danh của người dùng từ Security Context.
     */
    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<Page<BookingDTO>>> myBookings(
            @PageableDefault(size = 50) Pageable pageable,
            Authentication authentication) {
        // Tìm đối tượng khách hàng (Customer) dựa trên Username/Email trích xuất từ JWT
        Customer customer = customerService.findByIdentifier(authentication.getName());
        
        // Truy vấn dữ liệu từ Service và thực hiện ánh xạ (mapping) sang DTO
        Page<BookingDTO> bookings = bookingService.getBookingsByCustomer(customer.getCustomerId(), pageable)
                .map(this::convertToDTO);
                
        return ResponseEntity.ok(ApiResponse.success(bookings, "Lấy danh sách lịch thuê của bạn thành công"));
    }

    /**
     * Nghiệp vụ quan trọng nhất: Khởi tạo một đơn đặt xe mới cho một phương tiện.
     * 
     * Quy trình xử lý (Business Workflow):
     * 1. Xác thực thông tin Khách hàng và Phương tiện.
     * 2. KIỂM TRA ĐIỀU KIỆN GPLX: Đây là tính năng đặc thù của XeNow, tự động áp dụng
     *    luật giao thông đường bộ Việt Nam (bao gồm cả thay đổi hạng bằng lái từ 01/01/2025).
     * 3. Kiểm tra tính sẵn sàng của xe (Overlap check) thông qua Service.
     * 4. Tính toán giá trị đơn hàng, tiền cọc và lưu trạng thái PENDING.
     * 5. Khởi tạo giao dịch thanh toán điện tử (VNPAY hoặc VietQR) tùy theo lựa chọn của khách.
     * 
     * @param vehicleId ID của xe khách muốn thuê.
     * @param request DTO chứa thông tin ngày thuê, địa điểm và phương thức thanh toán.
     * @param authentication Thông tin người đặt.
     * @param httpRequest Đối tượng request để lấy IP người dùng phục vụ cho VNPAY.
     */
    @SuppressWarnings("null")
    @PostMapping("/create/{vehicleId}")
    public ResponseEntity<ApiResponse<BookingDTO>> createBooking(
            @PathVariable Integer vehicleId,
            @RequestBody BookingRequestDTO request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            // Lấy thông tin thực thể từ Database
            Customer customer = customerService.findByIdentifier(authentication.getName());
            Vehicle vehicle = vehicleService.getById(vehicleId);
            
            // --- GIAI ĐOẠN 1: KIỂM TRA TÍNH HỢP LỆ CỦA GIẤY PHÉP LÁI XE (GPLX) ---
            // Đảm bảo khách hàng đã chọn ít nhất một GPLX đã định danh trên hệ thống
            if (request.getDriverLicenseId() == null) {
                throw new RuntimeException("Vui lòng chọn Giấy phép lái xe để tiếp tục");
            }
            
            DriverLicense dl = driverLicenseRepository.findById(request.getDriverLicenseId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu Giấy phép lái xe trong hệ thống"));

            // Ràng buộc bảo mật: GPLX phải thuộc về tài khoản đang thực hiện giao dịch
            if (!dl.getCustomer().getUserId().equals(customer.getUserId())) {
                throw new RuntimeException("Phát hiện hành vi gian lận: GPLX không thuộc về tài khoản này");
            }

            licenseValidationService.validate(dl, vehicle);
            // --- KẾT THÚC GIAI ĐOẠN KIỂM TRA GPLX ---
            
            // GIAI ĐOẠN 2: KHỞI TẠO ĐƠN HÀNG TRỰC TIẾP
            Booking booking = new Booking();
            booking.setCustomer(customer);
            booking.setVehicle(vehicle);
            booking.setStartDate(request.getStartDate());
            booking.setEndDate(request.getEndDate());
            booking.setPickupAddress(request.getPickupAddress());
            booking.setReturnAddress(request.getReturnAddress());
            
            // Gọi Service để xử lý nghiệp vụ lưu trữ và kiểm tra lịch trống xe (Conflict validation)
            Booking saved = bookingService.createBooking(booking);
            BookingDTO dto = convertToDTO(saved);
            
            PaymentResult paymentResult = PaymentResult.empty();
            
            // GIAI ĐOẠN 3: XỬ LÝ THANH TOÁN (PAYMENT GATEWAY INTEGRATION)
            if (request.getPaymentAmount() != null && request.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
                PaymentAdapter adapter = paymentAdapterRegistry.getAdapter(request.getPaymentMethod());
                paymentResult = adapter.createPayment(new PaymentRequest(
                        saved,
                        vehicle,
                        request.getPaymentAmount(),
                        request.getPaymentMethod(),
                        httpRequest));
            }

            // Gán thông tin thanh toán vào DTO phản hồi để Frontend hiển thị QR hoặc nút bấm chuyển hướng
            dto.setPaymentUrl(paymentResult.paymentUrl());
            dto.setVietQrUrl(paymentResult.vietQrUrl());
            
            // Xây dựng Header Location theo chuẩn RESTful API
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(saved.getBookingId())
                    .toUri();
            
            return ResponseEntity.created(location)
                    .body(ApiResponse.created(dto, "Khởi tạo đơn đặt xe thành công! Vui lòng hoàn tất thanh toán (nếu có)."));
        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.badRequest().body(ApiResponse.error("Thất bại khi khởi tạo đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Xác thực trạng thái thanh toán và cập nhật đơn hàng thành công.
     * Thường được gọi sau khi hệ thống nhận được tín hiệu Webhook/IPN từ phía ngân hàng.
     * 
     * @param id Mã định danh đơn hàng cần xác nhận.
     * @return Kết quả cập nhật trạng thái mới.
     */
    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<ApiResponse<BookingDTO>> confirmPayment(@PathVariable Integer id) {
        try {
            // Cập nhật trạng thái đơn sang Confirmed (Đã xác nhận)
            Booking booking = bookingService.updateStatus(id, Booking.Status.Confirmed);
            return ResponseEntity.ok(ApiResponse.success(convertToDTO(booking), "Xác nhận thanh toán và duyệt đơn thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi xác nhận thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Phương thức nội bộ (Helper) để chuyển đổi thực thể Database sang đối tượng truyền tải dữ liệu.
     * Giúp ẩn đi các trường thông tin nhạy cảm và định dạng lại dữ liệu cho phù hợp với Frontend.
     */
    private BookingDTO convertToDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setBookingId(booking.getBookingId());
        dto.setVehicleId(booking.getVehicle().getVehicleId());
        dto.setVehicleModel(booking.getVehicle().getFullName()); // Trả về tên đầy đủ gồm: Hãng + Dòng xe + Năm sản xuất
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

