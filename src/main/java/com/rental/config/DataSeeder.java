package com.rental.config;

import com.rental.entity.*;
import com.rental.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final LocationRepository locationRepo;
    private final VehicleTypeRepository typeRepo;
    private final VehicleRepository vehicleRepo;
    private final ManagerRepository managerRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (managerRepo.count() > 0) return; // Chỉ seed lần đầu

        // Locations
        Location hcm = locationRepo.save(Location.builder()
                .address("123 Nguyễn Huệ").city("TP. Hồ Chí Minh").country("Việt Nam").phone("0281234567").build());
        Location hn = locationRepo.save(Location.builder()
                .address("45 Hoàn Kiếm").city("Hà Nội").country("Việt Nam").phone("0241234567").build());
        Location dn = locationRepo.save(Location.builder()
                .address("78 Bạch Đằng").city("Đà Nẵng").country("Việt Nam").phone("0236123456").build());

        // Vehicle Types
        VehicleType xe_may = typeRepo.save(VehicleType.builder().typeName("Xe máy").build());
        VehicleType o_to   = typeRepo.save(VehicleType.builder().typeName("Ô tô").build());
        VehicleType xe_tai = typeRepo.save(VehicleType.builder().typeName("Xe tải").build());

        // Vehicles
        vehicleRepo.save(Vehicle.builder().type(o_to).location(hcm).licensePlate("51A-12345")
                .brand("Toyota").model("Vios").color("Trắng").yearMade(2022)
                .dailyRate(new BigDecimal("600000")).depositAmount(new BigDecimal("5000000"))
                .status(Vehicle.Status.Available).build());
        vehicleRepo.save(Vehicle.builder().type(o_to).location(hcm).licensePlate("51B-67890")
                .brand("Honda").model("City").color("Đen").yearMade(2023)
                .dailyRate(new BigDecimal("700000")).depositAmount(new BigDecimal("6000000"))
                .status(Vehicle.Status.Available).build());
        vehicleRepo.save(Vehicle.builder().type(xe_may).location(hn).licensePlate("29H-11111")
                .brand("Honda").model("Air Blade").color("Đỏ").yearMade(2023)
                .dailyRate(new BigDecimal("150000")).depositAmount(new BigDecimal("2000000"))
                .status(Vehicle.Status.Available).build());
        vehicleRepo.save(Vehicle.builder().type(xe_may).location(dn).licensePlate("43A-22222")
                .brand("Yamaha").model("NVX").color("Xanh").yearMade(2022)
                .dailyRate(new BigDecimal("180000")).depositAmount(new BigDecimal("2500000"))
                .status(Vehicle.Status.Available).build());
        vehicleRepo.save(Vehicle.builder().type(o_to).location(dn).licensePlate("43B-33333")
                .brand("Hyundai").model("Accent").color("Bạc").yearMade(2021)
                .dailyRate(new BigDecimal("550000")).depositAmount(new BigDecimal("4500000"))
                .status(Vehicle.Status.Available).build());
        vehicleRepo.save(Vehicle.builder().type(xe_tai).location(hcm).licensePlate("51C-44444")
                .brand("Isuzu").model("QKR77H").color("Trắng").yearMade(2020)
                .dailyRate(new BigDecimal("1200000")).depositAmount(new BigDecimal("10000000"))
                .status(Vehicle.Status.Available).build());

        // Admin Manager
        managerRepo.save(Manager.builder()
                .name("Quản trị viên")
                .username("admin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .email("admin@driveeasy.vn")
                .phone("0900000000")
                .role(Manager.Role.Admin)
                .location(hcm)
                .build());

        System.out.println("✅ Seeded: 3 locations, 3 vehicle types, 6 vehicles, 1 admin manager");
        System.out.println("👤 Admin login: username=admin / password=admin123");
    }
}
