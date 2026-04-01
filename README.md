# XeNow - Backend Spring Boot Application

## 📌 Giới Thiệu
XeNow là hệ thống API Server quản lý nghiệp vụ và cho thuê xe số hóa, cung cấp bộ công cụ mạnh mẽ dành cho doanh nghiệp và khách hàng cá nhân. Hệ thống được triển khai theo kiến trúc RESTful chuẩn chỉnh.

## 🚀 Các Luồng Nghiệp Vụ Hoạt Động (Backend Workflows)

Chúng tôi đã thiết kế các Controller và Service cho phép xử lý dữ liệu ổn định và an ninh bảo mật:

### 1. Luồng Lọc và Truy Xuất Xe (`VehicleController`)
Sử dụng Stream API của Java kết hợp với `Pageable` để Query phân trang danh sách xe tuỳ theo Model, Location, Brand,...
Tránh nghẽn dữ liệu trên hệ thống khi có trên hàng trăm phương tiện.

> *(Chụp ảnh ERD Database dán dưới đây nha)*
`![Database Entity Model](docs/images/erd.png)`

### 2. Luồng Booking (Nhận Đơn Điện Tử) (`BookingController`)
Sử dụng FPT.AI OCR Endpoint gỡ chép thông tin ảnh từ MultipartFile thành Data Entity thuần.
Logic Validate EngineCapacity bắt buộc (ví dụ bằng A1 chỉ được lái xe dưới 175cc, hệ thống tự động bốc từ mô tả của xe).

> *(Chụp ảnh API Test Postman OCR chèn vào dưới đây)*
`![FPT AI Flow](docs/images/api_fpt_ai.png)`

### 3. Luồng Giao Dịch Đa Thuật Thuế (`VnPayConfig` & API)
* **VietQR:** Tự động sinh Link QRCode từ API đối tác với mô tả sạch chuẩn ngân hàng nội địa `replaceAll([^a-zA-Z0-9 ])`.
* **VNPAY:** Cấu hình Sandbox Test với chữ ký HMAC-SHA512 để trả biến số URL Redirect sang React mượt mà.

> *(Chụp Code VNPAY Hash / VietQR Builder chèn dưới)*
`![Banking Hash Generation](docs/images/payment_config.png)`

### 4. Luồng Quản Trị Hệ Thống (`AdminController`)
Dành riêng cho Staff kiểm kê hóa đơn trả xe. Cập nhật TotalPrice và Status Payment sau khi trừ hao tiền Cọc (Deposit Amount) bằng `BigDecimal` chuẩn xác.

> *(Chụp Test API ReturnVehicle Admin dán ở đây nha)*
`![Admin Flow](docs/images/api_admin.png)`

## 🛠 Nền Tảng Kỹ Thuật (Tech Stack)
* **Framework:** Spring Boot 3
* **Ngôn Ngữ:** Java 17
* **Database:** MySQL & Hibernate JPA
* **Bảo Mật:** Spring Security (Stateless JWT Authentication)
* **Bổ Liệu Cứng:** Lombok

## 📝 Cài Đặt và Chạy

**Yêu cầu:** JDK 17, MySQL 8.0, Maven
* Đảm bảo đã nhét thông tin DataBase của bạn vào `application.properties` (Mặc định `root`/`root` cho DB tên `XeNow`).

```bash
# Clean & cài gói Dependencies qua Maven
mvn clean install

# Khởi chạy ứng dụng
mvn spring-boot:run
```
(Ứng dụng sẽ tự động Run DB Creation và chạy trên Port 8080)
