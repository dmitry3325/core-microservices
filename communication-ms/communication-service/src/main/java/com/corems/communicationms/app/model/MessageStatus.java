package com.corems.communicationms.app.model;

import lombok.Getter;

@Getter
public enum MessageStatus {
    CREATED,
    ENQUEUED,
    SENT,
    FAILED
}
