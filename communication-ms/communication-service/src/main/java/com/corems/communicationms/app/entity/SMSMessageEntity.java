package com.corems.communicationms.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "sms")
@DiscriminatorValue("sms")
@Getter
@Setter
public class SMSMessageEntity extends MessageEntity {

    public SMSMessageEntity() {
        this.setType(MessageType.SMS);
    }

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String message;

    @Column
    private String sid;

}
