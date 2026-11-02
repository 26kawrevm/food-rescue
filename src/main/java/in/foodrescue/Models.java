package in.foodrescue;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
class FoodListing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String title;
    public String category;
    public Double quantity;
    public String unit;
    public String location;
    public String pickupDeadline;
    @Column(length = 2000) public String storageDetails;
    @Column(length = 1000) public String allergens;
    public String expiryInfo;
    public String providerName;
    public boolean safetyChecked;
    public String status = "AVAILABLE";
    public LocalDateTime createdAt = LocalDateTime.now();
    public FoodListing() {}
}

@Entity
class FoodRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long listingId;
    public String recipientName;
    public String organisation;
    @Column(length = 1000) public String message;
    public String status = "PENDING";
    public LocalDateTime createdAt = LocalDateTime.now();
    public FoodRequest() {}
}
