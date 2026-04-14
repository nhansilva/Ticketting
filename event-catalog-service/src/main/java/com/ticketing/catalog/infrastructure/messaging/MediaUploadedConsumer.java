package com.ticketing.catalog.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MediaUploadedConsumer {

    // TODO: inject EventRepository và update event.imageUrls khi media-service được build
    // Payload sẽ có: { mediaId, url, eventId, size: "thumbnail|card|banner" }

    @KafkaListener(topics = "media.uploaded", groupId = "event-catalog-service")
    public void onMediaUploaded(Map<String, String> payload) {
        log.info("media.uploaded received — eventId={}, url={}, size={}",
            payload.get("eventId"), payload.get("url"), payload.get("size"));
        // TODO: eventRepository.findById(payload.get("eventId"))
        //     .flatMap(event -> { event.getImageUrls().add(url); return save; })
        //     .subscribe();
    }
}
