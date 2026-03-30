package com.rental.controller;

import com.rental.entity.Permission;
import com.rental.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPermissionById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(permissionService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createPermission(@RequestBody Permission permission) {
        try {
            return ResponseEntity.ok(permissionService.create(permission));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePermission(@PathVariable Integer id, @RequestBody Permission permissionDetails) {
        try {
            return ResponseEntity.ok(permissionService.update(id, permissionDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePermission(@PathVariable Integer id) {
        try {
            permissionService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Xóa quyền thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
