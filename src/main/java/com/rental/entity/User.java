package com.rental.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "User")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    public enum Status {
        Active, Disabled, Pending
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "Username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "Password", nullable = false, length = 255)
    private String password;

    @Column(name = "FullName", length = 100)
    private String fullName;

    @Column(name = "Email", unique = true, length = 100)
    private String email;

    @Column(name = "Phone", length = 20)
    private String phone;

    @Column(name = "Address", length = 255)
    private String address;

    @Column(name = "Gender", length = 20)
    private String gender;

    @Column(name = "Avatar", length = 255)
    private String avatar;

    @Column(name = "DateOfBirth")
    private java.time.LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status")
    @Builder.Default
    private Status status = Status.Active;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<Role> roles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<RefreshToken> refreshTokens;
}
