package com.ticketing.catalog.domain.model;

import com.ticketing.catalog.domain.model.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("seats")
public class Seat {
    @Id
    private String id;
    private String eventId;
    private String zone;
    private String row;
    private int seatNumber;
    private String code;
    private BigDecimal price;
    private SeatStatus status;
}
