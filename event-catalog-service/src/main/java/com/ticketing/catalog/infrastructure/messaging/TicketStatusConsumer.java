package com.ticketing.catalog.infrastructure.messaging;

import com.ticketing.catalog.application.service.SeatService;
import com.ticketing.catalog.domain.model.enums.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketStatusConsumer {

    private final SeatService seatService;

    @KafkaListener(topics = "ticket.reserved", groupId = "event-catalog-service")
    public void onTicketReserved(Map<String, String> payload) {
        String seatId = payload.get("seatId");
        log.info("ticket.reserved received — seatId={}", seatId);
        seatService.updateStatus(seatId, SeatStatus.LOCKED)
            .doOnError(e -> log.error("Failed to lock seat {}: {}", seatId, e.getMessage()))
            .subscribe();
    }

    @KafkaListener(topics = "ticket.released", groupId = "event-catalog-service")
    public void onTicketReleased(Map<String, String> payload) {
        String seatId = payload.get("seatId");
        log.info("ticket.released received — seatId={}", seatId);
        seatService.updateStatus(seatId, SeatStatus.AVAILABLE)
            .doOnError(e -> log.error("Failed to release seat {}: {}", seatId, e.getMessage()))
            .subscribe();
    }

    @KafkaListener(topics = "ticket.issued", groupId = "event-catalog-service")
    public void onTicketIssued(Map<String, String> payload) {
        String seatId = payload.get("seatId");
        log.info("ticket.issued received — seatId={}", seatId);
        seatService.updateStatus(seatId, SeatStatus.SOLD)
            .doOnError(e -> log.error("Failed to mark seat sold {}: {}", seatId, e.getMessage()))
            .subscribe();
    }
}
