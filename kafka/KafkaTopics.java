package com.persistent.booking_platform.kafka;

public final class KafkaTopics {
    /** Key = booking id, so all events of one booking stay in order inside one partition. */
    public static final String TICKET_EVENTS = "ticket.events";
    public static final String TICKET_EVENTS_DLT = TICKET_EVENTS + ".DLT";

    private KafkaTopics() {
    }
}
