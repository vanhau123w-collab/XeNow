package com.rental.config;

import com.rental.entity.*;
import com.rental.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

        private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
        private static final String DEFAULT_PASSWORD = "admin123";

        private final PermissionRepository permissionRepository;
        private final RoleRepository roleRepository;
        private final UserRepository userRepository;
        private final LocationRepository locationRepository;
        private final BrandRepository brandRepository;
        private final ModelRepository modelRepository;
        private final VehicleRepository vehicleRepository;
        private final CustomerRepository customerRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public void run(String... args) {
                if (userRepository.count() > 0) {
                        log.info(">>> Database already seeded — skipping");
                        return;
                }

                log.info(">>> Seeding database...");

                List<Permission> permissions = seedPermissions();
                List<Role> roles = seedRoles(permissions);
                seedUsers(roles);
                seedBusinessData();

                log.info(">>> Database seeded successfully");
        }

        private List<Permission> seedPermissions() {
                List<Permission> permissions = new ArrayList<>();

                // ===== USER admin module: /api/admin/users =====
                permissions.add(createPermission("ADMIN_VIEW_ALL_USER", "/api/admin/users", "GET", "USER"));
                permissions.add(createPermission("ADMIN_UPDATE_USER_STATUS", "/api/admin/users/*/status", "POST", "USER"));
                permissions.add(createPermission("ADMIN_UPDATE_USER_ROLES", "/api/admin/users/*/roles", "POST", "USER"));
                // Fallback CRUD for general /api/users (used by regular users/profile)
                addCrudPermissions(permissions, "USER", "/api/users");

                // ===== CUSTOMER admin module: /api/admin/customers =====
                permissions.add(createPermission("VIEW_ALL_CUSTOMER", "/api/admin/customers", "GET", "CUSTOMER"));
                // CUSTOMER self-service module: /api/customer
                permissions.add(createPermission("VIEW_CUSTOMER", "/api/customer/**", "GET", "CUSTOMER"));
                permissions.add(createPermission("VERIFY_CUSTOMER", "/api/customer/verify", "POST", "CUSTOMER"));
                permissions.add(createPermission("UPDATE_CUSTOMER_SELF", "/api/customer/**", "PUT", "CUSTOMER"));

                // ===== BOOKING module: /api/bookings =====
                permissions.add(createPermission("CREATE_BOOKING", "/api/bookings", "POST", "BOOKING"));
                permissions.add(createPermission("VIEW_ALL_BOOKING", "/api/bookings/**", "GET", "BOOKING"));
                permissions.add(createPermission("VIEW_BOOKING", "/api/bookings/{id}", "GET", "BOOKING"));
                permissions.add(createPermission("UPDATE_BOOKING", "/api/bookings/**", "PUT", "BOOKING"));
                permissions.add(createPermission("CANCEL_BOOKING", "/api/bookings/*/cancel", "POST", "BOOKING"));
                // Admin booking management
                permissions.add(createPermission("ADMIN_VIEW_ALL_BOOKING", "/api/admin/bookings", "GET", "BOOKING"));
                permissions.add(createPermission("ADMIN_UPDATE_BOOKING_STATUS", "/api/admin/bookings/*/status", "POST",
                                "BOOKING"));

                // ===== BRANCH (Location) admin module: /api/admin/locations =====
                addCrudPermissions(permissions, "BRANCH", "/api/admin/locations");
                permissions.add(createPermission("VIEW_ALL_PUBLIC_BRANCH", "/api/locations", "GET", "BRANCH"));

                // ===== BRAND admin module: /api/admin/brands =====
                addCrudPermissions(permissions, "BRAND", "/api/admin/brands");

                // ===== MODEL admin module: /api/admin/models =====
                addCrudPermissions(permissions, "MODEL", "/api/admin/models");

                // ===== VEHICLE admin module: /api/admin/vehicles =====
                addCrudPermissions(permissions, "VEHICLE", "/api/admin/vehicles");
                permissions.add(createPermission("VIEW_ALL_PUBLIC_VEHICLE", "/api/vehicles", "GET", "VEHICLE"));
                permissions.add(createPermission("VIEW_PUBLIC_VEHICLE", "/api/vehicles/{id}", "GET", "VEHICLE"));
                // Vehicle management extras
                permissions.add(createPermission("UPLOAD_VEHICLE_IMAGE", "/api/admin/vehicles/*/images", "POST",
                                "VEHICLE"));
                permissions.add(createPermission("DELETE_VEHICLE_IMAGE", "/api/admin/vehicles/images/*", "DELETE",
                                "VEHICLE"));
                permissions.add(createPermission("SET_PRIMARY_IMAGE", "/api/admin/vehicles/images/*/primary", "PUT",
                                "VEHICLE"));
                permissions.add(createPermission("UPDATE_VEHICLE_STATUS", "/api/admin/vehicles/*/status", "POST",
                                "VEHICLE"));
                permissions.add(createPermission("COMPLETE_MAINTENANCE", "/api/admin/vehicles/*/maintenance/complete",
                                "POST", "VEHICLE"));

                // ===== DASHBOARD module =====
                permissions.add(createPermission("VIEW_DASHBOARD", "/api/admin/dashboard", "GET", "DASHBOARD"));

                // ===== REPORT module =====
                permissions.add(createPermission("VIEW_REPORT", "/api/admin/reports", "GET", "REPORT"));

                // ===== PERMISSION module: /api/permissions =====
                addCrudPermissions(permissions, "PERMISSION", "/api/permissions");

                // ===== ROLE module: /api/roles =====
                addCrudPermissions(permissions, "ROLE", "/api/roles");

                List<Permission> saved = permissionRepository.saveAll(permissions);
                permissionRepository.flush();
                log.info("Seeded {} permissions", saved.size());
                return saved;
        }

        private void addCrudPermissions(List<Permission> list, String module, String basePath) {
                list.add(createPermission("CREATE_" + module, basePath, "POST", module));
                list.add(createPermission("UPDATE_" + module, basePath + "/{id}", "PUT", module));
                list.add(createPermission("DELETE_" + module, basePath + "/{id}", "DELETE", module));
                list.add(createPermission("VIEW_ALL_" + module, basePath, "GET", module));
                list.add(createPermission("VIEW_" + module, basePath + "/{id}", "GET", module));
                // Add paged variation if relevant (for Roles/Permissions)
                if (module.equals("ROLE") || module.equals("PERMISSION")) {
                        list.add(createPermission("VIEW_PAGED_" + module, basePath + "/paged", "GET", module));
                }
        }

        private Permission createPermission(String name, String apiPath, String method, String module) {
                return Permission.builder()
                                .name(name)
                                .apiPath(apiPath)
                                .method(method)
                                .module(module)
                                .build();
        }

        private List<Role> seedRoles(List<Permission> allPermissions) {
                // ADMIN — all permissions
                Role admin = createRole("ADMIN", "Quản trị viên toàn hệ thống", allPermissions);

                // MANAGER — crud except delete for most, view for setup
                List<String> managerPermissionNames = allPermissions.stream()
                                .map(Permission::getName)
                                .filter(name -> !name.startsWith("DELETE_") && !name.contains("PERMISSION")
                                                && !name.contains("ROLE"))
                                .collect(Collectors.toList());
                managerPermissionNames.add("VIEW_ALL_ROLE");
                managerPermissionNames.add("VIEW_ALL_PERMISSION");
                managerPermissionNames.add("VIEW_PAGED_ROLE");
                managerPermissionNames.add("VIEW_PAGED_PERMISSION");
                managerPermissionNames.add("ADMIN_VIEW_ALL_USER"); // Cập nhật tên quyền Admin của User
                Role manager = createRole("MANAGER", "Quản lý kinh doanh",
                                filterPermissions(allPermissions, managerPermissionNames));

                // STAFF — view all, update vehicles/bookings, booking management
                List<String> staffPermissionNames = allPermissions.stream()
                                .map(Permission::getName)
                                .filter(name -> name.startsWith("VIEW_") || name.contains("VEHICLE")
                                                || name.contains("MODEL") || name.contains("BOOKING")
                                                || name.contains("REPORT"))
                                .filter(name -> !name.startsWith("DELETE_"))
                                .collect(Collectors.toList());
                Role staff = createRole("STAFF", "Nhân viên vận hành",
                                filterPermissions(allPermissions, staffPermissionNames));

                // CUSTOMER — booking, customer self-service, public view
                List<String> customerPermissionNames = allPermissions.stream()
                                .map(Permission::getName)
                                .filter(name -> name.contains("PUBLIC")
                                                || name.contains("BOOKING")
                                                || name.equals("VIEW_CUSTOMER")
                                                || name.equals("UPDATE_CUSTOMER_SELF")
                                                || name.equals("VERIFY_CUSTOMER"))
                                .collect(Collectors.toList());
                Role customer = createRole("CUSTOMER", "Khách hàng",
                                filterPermissions(allPermissions, customerPermissionNames));

                List<Role> saved = roleRepository.saveAll(List.of(admin, manager, staff, customer));
                roleRepository.flush();
                log.info("Seeded {} roles: ADMIN, MANAGER, STAFF, CUSTOMER", saved.size());
                return saved;
        }

        private Role createRole(String name, String description, List<Permission> permissions) {
                return Role.builder()
                                .name(name)
                                .description(description)
                                .permissions(permissions)
                                .build();
        }

        private List<Permission> filterPermissions(List<Permission> all, List<String> names) {
                return all.stream()
                                .filter(p -> names.contains(p.getName()))
                                .collect(Collectors.toList());
        }

        private void seedUsers(List<Role> allRoles) {
                String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

                Role adminRole = allRoles.stream().filter(r -> r.getName().equals("ADMIN")).findFirst().get();
                Role managerRole = allRoles.stream().filter(r -> r.getName().equals("MANAGER")).findFirst().get();
                Role staffRole = allRoles.stream().filter(r -> r.getName().equals("STAFF")).findFirst().get();
                Role customerRole = allRoles.stream().filter(r -> r.getName().equals("CUSTOMER")).findFirst().get();

                userRepository.saveAll(List.of(
                                createUser("admin", "admin@xenow.vn", encodedPassword, "Hệ Thống - Admin", adminRole),
                                createUser("manager", "manager@xenow.vn", encodedPassword, "Hệ Thống - Manager",
                                                managerRole),
                                createUser("staff", "staff@xenow.vn", encodedPassword, "Hệ Thống - Staff", staffRole),
                                createUser("customer1", "khach1@xenow.vn", encodedPassword, "Nguyễn Văn Khách",
                                                customerRole)));
                userRepository.flush(); // Ensure IDs are generated for reference in business data
        }

        private User createUser(String username, String email, String password, String fullName, Role role) {
                return User.builder()
                                .username(username)
                                .email(email)
                                .password(password)
                                .fullName(fullName)
                                .roles(java.util.Set.of(role))
                                .status(User.Status.Active)
                                .build();
        }

        private void seedBusinessData() {
                // Locations
                Location hcm = locationRepository.save(Location.builder()
                                .branchName("Chi nhánh TP. Hồ Chí Minh")
                                .address("123 Nguyễn Huệ").city("TP. Hồ Chí Minh").phone("0281234567").build());
                Location hn = locationRepository.save(Location.builder()
                                .branchName("Chi nhánh Hà Nội")
                                .address("45 Hoàn Kiếm").city("Hà Nội").phone("0241234567").build());

                // Brands
                Brand toyota = brandRepository.save(Brand.builder().brandName("Toyota").build());
                Brand honda = brandRepository.save(Brand.builder().brandName("Honda").build());

                // Models
                Model vios = modelRepository.save(Model.builder().modelName("Vios").brand(toyota).build());
                Model crv = modelRepository.save(Model.builder().modelName("CR-V").brand(honda).build());

                // Vehicles
                vehicleRepository.save(Vehicle.builder()
                                .type("Xe Ô Tô")
                                .currentLocation(hcm)
                                .licensePlate("51A-12345")
                                .model(vios)
                                .manufactureYear(2022)
                                .mileage(15000)
                                .pricePerDay(new BigDecimal("600000"))
                                .status(Vehicle.Status.Available)
                                .fuelType("Xăng")
                                .transmission("Tự động")
                                .seats(4)
                                .build());

                vehicleRepository.save(Vehicle.builder()
                                .type("Xe Ô Tô")
                                .currentLocation(hn)
                                .licensePlate("29H-11111")
                                .model(crv)
                                .manufactureYear(2023)
                                .mileage(5000)
                                .pricePerDay(new BigDecimal("1200000"))
                                .status(Vehicle.Status.Available)
                                .fuelType("Xăng")
                                .transmission("Tự động")
                                .seats(7)
                                .build());

                // Link customer entity to customer1 user
                userRepository.findByUsername("customer1").ifPresent(user -> {
                        customerRepository.save(Customer.builder()
                                        .user(user)
                                        .identityCard("001099123456")
                                        .address("456 Lê Lợi, TP.HCM")
                                        .build());
                });
        }
}
