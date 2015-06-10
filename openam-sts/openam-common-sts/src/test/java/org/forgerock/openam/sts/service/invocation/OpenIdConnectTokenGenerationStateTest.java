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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.service.invocation;

import org.forgerock.guava.common.collect.Sets;
import org.testng.annotations.Test;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class OpenIdConnectTokenGenerationStateTest {
    private static final boolean WITH_AUTHN_CLASS_REF = true;
    private static final boolean WITH_AUTHN_METHOD_REFS = true;
    private static final boolean WITH_AUTHN_TIME = true;

    private static final String AUTHN_CLASS_REF = "http://proper_authn_indicator";
    private static final Set<String> AUTHN_METHOD_REFS = Sets.newHashSet("ref1", "ref2");
    private static final String NONCE = "334323";
    private static final long AUTHN_TIME = 23333333333L;

    @Test
    public void testEquals() {
        OpenIdConnectTokenGenerationState stateOne = buildState(WITH_AUTHN_CLASS_REF, WITH_AUTHN_METHOD_REFS, WITH_AUTHN_TIME);
        OpenIdConnectTokenGenerationState stateTwo = buildState(WITH_AUTHN_CLASS_REF, WITH_AUTHN_METHOD_REFS, WITH_AUTHN_TIME);
        assertEquals(stateOne, stateTwo);

        stateOne = buildState(!WITH_AUTHN_CLASS_REF, WITH_AUTHN_METHOD_REFS, WITH_AUTHN_TIME);
        stateTwo = buildState(!WITH_AUTHN_CLASS_REF, WITH_AUTHN_METHOD_REFS, WITH_AUTHN_TIME);
        assertEquals(stateOne, stateTwo);

        stateOne = buildState(!WITH_AUTHN_CLASS_REF, !WITH_AUTHN_METHOD_REFS, WITH_AUTHN_TIME);
        stateTwo = buildState(!WITH_AUTHN_CLASS_REF, !WITH_AUTHN_METHOD_REFS, WITH_AUTHN_TIME);
        assertEquals(stateOne, stateTwo);

        stateOne = buildState(WITH_AUTHN_CLASS_REF, WITH_AUTHN_METHOD_REFS, !WITH_AUTHN_TIME);
        stateTwo = buildState(WITH_AUTHN_CLASS_REF, WITH_AUTHN_METHOD_REFS, !WITH_AUTHN_TIME);
        assertEquals(stateOne, stateTwo);

        stateOne = buildState(WITH_AUTHN_CLASS_REF, !WITH_AUTHN_METHOD_REFS, !WITH_AUTHN_TIME);
        stateTwo = buildState(WITH_AUTHN_CLASS_REF, !WITH_AUTHN_METHOD_REFS, !WITH_AUTHN_TIME);
        assertEquals(stateOne, stateTwo);

        stateOne = buildState(!WITH_AUTHN_CLASS_REF, !WITH_AUTHN_METHOD_REFS, !WITH_AUTHN_TIME);
        stateTwo = buildState(!WITH_AUTHN_CLASS_REF, !WITH_AUTHN_METHOD_REFS, !WITH_AUTHN_TIME);
        assertEquals(stateOne, stateTwo);
    }

    @Test
    public void testJsonRoundTrip() {
        OpenIdConnectTokenGenerationState stateOne = buildState(WITH_AUTHN_CLASS_REF, WITH_AUTHN_METHOD_REFS, WITH_AUTHN_TIME);
        assertEquals(stateOne, OpenIdConnectTokenGenerationState.fromJson(stateOne.toJson()));

        stateOne = buildState(!WITH_AUTHN_CLASS_REF, !WITH_AUTHN_METHOD_REFS, !WITH_AUTHN_TIME);
        assertEquals(stateOne, OpenIdConnectTokenGenerationState.fromJson(stateOne.toJson()));

        stateOne = buildState(!WITH_AUTHN_CLASS_REF, WITH_AUTHN_METHOD_REFS, WITH_AUTHN_TIME);
        assertEquals(stateOne, OpenIdConnectTokenGenerationState.fromJson(stateOne.toJson()));

        stateOne = buildState(WITH_AUTHN_CLASS_REF, !WITH_AUTHN_METHOD_REFS, WITH_AUTHN_TIME);
        assertEquals(stateOne, OpenIdConnectTokenGenerationState.fromJson(stateOne.toJson()));

        stateOne = buildState(WITH_AUTHN_CLASS_REF, WITH_AUTHN_METHOD_REFS, !WITH_AUTHN_TIME);
        assertEquals(stateOne, OpenIdConnectTokenGenerationState.fromJson(stateOne.toJson()));

        stateOne = buildState(WITH_AUTHN_CLASS_REF, !WITH_AUTHN_METHOD_REFS, !WITH_AUTHN_TIME);
        assertEquals(stateOne, OpenIdConnectTokenGenerationState.fromJson(stateOne.toJson()));
    }

    private OpenIdConnectTokenGenerationState buildState(boolean withAuthnClassRef, boolean withAuthnMethodRefs, boolean withAuthNTime) {
        OpenIdConnectTokenGenerationState.OpenIdConnectTokenGenerationStateBuilder builder = OpenIdConnectTokenGenerationState.builder();
        if (withAuthnClassRef) {
            builder.authenticationContextClassReference(AUTHN_CLASS_REF);
        }
        if (withAuthnMethodRefs) {
            builder.authenticationMethodReferences(AUTHN_METHOD_REFS);
        }
        if (withAuthNTime) {
            builder.authenticationTimeInSeconds(AUTHN_TIME);
        }
        builder.nonce(NONCE);
        return builder.build();
    }
}
