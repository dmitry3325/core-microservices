package com.corems.authms.entity;

import com.corems.authms.controller.model.enums.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@ToString
@Entity
@Table(name = "app_user")
public class User {

    public User() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        this.lastLogin = OffsetDateTime.now();
    }

    public static UserBuilder builder() {
        return new CustomUserBuilder();
    }

    private static class CustomUserBuilder extends UserBuilder {
        @Override
        public User build() {
            this.createdAt(OffsetDateTime.now());
            this.updatedAt(OffsetDateTime.now());
            this.lastLogin(OffsetDateTime.now());
            return super.build();
        }
    }


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true)
    private String id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    @NonNull
    @Column(unique = true)
    private String email;

    private String firstName;

    private String lastName;

    private String imageUrl;

    @JsonIgnore
    private String password;

    @NotNull
    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @CreationTimestamp
    private OffsetDateTime updatedAt;

    @NotNull
    @CreationTimestamp
    private OffsetDateTime lastLogin;

    public String getUserName() {
        return firstName + " " + lastName;
    }
}