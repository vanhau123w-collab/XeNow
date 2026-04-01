package com.rental.service;

import com.rental.dto.RoleDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface RoleService {
    Page<RoleDTO> findAll(Pageable pageable, String keyword);
    List<RoleDTO> findAll();
    RoleDTO findById(Integer id);
    RoleDTO create(RoleDTO roleDTO);
    RoleDTO update(Integer id, RoleDTO roleDTO);
    void delete(Integer id);
}
