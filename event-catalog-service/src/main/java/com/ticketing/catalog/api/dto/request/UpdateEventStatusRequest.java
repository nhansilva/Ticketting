package com.ticketing.catalog.api.dto.request;

import com.ticketing.common.dto.enums.EventStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEventStatusRequest(@NotNull EventStatus status) {}
