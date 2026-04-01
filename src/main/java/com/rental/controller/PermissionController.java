package com.rental.controller;

import com.rental.dto.ApiResponse;
import com.rental.dto.PermissionDTO;
import com.rental.security.PermissionAuthorizationManager;
import com.rental.service.PermissionService;
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
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    private final PermissionService permissionService;
    private final PermissionAuthorizationManager permissionAuthorizationManager;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.success(permissionService.findAll(), "Lấy danh sách quyền thành công"));
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<PermissionDTO>>> getPagedPermissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by("id").descending() : Sort.by("id").ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(permissionService.findAll(pageable, keyword), "Lấy danh sách quyền (phân trang) thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionDTO>> getPermissionById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.findById(id), "Lấy thông tin quyền thành công"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PermissionDTO>> createPermission(@RequestBody PermissionDTO permissionDTO) {
        PermissionDTO created = permissionService.create(permissionDTO);
        permissionAuthorizationManager.loadCache(); // Reload cache
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Tạo quyền mới thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionDTO>> updatePermission(@PathVariable Integer id, @RequestBody PermissionDTO permissionDTO) {
        PermissionDTO updated = permissionService.update(id, permissionDTO);
        permissionAuthorizationManager.loadCache(); // Reload cache
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật quyền thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deletePermission(@PathVariable Integer id) {
        permissionService.delete(id);
        permissionAuthorizationManager.loadCache(); // Reload cache
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa quyền thành công"));
    }
}
