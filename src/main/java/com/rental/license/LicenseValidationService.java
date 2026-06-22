package com.rental.license;

import com.rental.entity.DriverLicense;
import com.rental.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LicenseValidationService {

    private static final LocalDate NEW_TRAFFIC_LAW_DATE = LocalDate.of(2025, 1, 1);

    private final List<LicenseValidationStrategy> strategies;

    public void validate(DriverLicense license, Vehicle vehicle) {
        if (license.getExpiryDate() != null && license.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Giấy phép lái xe này đã hết hạn sử dụng. Vui lòng cập nhật GPLX mới.");
        }
        if (license.getIssueDate() == null) {
            throw new RuntimeException("Dữ liệu GPLX không hợp lệ: Thiếu ngày cấp bằng");
        }

        LicenseValidationStrategy strategy = strategies.stream()
                .filter(item -> item.supports(vehicle))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến lược kiểm tra GPLX phù hợp cho phương tiện này."));

        if (!strategy.isValid(license, vehicle)) {
            throw new RuntimeException(String.format(
                    "Hệ thống từ chối: GPLX hạng %s của bạn không được phép điều khiển phương tiện này theo quy định của Bộ GTVT (áp dụng cho bằng cấp %s).",
                    license.getLicenseClass(),
                    isOldLawLicense(license) ? "trước năm 2025" : "từ năm 2025"));
        }
    }

    static boolean isOldLawLicense(DriverLicense license) {
        return license.getIssueDate().isBefore(NEW_TRAFFIC_LAW_DATE);
    }

    static String normalizedClass(DriverLicense license) {
        return license.getLicenseClass() != null ? license.getLicenseClass().toUpperCase() : "";
    }
}
