package esq.phonemod.api;

import javax.annotation.Nonnull;

public interface PresenceService { boolean online(@Nonnull DeviceIdentity identity); }
