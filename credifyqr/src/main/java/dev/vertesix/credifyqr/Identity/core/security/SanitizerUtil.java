package dev.vertesix.credifyqr.Identity.core.security;

import org.owasp.html.PolicyFactory;

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