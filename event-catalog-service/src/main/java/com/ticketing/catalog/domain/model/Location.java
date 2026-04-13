package com.ticketing.catalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("locations")
public class Location {
    @Id
    private String id;
    private String type;        // "PROVINCE" | "DISTRICT"
    private String code;
    private String name;
    private String parentCode;
}
