package com.eventhall.model;

import java.math.BigDecimal;

public record PaymentState(BigDecimal totalAmount, BookingStatus status) { }