package com.ticketing.catalog.api.dto.response;

public record LocationResponse(
    String code,
    String name,
    String parentCode
) {}
