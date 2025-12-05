package com.corems.communicationms.app.entity;

import com.corems.communicationms.app.model.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity(name = "sms")
@DiscriminatorValue("sms")
@Getter
@Setter
public class SMSMessageEntity extends MessageEntity {

    public SMSMessageEntity() {
        this.setType(MessageType.sms);
        this.setCreatedAt(Instant.now());
    }

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String message;

    private String sid;
}