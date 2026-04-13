package com.ticketing.catalog.api.dto.response;

import com.ticketing.catalog.domain.model.enums.SeatStatus;

import java.math.BigDecimal;

public record SeatResponse(
    String id,
    String zone,
    String row,
    int seatNumber,
    String code,
    BigDecimal price,
    SeatStatus status
) {}
