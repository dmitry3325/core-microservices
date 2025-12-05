package com.corems.communicationms.app.entity;

import com.corems.communicationms.app.model.MessageSenderType;
import com.corems.communicationms.app.model.MessageStatus;
import com.corems.communicationms.app.model.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "message")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class MessageEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", insertable = false, updatable = false)
    private MessageType type;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    @Column(nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column
    private MessageSenderType sentByType;

    @Column(length = 36)
    private UUID sentById;

    public MessageEntity() {
        this.createdAt = Instant.now();
        this.status = MessageStatus.created;
    }
}