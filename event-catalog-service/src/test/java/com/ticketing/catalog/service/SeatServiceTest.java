package com.ticketing.catalog.service;

import com.ticketing.catalog.application.service.SeatService;
import com.ticketing.catalog.domain.model.EventZone;
import com.ticketing.catalog.domain.model.Seat;
import com.ticketing.catalog.domain.model.enums.SeatStatus;
import com.ticketing.catalog.domain.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock SeatRepository seatRepository;
    @InjectMocks SeatService seatService;

    @Test
    void shouldGenerateCorrectSeatCount() {
        // 2 zones: VIP(2 rows × 3 seats) + FLOOR(1 row × 5 seats) = 11 seats
        var zones = List.of(
            new EventZone("VIP", 2, 3, BigDecimal.valueOf(500000), "A"),
            new EventZone("FLOOR", 1, 5, BigDecimal.valueOf(200000), "C")
        );

        when(seatRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Seat> seats = inv.getArgument(0);
            return Flux.fromIterable(seats);
        });

        StepVerifier.create(seatService.generateSeats("event1", zones).count())
            .expectNext(11L)
            .verifyComplete();
    }

    @Test
    void shouldGenerateCorrectSeatCodes() {
        var zones = List.of(
            new EventZone("VIP", 1, 3, BigDecimal.valueOf(500000), "A")
        );

        when(seatRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Seat> seats = inv.getArgument(0);
            return Flux.fromIterable(seats);
        });

        StepVerifier.create(seatService.generateSeats("event1", zones).collectList())
            .assertNext(seats -> {
                assertThat(seats).hasSize(3);
                assertThat(seats.get(0).getCode()).isEqualTo("A1");
                assertThat(seats.get(1).getCode()).isEqualTo("A2");
                assertThat(seats.get(2).getCode()).isEqualTo("A3");
                assertThat(seats.get(0).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
                assertThat(seats.get(0).getZone()).isEqualTo("VIP");
                assertThat(seats.get(0).getRow()).isEqualTo("A");
            })
            .verifyComplete();
    }

    @Test
    void shouldUpdateSeatStatusToLocked() {
        var seat = Seat.builder().id("s1").status(SeatStatus.AVAILABLE).build();
        var updated = Seat.builder().id("s1").status(SeatStatus.LOCKED).build();

        when(seatRepository.findById("s1")).thenReturn(Mono.just(seat));
        when(seatRepository.save(any(Seat.class))).thenReturn(Mono.just(updated));

        StepVerifier.create(seatService.updateStatus("s1", SeatStatus.LOCKED))
            .expectNextMatches(s -> s.getStatus() == SeatStatus.LOCKED)
            .verifyComplete();
    }
}
