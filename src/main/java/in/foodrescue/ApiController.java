package in.foodrescue;

import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api")
class ApiController {
    private final ListingRepository listings;
    private final RequestRepository requests;

    ApiController(ListingRepository listings, RequestRepository requests) {
        this.listings = listings; this.requests = requests;
    }

    @GetMapping("/listings")
    List<FoodListing> allListings() { return listings.findAllByOrderByCreatedAtDesc(); }

    @PostMapping("/listings")
    ResponseEntity<FoodListing> createListing(@RequestBody FoodListing item) {
        if (blank(item.title) || blank(item.location) || item.quantity == null || item.quantity <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add a food name, positive quantity and pickup location.");
        item.id = null; item.status = "AVAILABLE";
        return ResponseEntity.status(HttpStatus.CREATED).body(listings.save(item));
    }

    @DeleteMapping("/listings/{id}")
    ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        if (!listings.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        listings.deleteById(id); return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests")
    List<FoodRequest> allRequests() { return requests.findAllByOrderByCreatedAtDesc(); }

    @PostMapping("/requests")
    @Transactional
    ResponseEntity<FoodRequest> createRequest(@RequestBody FoodRequest request) {
        if (request == null || request.listingId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a food listing.");
        FoodListing listing = listings.findByIdForUpdate(request.listingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food listing not found."));
        if (!"AVAILABLE".equals(listing.status))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This food listing is no longer available.");
        if (blank(request.recipientName) || blank(request.organisation))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter recipient name and organisation.");
        request.id = null; request.status = "PENDING";
        FoodRequest saved = requests.save(request);
        listing.status = "REQUESTED"; listings.save(listing);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/requests/{id}/status")
    @Transactional
    FoodRequest updateRequest(@PathVariable Long id, @RequestBody Map<String,String> body) {
        FoodRequest req = requests.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String status = body == null ? "" : body.getOrDefault("status", "");
        if (!List.of("PENDING", "APPROVED", "COLLECTED", "REJECTED").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request status.");
        if (!status.equals(req.status) && !validTransition(req.status, status))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request cannot move to that status.");
        req.status = status;
        FoodRequest saved = requests.save(req);
        FoodListing listing = listings.findByIdForUpdate(req.listingId).orElse(null);
        if (listing != null) {
            List<FoodRequest> listingRequests = requests.findByListingIdOrderByCreatedAtDesc(req.listingId);
            if (listingRequests.stream().anyMatch(item -> "COLLECTED".equals(item.status)))
                listing.status = "COLLECTED";
            else if (listingRequests.stream().anyMatch(item -> "APPROVED".equals(item.status)))
                listing.status = "APPROVED";
            else if (listingRequests.stream().anyMatch(item -> "PENDING".equals(item.status)))
                listing.status = "REQUESTED";
            else listing.status = "AVAILABLE";
            listings.save(listing);
        }
        return saved;
    }

    private boolean validTransition(String current, String next) {
        return ("PENDING".equals(current) && List.of("APPROVED", "REJECTED").contains(next))
            || ("APPROVED".equals(current) && List.of("COLLECTED", "REJECTED").contains(next));
    }

    @GetMapping("/dashboard")
    Map<String,Object> dashboard() {
        List<FoodListing> all = listings.findAll();
        List<FoodRequest> allReq = requests.findAll();
        double rescued = all.stream().filter(x -> "COLLECTED".equals(x.status))
            .mapToDouble(x -> x.quantity == null ? 0 : x.quantity).sum();
        return Map.of("totalListings", all.size(),
            "availableListings", all.stream().filter(x -> "AVAILABLE".equals(x.status)).count(),
            "totalRequests", allReq.size(),
            "successfulPickups", allReq.stream().filter(x -> "COLLECTED".equals(x.status)).count(),
            "quantityRescued", rescued);
    }

    private boolean blank(String s) { return s == null || s.trim().isEmpty(); }
}
