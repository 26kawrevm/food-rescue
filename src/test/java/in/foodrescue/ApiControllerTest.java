package in.foodrescue;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:foodrescue-test;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ApiControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ListingRepository listings;
    @Autowired private RequestRepository requests;

    @BeforeEach
    void clearDatabase() {
        requests.deleteAll();
        listings.deleteAll();
    }

    @Test
    void requestLifecycleKeepsListingStatusConsistent() throws Exception {
        FoodListing listing = createListing();
        String requestBody = "{\"listingId\":" + listing.id
            + ",\"recipientName\":\"Asha\",\"organisation\":\"Community group\"}";
        MvcResult created = mockMvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status", is("PENDING")))
            .andReturn();
        Long requestId = responseId(created);
        assertListingStatus(listing.id, "REQUESTED");
        mockMvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isConflict());

        updateStatus(requestId, "APPROVED").andExpect(status().isOk());
        assertListingStatus(listing.id, "APPROVED");

        updateStatus(requestId, "PENDING").andExpect(status().isConflict());
        assertListingStatus(listing.id, "APPROVED");

        updateStatus(requestId, "REJECTED").andExpect(status().isOk());
        assertListingStatus(listing.id, "AVAILABLE");

        MvcResult nextRequest = mockMvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody.replace("Asha", "Mina")))
            .andExpect(status().isCreated())
            .andReturn();
        Long nextRequestId = responseId(nextRequest);
        assertListingStatus(listing.id, "REQUESTED");

        updateStatus(requestId, "APPROVED").andExpect(status().isConflict());
        assertListingStatus(listing.id, "REQUESTED");
        updateStatus(nextRequestId, "APPROVED").andExpect(status().isOk());
        assertListingStatus(listing.id, "APPROVED");
        updateStatus(nextRequestId, "COLLECTED").andExpect(status().isOk());
        assertListingStatus(listing.id, "COLLECTED");
        mockMvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isConflict());
    }

    @Test
    void requestWithoutListingIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientName\":\"Asha\",\"organisation\":\"Community group\"}"))
            .andExpect(status().isBadRequest());
    }

    private FoodListing createListing() {
        FoodListing listing = new FoodListing();
        listing.title = "Vegetable meals";
        listing.location = "Community kitchen";
        listing.quantity = 10.0;
        return listings.saveAndFlush(listing);
    }

    private org.springframework.test.web.servlet.ResultActions updateStatus(Long id, String nextStatus)
            throws Exception {
        return mockMvc.perform(patch("/api/requests/{id}/status", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"" + nextStatus + "\"}"));
    }

    private Long responseId(MvcResult result) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void assertListingStatus(Long id, String expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, listings.findById(id).orElseThrow().status);
    }
}
