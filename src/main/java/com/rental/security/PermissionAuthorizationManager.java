package com.rental.security;

import com.rental.entity.Permission;
import com.rental.entity.Role;
import com.rental.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Dynamic permission-based authorization manager.
 * - ADMIN role bypasses all checks (full access).
 * - Other roles: checks if the user's role has a permission matching request
 * path + HTTP method.
 */
@Component
@Slf4j
public class PermissionAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final RoleRepository roleRepository;
    private final com.rental.repository.UserRepository userRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Cache: "ROLE_ADMIN" → List<Permission>
    private volatile Map<String, List<Permission>> rolePermissionsCache;

    public PermissionAuthorizationManager(RoleRepository roleRepository, com.rental.repository.UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Reload cache from database. Called on startup and whenever admin modifies
     * role/permission.
     */
    public synchronized void loadCache() {
        log.info("[AUTH] Loading standard permissions into cache...");
        try {
            List<Role> roles = roleRepository.findAllWithPermissions();

            Map<String, List<Permission>> cache = new HashMap<>();
            for (Role role : roles) {
                String cacheKey = "ROLE_" + role.getName();
                cache.computeIfAbsent(cacheKey, k -> new ArrayList<>())
                        .addAll(role.getPermissions());
                log.info("[AUTH] Cached role: {} with {} permissions", cacheKey, role.getPermissions().size());
            }
            this.rolePermissionsCache = cache;
            log.info("[AUTH] Cache loaded for {} roles: {}", cache.size(), cache.keySet());
        } catch (Exception e) {
            log.error("[AUTH] Failed to load role permissions cache!", e);
            // Fallback to empty cache so ADMIN bypass still works
            this.rolePermissionsCache = new HashMap<>();
        }
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authSupplier,
            RequestAuthorizationContext context) {

        if (rolePermissionsCache == null) {
            loadCache();
        }

        Authentication authentication = authSupplier.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("[AUTH] No authentication or not authenticated");
            return new AuthorizationDecision(false);
        }

        // Check if user is Disabled - INSTANT LOCKING
        try {
            com.rental.entity.User user = userRepository.findByUsername(authentication.getName())
                    .or(() -> userRepository.findByEmail(authentication.getName()))
                    .orElse(null);
            
            if (user != null && user.getStatus() != com.rental.entity.User.Status.Active) {
                log.warn("[AUTH] ❌ User '{}' is {} - ACCESS DENIED", 
                    authentication.getName(), user.getStatus());
                return new AuthorizationDecision(false);
            }
        } catch (Exception e) {
            log.warn("[AUTH] Failed to check user status for '{}': {}", 
                authentication.getName(), e.getMessage());
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String requestPath = context.getRequest().getRequestURI();
        String httpMethod = context.getRequest().getMethod();

        // Log all authorities for debugging
        List<String> authorityNames = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        log.info("[AUTH] Checking: {} {} | user={} | authType={} | authorities={}",
                httpMethod, requestPath, authentication.getName(),
                authentication.getClass().getSimpleName(), authorityNames);

        // ADMIN bypass: if user has ROLE_ADMIN, grant access to everything
        for (GrantedAuthority authority : authorities) {
            String roleName = authority.getAuthority();
            if ("ROLE_ADMIN".equals(roleName)) {
                log.info("[AUTH] ✅ ADMIN bypass granted for {} {}", httpMethod, requestPath);
                return new AuthorizationDecision(true);
            }
        }

        // For other roles: check against cached permissions
        for (GrantedAuthority authority : authorities) {
            String roleName = authority.getAuthority();

            List<Permission> permissions = rolePermissionsCache
                    .getOrDefault(roleName, Collections.emptyList());

            for (Permission perm : permissions) {
                if (perm.getMethod().equalsIgnoreCase(httpMethod)
                        && pathMatcher.match(perm.getApiPath(), requestPath)) {
                    log.info("[AUTH] ✅ Access granted for role {}: {} {}", roleName, httpMethod, requestPath);
                    return new AuthorizationDecision(true);
                }
            }
        }

        log.warn("[AUTH] ❌ ACCESS DENIED for user {}: {} {} | had authorities: {}",
                authentication.getName(), httpMethod, requestPath, authorityNames);
        return new AuthorizationDecision(false);
    }
}
