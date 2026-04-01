package com.rental.service.impl;

import com.rental.dto.PermissionDTO;
import com.rental.entity.Permission;
import com.rental.repository.PermissionRepository;
import com.rental.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    public List<PermissionDTO> findAll() {
        return permissionRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PermissionDTO> findAll(Pageable pageable, String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return permissionRepository.search(keyword, pageable)
                    .map(this::convertToDTO);
        }
        return permissionRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Override
    public PermissionDTO findById(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quyền với ID: " + id));
        return convertToDTO(permission);
    }

    @Override
    @Transactional
    public PermissionDTO create(PermissionDTO permissionDTO) {
        if (permissionRepository.findByName(permissionDTO.getName()).isPresent()) {
            throw new RuntimeException("Quyền '" + permissionDTO.getName() + "' đã tồn tại!");
        }
        Permission permission = mapToEntity(permissionDTO);
        return convertToDTO(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public PermissionDTO update(Integer id, PermissionDTO permissionDTO) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quyền với ID: " + id));
        
        permission.setName(permissionDTO.getName());
        permission.setApiPath(permissionDTO.getApiPath());
        permission.setMethod(permissionDTO.getMethod());
        permission.setModule(permissionDTO.getModule());
        
        return convertToDTO(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quyền với ID: " + id));
        permissionRepository.delete(permission);
    }

    private PermissionDTO convertToDTO(Permission permission) {
        if (permission == null) return null;
        return PermissionDTO.builder()
                .id(permission.getId())
                .name(permission.getName())
                .apiPath(permission.getApiPath())
                .method(permission.getMethod())
                .module(permission.getModule())
                .build();
    }

    private Permission mapToEntity(PermissionDTO dto) {
        if (dto == null) return null;
        return Permission.builder()
                .id(dto.getId())
                .name(dto.getName())
                .apiPath(dto.getApiPath())
                .method(dto.getMethod())
                .module(dto.getModule())
                .build();
    }
}
