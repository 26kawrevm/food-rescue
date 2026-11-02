package in.foodrescue;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

interface ListingRepository extends JpaRepository<FoodListing, Long> {
    List<FoodListing> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select listing from FoodListing listing where listing.id = :id")
    Optional<FoodListing> findByIdForUpdate(@Param("id") Long id);
}
interface RequestRepository extends JpaRepository<FoodRequest, Long> {
    List<FoodRequest> findAllByOrderByCreatedAtDesc();
    List<FoodRequest> findByListingIdOrderByCreatedAtDesc(Long listingId);
}
