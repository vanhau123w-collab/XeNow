package com.rental.service;

import com.rental.entity.Permission;
import java.util.List;

public interface PermissionService {
    List<Permission> findAll();
    Permission findById(Integer id);
    Permission create(Permission permission);
    Permission update(Integer id, Permission permission);
    void delete(Integer id);
}
