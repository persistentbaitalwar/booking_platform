package com.persistent.booking_platform.kafka;

public class EventPublishException extends RuntimeException {
    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
