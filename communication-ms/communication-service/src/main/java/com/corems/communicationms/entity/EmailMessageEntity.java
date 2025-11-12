package com.corems.communicationms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "email")
@DiscriminatorValue("email")
@Getter
@Setter
public class EmailMessageEntity extends MessageEntity {

    @Column(nullable = false)
    private String emailType;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String body;


}