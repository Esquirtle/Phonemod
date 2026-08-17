package esq.phonemod.migration;

import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import esq.phonemod.device.components.DeviceSettingsComponent;
import esq.phonemod.phone.appstore.DeviceInstalledAppsComponent;
import esq.phonemod.phone.components.CallHistoryComponent;
import esq.phonemod.phone.components.CallRecord;
import esq.phonemod.phone.components.ConversationHistoryComponent;
import esq.phonemod.phone.components.PhoneAppSessionState;
import esq.phonemod.phone.components.PhoneOwnerComponent;
import esq.phonemod.phone.messaging.ChatMessage;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MigrationComponentTest {
    @Test void installedAppsRemapIsIdempotentAndNeverOverwritesDestination() {
        DeviceInstalledAppsComponent component = new DeviceInstalledAppsComponent();
        component.install("old", "legacy-app");
        component.install("new", "existing-app");
        component.remapDevice("old", "new");
        component.remapDevice("old", "new");
        assertEquals(java.util.Set.of("existing-app"), component.getInstalled("new"));
        assertTrue(component.getInstalled("old").isEmpty());
    }

    @Test void settingsAndSessionStateRemapTwiceToSameData() {
        DeviceSettingsComponent settings = new DeviceSettingsComponent();
        var oldSettings = settings.getOrCreate("old");
        settings.remapDevice("old", "new");
        settings.remapDevice("old", "new");
        assertSame(oldSettings, settings.get("new"));

        PhoneAppSessionState state = new PhoneAppSessionState();
        state.set("old|legacy", "view", "detail");
        state.remapDevice("old", "new");
        state.remapDevice("old", "new");
        assertEquals("detail", state.get("new|legacy", "view"));
        assertNull(state.get("old|legacy", "view"));
    }

    @Test void contactsRemapIsIdempotentAndNeverOverwritesAnotherOwnersContacts() {
        // PhoneOwnerComponent#addContact enforces PhonemodConfig limits through the live
        // PhoneMod plugin instance, which isn't available in a unit test — seed the mutable
        // map directly (as DeviceSettingsComponent#getOrCreate-style tests already do).
        PhoneOwnerComponent component = new PhoneOwnerComponent();
        component.getContacts("old").put("555-1000", "Duplicate Claimant's Contact");
        component.getContacts("new").put("555-2000", "Existing Owner's Contact");
        component.remapDevice("old", "new");
        component.remapDevice("old", "new");
        assertEquals(java.util.Map.of("555-2000", "Existing Owner's Contact"), component.getContacts("new"));
        assertTrue(component.getContacts("old").isEmpty());
    }

    @Test void conversationHistoryRemapIsIdempotentAndNeverOverwritesAnotherOwnersMessages() {
        // ConversationHistoryComponent#addMessage also depends on the live PhoneMod config
        // for retention trimming, so seed data through the component's own CODEC instead —
        // exercising the exact on-disk schema the real migration mutates.
        ArrayCodec<ChatMessage> messages = new ArrayCodec<>(ChatMessage.CODEC, ChatMessage[]::new);
        BsonDocument history = new BsonDocument();
        history.put("old", new BsonDocument("555-1000",
                messages.encode(new ChatMessage[]{new ChatMessage(true, "Me", "hi from duplicate claimant")})));
        history.put("new", new BsonDocument("555-2000",
                messages.encode(new ChatMessage[]{new ChatMessage(true, "Me", "existing owner's message")})));
        BsonDocument root = new BsonDocument("History", history);

        ConversationHistoryComponent component = ConversationHistoryComponent.CODEC.decode(root);
        component.remapDevice("old", "new");
        component.remapDevice("old", "new");
        assertEquals(1, component.getMessages("new", "555-2000").size());
        assertEquals("existing owner's message", component.getMessages("new", "555-2000").get(0).body());
        assertTrue(component.getMessages("new", "555-1000").isEmpty());
        assertTrue(component.getMessages("old", "555-1000").isEmpty());
    }

    @Test void callHistoryRemapIsIdempotentAndNeverOverwritesAnotherOwnersRecords() {
        // CallHistoryComponent#addRecord also needs the live PhoneMod config; seed through
        // the component's own CODEC as above.
        ArrayCodec<CallRecord> records = new ArrayCodec<>(CallRecord.CODEC, CallRecord[]::new);
        BsonDocument history = new BsonDocument();
        history.put("old", records.encode(new CallRecord[]{
                new CallRecord("555-1000", "Duplicate Claimant Call", 1L, true, false)}));
        history.put("new", records.encode(new CallRecord[]{
                new CallRecord("555-2000", "Existing Owner Call", 2L, true, false)}));
        BsonDocument root = new BsonDocument("History", history);

        CallHistoryComponent component = CallHistoryComponent.CODEC.decode(root);
        component.remapDevice("old", "new");
        component.remapDevice("old", "new");
        assertEquals(1, component.getHistory("new").size());
        assertEquals("Existing Owner Call", component.getHistory("new").get(0).displayName);
        assertTrue(component.getHistory("old").isEmpty());
    }
}
