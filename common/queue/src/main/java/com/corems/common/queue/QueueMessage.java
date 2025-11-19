package com.corems.common.queue;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@Setter
@Getter
public class QueueMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String id;
    private String type;
    private Object payload;
    private int attempts = 0;
    private Map<String, String> headers;
    private Instant createdAt = Instant.now();
}

