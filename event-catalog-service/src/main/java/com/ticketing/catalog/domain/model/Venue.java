package com.ticketing.catalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("venues")
public class Venue {
    @Id
    private String id;
    private String name;
    private String provinceCode;
    private String provinceName;
    private String districtCode;
    private String districtName;
    private String streetAddress;
    private Double lat;
    private Double lng;
    private int totalCapacity;
    private List<ZoneConfig> zones;
    private LocalDateTime createdAt;
}
