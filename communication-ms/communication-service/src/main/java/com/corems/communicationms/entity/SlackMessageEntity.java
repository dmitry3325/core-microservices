package com.corems.communicationms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "slack")
@DiscriminatorValue("slack")
@Getter
@Setter
public class SlackMessageEntity extends MessageEntity {
    @Column(nullable = false)
    private String channel;
    @Column(nullable = false)
    private String message;

    public SlackMessageEntity() {
        this.setType(MessageType.SLACK);
    }

}
