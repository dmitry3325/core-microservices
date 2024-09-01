package com.corems.notificationms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "slack")
@DiscriminatorValue("slack")
@Getter
@Setter
public class SlackNotificationEntity extends NotificationEntity {
    @Column(nullable = false)
    private String channel;
    @Column(nullable = false)
    private String message;

    public SlackNotificationEntity() {
        this.setType(NotificationType.SLACK);
    }


}
