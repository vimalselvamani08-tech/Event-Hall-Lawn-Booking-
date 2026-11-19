package com.eventhall.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MarriageHall extends Venue {
    public MarriageHall(long id, String name, int capacity, BigDecimal baseRent,
                       String facilities, VenueStatus status) {
        super(id, name, capacity, baseRent, facilities, status);
    }

    public MarriageHall(long id, String name, int capacity, BigDecimal baseRent,
                       String facilities, String description, String imageUrl, VenueStatus status) {
        super(id, name, capacity, baseRent, facilities, description, imageUrl, status);
    }

    @Override
    public BigDecimal calculateRent(int guests) {
        validateGuestCount(guests);
        BigDecimal surcharge = BigDecimal.valueOf(Math.max(0, guests - 200) * 2L);
        return getBaseRent().add(surcharge).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getType() { return "MARRIAGE_HALL"; }
}