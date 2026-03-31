package com.rental.config;

import com.rental.entity.*;
import com.rental.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final LocationRepository locationRepo;
    private final BrandRepository brandRepo;
    private final ModelRepository modelRepo;
    private final VehicleRepository vehicleRepo;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final CustomerRepository customerRepo;
    private final BookingRepository bookingRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepo.count() > 0) return; // Chỉ seed lần đầu

        // Roles - kiểm tra trước khi tạo
        roleRepo.findByRoleName("ADMIN")
                .orElseGet(() -> roleRepo.save(Role.builder().roleName("ADMIN").build()));
        roleRepo.findByRoleName("STAFF")
                .orElseGet(() -> roleRepo.save(Role.builder().roleName("STAFF").build()));
        roleRepo.findByRoleName("CUSTOMER")
                .orElseGet(() -> roleRepo.save(Role.builder().roleName("CUSTOMER").build()));



        // Locations
        if (locationRepo.count() == 0) {
            Location hcm = locationRepo.save(Location.builder()
                    .branchName("Chi nhánh TP. Hồ Chí Minh")
                    .address("123 Nguyễn Huệ").city("TP. Hồ Chí Minh").phone("0281234567").build());
            Location hn = locationRepo.save(Location.builder()
                    .branchName("Chi nhánh Hà Nội")
                    .address("45 Hoàn Kiếm").city("Hà Nội").phone("0241234567").build());
            Location dn = locationRepo.save(Location.builder()
                    .branchName("Chi nhánh Đà Nẵng")
                    .address("78 Bạch Đằng").city("Đà Nẵng").phone("0236123456").build());

            // Brands
            Brand toyota = brandRepo.save(Brand.builder().brandName("Toyota").build());
            Brand honda = brandRepo.save(Brand.builder().brandName("Honda").build());
            Brand mazda = brandRepo.save(Brand.builder().brandName("Mazda").build());
            Brand ford = brandRepo.save(Brand.builder().brandName("Ford").build());

            // Models
            Model vios = modelRepo.save(Model.builder().modelName("Vios").brand(toyota).build());
            Model crv = modelRepo.save(Model.builder().modelName("CR-V").brand(honda).build());
            Model m3 = modelRepo.save(Model.builder().modelName("Mazda 3").brand(mazda).build());
            Model everest = modelRepo.save(Model.builder().modelName("Everest").brand(ford).build());

            // Vehicles
            vehicleRepo.save(Vehicle.builder().type("Xe Ô Tô").currentLocation(hcm).licensePlate("51A-12345")
                    .model(vios).manufactureYear(2022).mileage(15000)
                    .pricePerDay(new BigDecimal("600000")).status(Vehicle.Status.Available).build());
            vehicleRepo.save(Vehicle.builder().type("Xe Ô Tô").currentLocation(hcm).licensePlate("51B-67890")
                    .model(crv).manufactureYear(2023).mileage(8000)
                    .pricePerDay(new BigDecimal("1200000")).status(Vehicle.Status.Available).build());
            vehicleRepo.save(Vehicle.builder().type("Xe Ô Tô").currentLocation(hn).licensePlate("29H-11111")
                    .model(m3).manufactureYear(2023).mileage(5000)
                    .pricePerDay(new BigDecimal("800000")).status(Vehicle.Status.Available).build());
            vehicleRepo.save(Vehicle.builder().type("Xe Ô Tô").currentLocation(dn).licensePlate("43A-22222")
                    .model(everest).manufactureYear(2022).mileage(20000)
                    .pricePerDay(new BigDecimal("1500000")).status(Vehicle.Status.Available).build());
        }

        // Admin user
        if (!userRepo.findByUsername("admin").isPresent()) {
            userRepo.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Quản trị viên")
                    .email("admin@xenow.vn")
                    .phone("0900000000")
                    .status(User.Status.Active)
                    .role(roleRepo.findByRoleName("ADMIN").get())
                    .build());
        }

        // Sample Customer
        if (!userRepo.findByUsername("customer1").isPresent()) {
            User customerUser = userRepo.save(User.builder()
                    .username("customer1")
                    .password(passwordEncoder.encode("customer123"))
                    .fullName("Nguyễn Văn Khách")
                    .email("khach1@xenow.vn")
                    .phone("0987654321")
                    .status(User.Status.Active)
                    .role(roleRepo.findByRoleName("CUSTOMER").get())
                    .build());
            
            Customer customer = customerRepo.save(Customer.builder()
                    .user(customerUser)
                    .identityCard("001099123456")
                    .driverLicense("GPLX123456")
                    .address("456 Lê Lợi, TP.HCM")
                    .build());

            // Sample Bookings
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            List<Vehicle> allVehicles = vehicleRepo.findAll();
            
            if (!allVehicles.isEmpty()) {
                // 1. Pending Booking
                bookingRepo.save(Booking.builder()
                        .customer(customer)
                        .vehicle(allVehicles.get(0))
                        .startDate(now.plusDays(1))
                        .endDate(now.plusDays(3))
                        .totalPrice(new BigDecimal("1200000"))
                        .status(Booking.Status.Pending)
                        .build());

                // 2. Confirmed Booking
                bookingRepo.save(Booking.builder()
                        .customer(customer)
                        .vehicle(allVehicles.get(1))
                        .startDate(now.minusDays(1))
                        .endDate(now.plusDays(2))
                        .totalPrice(new BigDecimal("3600000"))
                        .status(Booking.Status.Confirmed)
                        .build());

                // 3. Ongoing Booking (Vehicle should be Rented)
                Vehicle v2 = allVehicles.get(2);
                v2.setStatus(Vehicle.Status.Rented);
                vehicleRepo.save(v2);

                bookingRepo.save(Booking.builder()
                        .customer(customer)
                        .vehicle(v2)
                        .startDate(now.minusDays(2))
                        .endDate(now.plusDays(1))
                        .totalPrice(new BigDecimal("1600000"))
                        .status(Booking.Status.Ongoing)
                        .build());
                
                // 4. Completed Booking
                bookingRepo.save(Booking.builder()
                        .customer(customer)
                        .vehicle(allVehicles.get(3))
                        .startDate(now.minusDays(10))
                        .endDate(now.minusDays(7))
                        .totalPrice(new BigDecimal("4500000"))
                        .status(Booking.Status.Completed)
                        .returnMileage(20500)
                        .returnNote("Xe trả đúng hạn, sạch sẽ.")
                        .build());
            }
        }

        System.out.println("✅ Seeded: Roles, Locations, Brands, Vehicles, Admin, Customer & Bookings");
        System.out.println("👤 Admin login: admin / admin123");
        System.out.println("👤 Customer login: customer1 / customer123");
    }
}
