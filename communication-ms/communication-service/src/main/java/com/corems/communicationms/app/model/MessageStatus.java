package com.corems.communicationms.app.model;

import lombok.Getter;

@Getter
public enum MessageStatus {
    created,
    enqueued,
    sent,
    failed
}
