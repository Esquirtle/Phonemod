package esq.phonemod.api;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** ECS handles valid only inside a context-scheduled world-thread callback. */
public record WorldAccess(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {}
