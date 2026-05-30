package esq.phonemod.phone.api;

/**
 * Constants for all UI asset paths used by the phone framework.
 *
 * <p>
 * Paths are relative to the game's asset root, as required by
 * {@code UICommandBuilder.append()} and {@code UICommandBuilder.clear()}.
 *
 * <p>
 * Image paths inside {@code .ui} files are resolved relative to the
 * {@code .ui} file's own location in the asset namespace.
 */
public final class PhoneAssetPaths {

    private PhoneAssetPaths() {
    }

    // ── Phone frame ───────────────────────────────────────────────────────────

    /** The outer phone shell UI (background, top bar, bottom bar). */
    public static final String PHONE_UI = "Pages/Phone/Phone.ui";

    // ── App button (home-grid icon) fragments ─────────────────────────────────

    public static final String WHATGRAM_BUTTON_UI = "Pages/Phone/Components/WhatgramButton.ui";
    public static final String CONTACTS_BUTTON_UI = "Pages/Phone/Components/ContactsButton.ui";
    public static final String CALLS_BUTTON_UI = "Pages/Phone/Components/CallsButton.ui";
    public static final String SETTINGS_BUTTON_UI = "Pages/Phone/Components/SettingsButton.ui";

    // ── App UI roots (loaded into #AppContent) ────────────────────────────────
    // Built on the Dust component library (DustLib.ui) and themed at runtime by
    // ThemeService. The per-app render paths for appended fragments live in the
    // app helper classes (Whatgram / Contacts / Calls).

    public static final String DUST_WHATGRAM_UI = "Pages/Phone/DustWhatgram.ui";
    public static final String DUST_CONTACTS_UI = "Pages/Phone/DustContacts.ui";
    public static final String DUST_CALLS_UI = "Pages/Phone/DustCalls.ui";
    public static final String DUST_SETTINGS_UI = "Pages/Phone/DustSettings.ui";
    public static final String DUST_APPSTORE_UI = "Pages/Phone/DustAppStore.ui";

    public static final String APPSTORE_BUTTON_UI = "Pages/Phone/Components/AppStoreButton.ui";

}
