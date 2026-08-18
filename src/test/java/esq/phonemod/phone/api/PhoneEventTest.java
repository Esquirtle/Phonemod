package esq.phonemod.phone.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhoneEventTest {

    @Test
    void slotIndexParsesPresentValue() {
        PhoneEvent event = new PhoneEvent("select_listing", "auctionhouse", Map.of("SlotIndex", "7"));
        assertEquals(7, event.slotIndex());
    }

    @Test
    void slotIndexAbsentReturnsMinusOne() {
        assertEquals(-1, new PhoneEvent("open", "app", Map.of()).slotIndex());
    }

    @Test
    void slotIndexNonNumericReturnsMinusOne() {
        assertEquals(-1, new PhoneEvent("open", "app", Map.of("SlotIndex", "x")).slotIndex());
    }

    @Test
    void customParamsSurviveAlongsideSlotIndex() {
        PhoneEvent event = new PhoneEvent("open", "app", Map.of("SlotIndex", "3", "ListingId", "abc"));
        assertEquals(3, event.slotIndex());
        assertEquals("abc", event.getParam("ListingId"));
    }
}
