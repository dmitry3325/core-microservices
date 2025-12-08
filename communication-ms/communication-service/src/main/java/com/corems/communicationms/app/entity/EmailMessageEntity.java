package com.corems.communicationms.app.entity;

import com.corems.communicationms.app.model.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

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

    private String senderName;

    private String cc;

    private String bcc;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String body;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "emailMessage", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private Collection<EmailAttachmentEntity> attachments = new ArrayList<>();
}