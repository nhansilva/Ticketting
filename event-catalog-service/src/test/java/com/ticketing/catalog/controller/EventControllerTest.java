package com.ticketing.catalog.controller;

import com.ticketing.catalog.api.controller.EventController;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.catalog.application.service.EventService;
import com.ticketing.catalog.common.exception.EventNotFoundException;
import com.ticketing.catalog.common.exception.InvalidStatusTransitionException;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;
import com.ticketing.common.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(
    controllers = EventController.class,
    excludeAutoConfiguration = {
        ReactiveSecurityAutoConfiguration.class,
        ReactiveUserDetailsServiceAutoConfiguration.class
    }
)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    EventService eventService;

    private EventResponse sampleResponse() {
        return new EventResponse(
            "e1", "Concert A", "Desc", EventType.CONCERT,
            EventStatus.DRAFT, "v1", "Nhà hát Lớn",
            LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(3),
            List.of(), List.of(), null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void shouldReturn201WhenEventCreated() {
        when(eventService.createEvent(any(), eq("user1"))).thenReturn(Mono.just(sampleResponse()));

        webTestClient.post().uri("/api/v1/events")
            .header("X-User-Role", "ADMIN")
            .header("X-User-Id", "user1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "title": "Concert A",
                  "description": "Desc",
                  "type": "CONCERT",
                  "venueId": "v1",
                  "startTime": "2026-05-01T19:00:00",
                  "endTime": "2026-05-01T22:00:00",
                  "zones": [{"name":"VIP","rows":5,"seatsPerRow":10,"price":500000,"rowPrefix":"A"}],
                  "detail": {"type":"CONCERT","artists":["Artist A"],"genres":["Pop"],"ageRestriction":0}
                }
                """)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.id").isEqualTo("e1");
    }

    @Test
    void shouldReturn403WhenNotAdmin() {
        webTestClient.delete().uri("/api/v1/events/e1")
            .header("X-User-Role", "CUSTOMER")
            .exchange()
            .expectStatus().isForbidden()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.errorCode").isEqualTo("FORBIDDEN");
    }

    @Test
    void shouldReturn404WhenEventNotFound() {
        when(eventService.findById("not-exist"))
            .thenReturn(Mono.error(new EventNotFoundException("not-exist")));

        webTestClient.get().uri("/api/v1/events/not-exist")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }
}
