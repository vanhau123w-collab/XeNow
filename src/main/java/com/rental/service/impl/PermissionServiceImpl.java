package com.rental.service.impl;

import com.rental.entity.Permission;
import com.rental.repository.PermissionRepository;
import com.rental.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    @Override
    public Permission findById(Integer id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quyền với ID: " + id));
    }

    @Override
    @Transactional
    public Permission create(Permission permission) {
        if (permissionRepository.findByName(permission.getName()).isPresent()) {
            throw new RuntimeException("Quyền '" + permission.getName() + "' đã tồn tại!");
        }
        return permissionRepository.save(permission);
    }

    @Override
    @Transactional
    public Permission update(Integer id, Permission permissionDetails) {
        Permission permission = findById(id);
        permission.setName(permissionDetails.getName());
        permission.setApiPath(permissionDetails.getApiPath());
        permission.setMethod(permissionDetails.getMethod());
        permission.setModule(permissionDetails.getModule());
        return permissionRepository.save(permission);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Permission permission = findById(id);
        permissionRepository.delete(permission);
    }
}
