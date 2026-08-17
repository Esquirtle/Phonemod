package esq.phonemod.security;

/** Minimal immutable view of limits needed by untrusted UI validation. */
public interface UiSafetyLimits {
    int maxUiEventBytes();
    int maxUiParameters();
    int maxUiKeyCharacters();
    int maxUiValueCharacters();
    int maxMessageCharacters();
    int maxContactNameCharacters();
}
