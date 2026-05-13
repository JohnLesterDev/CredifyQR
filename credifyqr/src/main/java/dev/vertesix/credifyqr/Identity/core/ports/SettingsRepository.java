package dev.vertesix.credifyqr.Identity.core.ports;
import java.util.Optional;

/**
 * Repository port for retrieving and saving application settings.
 */
public interface SettingsRepository {
    /**
     * Retrieves a configuration value by key.
     */
    Optional<String> getSetting(String key);
    /**
     * Persists a configuration key/value pair.
     */
    void saveSetting(String key, String value);
}