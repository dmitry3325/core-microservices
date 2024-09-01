package com.corems.notificationms.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity(name = "sms")
@DiscriminatorValue("sms")
public class SMSNotificationEntity extends NotificationEntity {

}
