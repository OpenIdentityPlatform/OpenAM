/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.federation.services.util;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import org.testng.annotations.Test;

/**
 * Covers the signature policy predicates that guard the unauthenticated ID-FF
 * entry points ({@code /SOAPReceiver}, {@code /ProcessTermination},
 * {@code /ProcessLogout}, {@code /ProcessRegistration}).
 */
public class FSServiceUtilsTest {

    private static final String PROP_REQUIRE_SIGNATURE =
        "com.sun.identity.federation.services.requireSignature";

    private static final String PROP_SIGNING_ON =
        "com.sun.identity.federation.services.signingOn";

    /**
     * The predicate must default to "on" and must only be switched off by an
     * explicit {@code false}, including when the configured value carries
     * stray whitespace: {@code SystemPropertiesManager.get(key, default)}
     * returns the raw value, so an untrimmed {@code "true "} used to parse as
     * {@code false} and silently disabled enforcement.
     *
     * <p>Asserted through {@code isSignatureVerificationRequired()} because the
     * underlying predicate is deliberately private; with signing off the two
     * coincide, which is the stock configuration.
     */
    @Test
    public void onlyAnExplicitFalseDisablesTheSignatureRequirement() {
        // Pin the other half of the predicate so this test does not depend on
        // the order it runs in relative to the signing-mode test below.
        set(PROP_SIGNING_ON, "false");

        // Unset -> required. Asserted first: the properties store can be
        // overridden but not cleared.
        assertTrue(FSServiceUtils.isSignatureVerificationRequired(),
            "must default to requiring a signature when unset");

        for (String blank : new String[] {"", "   "}) {
            set(PROP_REQUIRE_SIGNATURE, blank);
            assertTrue(FSServiceUtils.isSignatureVerificationRequired(),
                "a blank value must fall back to the secure default");
        }

        for (String on : new String[] {"true", "true ", " true", "TRUE"}) {
            set(PROP_REQUIRE_SIGNATURE, on);
            assertTrue(FSServiceUtils.isSignatureVerificationRequired(),
                "value [" + on + "] must keep the signature requirement on");
        }

        for (String garbage : new String[] {"yes", "0", "disabled"}) {
            set(PROP_REQUIRE_SIGNATURE, garbage);
            assertTrue(FSServiceUtils.isSignatureVerificationRequired(),
                "malformed value [" + garbage + "] must fail closed");
        }

        for (String off : new String[] {"false", "false ", " false", "FALSE"}) {
            set(PROP_REQUIRE_SIGNATURE, off);
            assertFalse(FSServiceUtils.isSignatureVerificationRequired(),
                "value [" + off + "] must switch the requirement off");
        }

        set(PROP_REQUIRE_SIGNATURE, "true");
    }

    /**
     * The three-valued signing mode is read on every call, so a change to
     * {@code XMLSigningOn} takes effect without restarting the server. The two
     * predicates must stay mutually exclusive, and a blank value must fall back
     * to the {@code optional} default rather than reading as "signing off".
     */
    @Test
    public void signingModeIsReadOnEveryCall() {
        set(PROP_SIGNING_ON, "true");
        assertTrue(FSServiceUtils.isSigningOn(), "true -> signing on");
        assertFalse(FSServiceUtils.isSigningOptional(), "true -> not optional");

        // Same predicate, new value, no restart in between.
        set(PROP_SIGNING_ON, "optional");
        assertFalse(FSServiceUtils.isSigningOn(), "optional -> not signing on");
        assertTrue(FSServiceUtils.isSigningOptional(), "optional -> optional");

        set(PROP_SIGNING_ON, "false");
        assertFalse(FSServiceUtils.isSigningOn(), "false -> not signing on");
        assertFalse(FSServiceUtils.isSigningOptional(), "false -> not optional");

        for (String padded : new String[] {"true ", " true", "TRUE"}) {
            set(PROP_SIGNING_ON, padded);
            assertTrue(FSServiceUtils.isSigningOn(),
                "value [" + padded + "] must still enable signing");
        }

        for (String blank : new String[] {"", "   "}) {
            set(PROP_SIGNING_ON, blank);
            assertTrue(FSServiceUtils.isSigningOptional(),
                "a blank value must fall back to optional");
            assertFalse(FSServiceUtils.isSigningOn(),
                "a blank value must not enable signing");
        }

        set(PROP_SIGNING_ON, "false");
    }

    private static void set(String key, String value) {
        SystemPropertiesManager.initializeProperties(key, value);
    }
}
