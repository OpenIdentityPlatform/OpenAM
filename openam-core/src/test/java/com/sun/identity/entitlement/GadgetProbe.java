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

package com.sun.identity.entitlement;

/**
 * Test probe recording whether {@link NonEntitlementGadget}'s static initializer or no-argument
 * constructor has run. Tests reference this class (which has no side effects of its own) rather than
 * the gadget directly, so that merely reading the flags cannot itself trigger the gadget's class
 * initialization.
 * <p>
 * The flags are shared static state, so every test class that reads them resets them in a
 * {@code @BeforeMethod} and is annotated {@code @Test(singleThreaded = true)}.
 */
final class GadgetProbe {

    static volatile boolean staticInitialised = false;
    static volatile boolean constructed = false;

    private GadgetProbe() {
    }

    static void reset() {
        staticInitialised = false;
        constructed = false;
    }
}
