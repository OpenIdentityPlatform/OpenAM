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
 */

package com.sun.identity.authentication.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.Principal;

import org.testng.annotations.Test;

/**
 * setPrincipal(String, String) resolves a class by name. It is not reachable in-tree today, but it
 * is public, so it is held to the same contract as AuthXMLUtils.createCustomCallback after
 * GHSA-wg5r-wc3x-39vc: a class that is not a Principal must be rejected without being initialized
 * or instantiated.
 */
public class AuthXMLRequestSecurityTest {

    @Test
    public void setPrincipalRejectsNonPrincipalClassWithoutLoadingOrInstantiatingIt() {
        ProbeFlags.initialised = false;
        ProbeFlags.instantiated = false;

        AuthXMLRequest request = new AuthXMLRequest();
        request.setPrincipal(NonPrincipalProbe.class.getName(), "ignored");

        assertThat(request.getPrincipal())
                .as("a class that is not a Principal must not become the principal").isNull();
        assertThat(ProbeFlags.instantiated)
                .as("the named class must never be instantiated").isFalse();
        assertThat(ProbeFlags.initialised)
                .as("the named class must not even be initialized: Class.forName must be called"
                        + " with initialize=false so an unvalidated name cannot run a static"
                        + " initializer").isFalse();
    }

    @Test
    public void setPrincipalStillAcceptsALegitimatePrincipal() {
        AuthXMLRequest request = new AuthXMLRequest();
        request.setPrincipal(LegitimatePrincipal.class.getName(), "ignored");

        assertThat(request.getPrincipal())
                .as("a genuine Principal implementation must still resolve")
                .isInstanceOf(LegitimatePrincipal.class);
    }

    @Test
    public void setPrincipalIgnoresAnUnknownClassName() {
        AuthXMLRequest request = new AuthXMLRequest();
        request.setPrincipal("com.example.NoSuchClassAnywhere", "ignored");

        assertThat(request.getPrincipal()).isNull();
    }

    /**
     * The flags live outside the probe on purpose. Reading or writing a static field of a class
     * triggers that class's initialization, so flags held on the probe itself would be set by the
     * test's own reset, and the "was it initialized" assertion would pass even against the
     * unhardened code. Keeping them here means the probe is only ever named by a class literal,
     * which does not trigger initialization, so the assertion can actually fail.
     */
    static final class ProbeFlags {

        static volatile boolean initialised;
        static volatile boolean instantiated;

        private ProbeFlags() {
        }
    }

    /** Not a Principal. Records, elsewhere, whether it was initialized or constructed. */
    public static class NonPrincipalProbe {

        static {
            ProbeFlags.initialised = true;
        }

        public NonPrincipalProbe() {
            ProbeFlags.instantiated = true;
        }
    }

    /** A minimal genuine Principal, to show the guard is not over-broad. */
    public static class LegitimatePrincipal implements Principal {

        @Override
        public String getName() {
            return "legitimate";
        }
    }
}
