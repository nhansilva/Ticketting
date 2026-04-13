package com.ticketing.catalog.application.service;

import com.ticketing.catalog.api.dto.response.SeatResponse;
import com.ticketing.catalog.domain.model.EventZone;
import com.ticketing.catalog.domain.model.Seat;
import com.ticketing.catalog.domain.model.enums.SeatStatus;
import com.ticketing.catalog.domain.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;

    public Flux<Seat> generateSeats(String eventId, List<EventZone> zones) {
        List<Seat> seats = new ArrayList<>();
        for (EventZone zone : zones) {
            for (int r = 0; r < zone.rows(); r++) {
                String rowLabel = String.valueOf((char) (zone.rowPrefix().charAt(0) + r));
                for (int s = 1; s <= zone.seatsPerRow(); s++) {
                    seats.add(Seat.builder()
                        .eventId(eventId)
                        .zone(zone.name())
                        .row(rowLabel)
                        .seatNumber(s)
                        .code(rowLabel + s)
                        .price(zone.price())
                        .status(SeatStatus.AVAILABLE)
                        .build());
                }
            }
        }
        return seatRepository.saveAll(seats);
    }

    public Mono<Map<String, List<SeatResponse>>> findByEvent(String eventId, String zone) {
        Flux<Seat> seats = (zone != null && !zone.isBlank())
            ? seatRepository.findByEventIdAndZone(eventId, zone)
            : seatRepository.findByEventId(eventId);

        return seats
            .map(s -> new SeatResponse(s.getId(), s.getZone(), s.getRow(),
                s.getSeatNumber(), s.getCode(), s.getPrice(), s.getStatus()))
            .collectMultimap(SeatResponse::row)
            .map(m -> m.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> new ArrayList<>(e.getValue())
                )));
    }

    public Mono<Seat> updateStatus(String seatId, SeatStatus status) {
        return seatRepository.findById(seatId)
            .flatMap(seat -> {
                seat.setStatus(status);
                return seatRepository.save(seat);
            });
    }

    public Mono<Void> deleteByEvent(String eventId) {
        return seatRepository.deleteByEventId(eventId);
    }
}
