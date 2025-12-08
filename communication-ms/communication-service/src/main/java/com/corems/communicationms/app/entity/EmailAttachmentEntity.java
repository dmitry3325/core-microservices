package com.corems.communicationms.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_attachment")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EmailAttachmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "uuid")
    private EmailMessageEntity emailMessage;

    @Column(nullable = false, length = 36)
    private UUID documentUuid;

    private String checksum;

    @CreationTimestamp
    private Instant createdAt;
}

