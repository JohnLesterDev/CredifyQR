package dev.vertesix.credifyqr.Identity.core.ports;
import java.util.Optional;

public interface SettingsRepository {
    Optional<String> getSetting(String key);
    void saveSetting(String key, String value);
}