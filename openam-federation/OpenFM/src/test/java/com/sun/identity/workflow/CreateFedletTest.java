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
package com.sun.identity.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

@Test
public class CreateFedletTest {

    private static final String IDP_CONFIG = "<IDPSSOConfig metaAlias=\"/idp\"/>";

    public void hostedFlagIsFlippedOnUnprefixedRoot() {
        String config = "<EntityConfig xmlns=\"urn:sun:fm:SAML:2.0:entityconfig\""
                + " entityID=\"https://idp.example.com\" hosted=\"true\">" + IDP_CONFIG + "</EntityConfig>";

        assertThat(CreateFedlet.flipHostedParameter(config, false))
                .contains(" hosted=\"0\">")
                .endsWith(IDP_CONFIG + "</EntityConfig>");
        assertThat(CreateFedlet.flipHostedParameter(config, true)).contains(" hosted=\"1\">");
    }

    /**
     * The JAXB RI gives the root element a generated prefix when its context holds an element in no
     * namespace, for example from a package added through com.sun.identity.liberty.ws.jaxb.packageList.
     */
    public void hostedFlagIsFlippedOnPrefixedRoot() {
        String config = "<ns3:EntityConfig xmlns:ns3=\"urn:sun:fm:SAML:2.0:entityconfig\""
                + " entityID=\"https://idp.example.com\" hosted=\"true\">" + IDP_CONFIG + "</ns3:EntityConfig>";

        assertThat(CreateFedlet.flipHostedParameter(config, false)).contains(" hosted=\"0\">");
    }
}
