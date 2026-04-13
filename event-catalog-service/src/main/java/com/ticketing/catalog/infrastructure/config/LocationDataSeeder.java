package com.ticketing.catalog.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.catalog.domain.model.Location;
import com.ticketing.catalog.domain.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationDataSeeder {

    private final LocationRepository locationRepository;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        locationRepository.count()
            .flatMap(count -> {
                if (count > 0) {
                    log.info("Locations already seeded ({} records), skipping", count);
                    return Mono.empty();
                }
                return seedFromFile();
            })
            .subscribe(
                null,
                e -> log.error("Failed to seed locations: {}", e.getMessage())
            );
    }

    private Mono<Void> seedFromFile() {
        try {
            var resource = new ClassPathResource("data/locations.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());

            List<Location> locations = new ArrayList<>();

            root.get("provinces").forEach(node -> locations.add(
                Location.builder()
                    .type("PROVINCE")
                    .code(node.get("code").asText())
                    .name(node.get("name").asText())
                    .build()
            ));

            root.get("districts").forEach(node -> locations.add(
                Location.builder()
                    .type("DISTRICT")
                    .code(node.get("code").asText())
                    .name(node.get("name").asText())
                    .parentCode(node.get("parentCode").asText())
                    .build()
            ));

            return locationRepository.saveAll(locations)
                .then()
                .doOnSuccess(v -> log.info("Seeded {} locations", locations.size()));

        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to read locations.json", e));
        }
    }
}
