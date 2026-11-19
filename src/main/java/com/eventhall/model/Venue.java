package com.eventhall.model;

import java.math.BigDecimal;

// Abstraction and inheritance requirements: concrete venue types define their rent policy.
public abstract class Venue {
    private long id;
    private String name;
    private int capacity;
    private BigDecimal baseRent;
    private String facilities;
    private String description;
    private String imageUrl;
    private VenueStatus status;

    protected Venue(long id, String name, int capacity, BigDecimal baseRent,
                    String facilities, VenueStatus status) {
        this(id, name, capacity, baseRent, facilities, "", "", status);
    }

    protected Venue(long id, String name, int capacity, BigDecimal baseRent,
                    String facilities, String description, String imageUrl, VenueStatus status) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.baseRent = baseRent;
        this.facilities = facilities;
        this.description = description == null ? "" : description;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.status = status;
    }

    public abstract BigDecimal calculateRent(int guests);
    public abstract String getType();

    protected void validateGuestCount(int guests) {
        if (guests <= 0 || guests > capacity) {
            throw new IllegalArgumentException("Guests must be between 1 and venue capacity.");
        }
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public BigDecimal getBaseRent() { return baseRent; }
    public void setBaseRent(BigDecimal baseRent) { this.baseRent = baseRent; }
    public String getFacilities() { return facilities; }
    public void setFacilities(String facilities) { this.facilities = facilities; }
    public String getDescription() { return description == null ? "" : description; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public String getImageUrl() { return imageUrl == null ? "" : imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl == null ? "" : imageUrl; }
    public VenueStatus getStatus() { return status; }
    public void setStatus(VenueStatus status) { this.status = status; }
}