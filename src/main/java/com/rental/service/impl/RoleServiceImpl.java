package com.rental.service.impl;

import com.rental.dto.PermissionDTO;
import com.rental.dto.RoleDTO;
import com.rental.entity.Permission;
import com.rental.entity.Role;
import com.rental.exception.ResourceNotFoundException;
import com.rental.repository.PermissionRepository;
import com.rental.repository.RoleRepository;
import com.rental.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public Page<RoleDTO> findAll(Pageable pageable, String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return roleRepository.search(keyword, pageable).map(this::convertToDTO);
        }
        return roleRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Override
    public List<RoleDTO> findAll() {
        return roleRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDTO findById(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return convertToDTO(role);
    }

    @Override
    @Transactional
    public RoleDTO create(RoleDTO roleDTO) {
        Role role = Role.builder()
                .name(roleDTO.getName())
                .description(roleDTO.getDescription())
                .build();

        if (roleDTO.getPermissions() != null) {
            List<Permission> permissions = roleDTO.getPermissions().stream()
                    .map(p -> permissionRepository.findById(p.getId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Permission not found with id: " + p.getId())))
                    .collect(Collectors.toList());
            role.setPermissions(permissions);
        }

        return convertToDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleDTO update(Integer id, RoleDTO roleDTO) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        role.setName(roleDTO.getName());
        role.setDescription(roleDTO.getDescription());

        if (roleDTO.getPermissions() != null) {
            List<Permission> permissions = roleDTO.getPermissions().stream()
                    .map(p -> permissionRepository.findById(p.getId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Permission not found with id: " + p.getId())))
                    .collect(Collectors.toList());
            role.setPermissions(permissions);
        } else {
            role.setPermissions(new ArrayList<>());
        }

        return convertToDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        // Remove link to users or just handle it at DB level with FK
        // In current project, role-user relation is @ManyToOne in User
        // Check if role has users
        if (!role.getUsers().isEmpty()) {
            throw new RuntimeException("Cannot delete role as it is associated with users");
        }

        roleRepository.delete(role);
    }

    private RoleDTO convertToDTO(Role role) {
        List<PermissionDTO> permissionDTOs = null;
        if (role.getPermissions() != null) {
            permissionDTOs = role.getPermissions().stream()
                    .map(p -> PermissionDTO.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .apiPath(p.getApiPath())
                            .method(p.getMethod())
                            .module(p.getModule())
                            .build())
                    .collect(Collectors.toList());
        }

        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissionDTOs)
                .build();
    }
}
