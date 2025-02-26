package com.corems.communicationms.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity(name = "sms")
@DiscriminatorValue("sms")
public class SMSMessageEntity extends MessageEntity {

}
