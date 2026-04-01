package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.RoleDTO;
import com.rental.security.PermissionAuthorizationManager;
import com.rental.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;
    private final PermissionAuthorizationManager permissionAuthorizationManager;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleService.findAll(), "Lấy danh sách Role thành công!"));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<RoleDTO>>> getPagedRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by("id").descending() : Sort.by("id").ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(roleService.findAll(pageable, keyword), "Lấy danh sách Role phân trang thành công!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDTO>> getRoleById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(roleService.findById(id), "Lấy thông tin Role thành công!"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleDTO>> createRole(@RequestBody RoleDTO roleDTO) {
        RoleDTO created = roleService.create(roleDTO);
        permissionAuthorizationManager.loadCache(); // Reload permission cache
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Tạo Role mới thành công!"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDTO>> updateRole(@PathVariable Integer id, @RequestBody RoleDTO roleDTO) {
        RoleDTO updated = roleService.update(id, roleDTO);
        permissionAuthorizationManager.loadCache(); // Reload permission cache
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật Role thành công!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Integer id) {
        roleService.delete(id);
        permissionAuthorizationManager.loadCache(); // Reload permission cache
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa Role thành công!"));
    }
}
