package com.rental.service;

import com.rental.dto.PermissionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PermissionService {
    List<PermissionDTO> findAll();
    Page<PermissionDTO> findAll(Pageable pageable, String keyword);
    PermissionDTO findById(Integer id);
    PermissionDTO create(PermissionDTO permissionDTO);
    PermissionDTO update(Integer id, PermissionDTO permissionDTO);
    void delete(Integer id);
}
