package dev.vertesix.credifyqr.Identity.core.security;

import org.owasp.html.PolicyFactory;

/**
 * Utility class for sanitizing text input using a strict HTML policy.
 *
 * <p>This class is used to ensure form and request fields are cleaned before
 * being processed by the domain logic.</p>
 */
public class SanitizerUtil {
    // Blocks all HTML tags entirely. Strict plain-text only.
    private static final PolicyFactory STRICT_POLICY = new org.owasp.html.HtmlPolicyBuilder().toFactory();

    public static String clean(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        return STRICT_POLICY.sanitize(input);
    }
}