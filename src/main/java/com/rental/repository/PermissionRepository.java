package com.rental.repository;

import com.rental.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByName(String name);

    @Query("SELECT p FROM Permission p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.apiPath) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.module) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Permission> search(String keyword, Pageable pageable);
}
