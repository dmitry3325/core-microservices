package com.corems.communicationms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity(name = "email")
@DiscriminatorValue("email")
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