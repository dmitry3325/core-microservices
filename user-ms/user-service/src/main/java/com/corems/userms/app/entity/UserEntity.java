package com.corems.userms.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import net.minidev.json.annotate.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@ToString
@Entity
@Table(name = "app_user")
public class UserEntity {

    public UserEntity() {
        this.uuid = UUID.randomUUID();
    }

    public static UserEntityBuilder builder() {
        return new CustomUserBuilder();
    }

    private static class CustomUserBuilder extends UserEntityBuilder {
        @Override
        public UserEntity build() {
            this.uuid(UUID.randomUUID());
            return super.build();
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private Collection<LoginTokenEntity> tokens = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private Collection<RoleEntity> roles = new ArrayList<>();

    @NotNull
    private String provider;

    @NonNull
    @Column(unique = true)
    private String email;

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    private String imageUrl;

    @Column(length = 50)
    private String phoneNumber;

    @JsonIgnore
    private String password;

    @NotNull
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @NotNull
    @UpdateTimestamp
    private Instant updatedAt;

    private Instant lastLoginAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}