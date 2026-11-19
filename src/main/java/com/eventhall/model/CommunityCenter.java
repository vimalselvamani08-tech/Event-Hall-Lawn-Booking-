package com.eventhall.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CommunityCenter extends Venue {
    public CommunityCenter(long id, String name, int capacity, BigDecimal baseRent,
                           String facilities, VenueStatus status) {
        super(id, name, capacity, baseRent, facilities, status);
    }

    public CommunityCenter(long id, String name, int capacity, BigDecimal baseRent,
                           String facilities, String description, String imageUrl, VenueStatus status) {
        super(id, name, capacity, baseRent, facilities, description, imageUrl, status);
    }

    @Override
    public BigDecimal calculateRent(int guests) {
        validateGuestCount(guests);
        BigDecimal guestCharge = BigDecimal.valueOf(guests).multiply(new BigDecimal("1.50"));
        return getBaseRent().add(guestCharge).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getType() { return "COMMUNITY_CENTER"; }
}