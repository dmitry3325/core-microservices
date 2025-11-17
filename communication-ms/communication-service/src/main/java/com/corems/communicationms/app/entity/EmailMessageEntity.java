package com.corems.communicationms.app.entity;

import com.corems.communicationms.app.model.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity(name = "email")
@DiscriminatorValue("email")
@Getter
@Setter
public class EmailMessageEntity extends MessageEntity {

    public EmailMessageEntity() {
        this.setType(MessageType.email);
        this.setCreatedAt(Instant.now());
    }

    @Column(nullable = false)
    private String emailType;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String sender;

    @Column
    private String senderName;

    @Column
    private String cc;

    @Column
    private String bcc;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String body;
}