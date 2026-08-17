package esq.phonemod.api;

import javax.annotation.Nonnull;

public interface AppRegistration extends AutoCloseable {
    @Nonnull String appId();
    boolean active();
    @Override void close();
}
