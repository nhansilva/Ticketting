package com.ticketing.catalog.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    @PostConstruct
    public void createIndexes() {
        // seats: { eventId, zone, status }
        mongoTemplate.indexOps("seats")
            .ensureIndex(new CompoundIndexDefinition(
                new Document("eventId", 1).append("zone", 1).append("status", 1)))
            .subscribe(name -> log.info("Index created: {}", name));

        // seats: { eventId, row, seatNumber } — unique
        mongoTemplate.indexOps("seats")
            .ensureIndex(new CompoundIndexDefinition(
                new Document("eventId", 1).append("row", 1).append("seatNumber", 1))
                .unique())
            .subscribe(name -> log.info("Unique index created: {}", name));

        // events: { status }
        mongoTemplate.indexOps("events")
            .ensureIndex(new Index().on("status", Sort.Direction.ASC))
            .subscribe(name -> log.info("Index created: {}", name));

        // locations: { type }, { parentCode }
        mongoTemplate.indexOps("locations")
            .ensureIndex(new Index().on("type", Sort.Direction.ASC))
            .subscribe();
        mongoTemplate.indexOps("locations")
            .ensureIndex(new Index().on("parentCode", Sort.Direction.ASC))
            .subscribe();
    }
}
