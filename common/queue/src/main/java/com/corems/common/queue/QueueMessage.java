package com.corems.common.queue;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@Setter
@Getter
public class QueueMessage<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String type;
    private T payload;
    private Map<String, String> headers;
    private Instant createdAt;

    public QueueMessage() { this.createdAt = Instant.now(); }

}
