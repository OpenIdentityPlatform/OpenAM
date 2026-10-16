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
package com.sun.identity.saml.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import org.testng.annotations.Test;

public class SAMLUtilsTest {

    /**
     * postYN() only checks host, port and path of the target, so the query string
     * is attacker-influenced and must not be able to close the ACTION attribute.
     */
    @Test
    public void postToTargetEscapesTargetUrlInFormAction() throws Exception {
        String target = "https://sp.example.com/acs?next=\"><script>alert(1)</script><a href=\"";
        StringWriter html = new StringWriter();

        SAMLUtils.postToTarget(null, new PrintWriter(html), Collections.emptyList(), target,
                Collections.emptyMap());

        String out = html.toString();
        assertThat(out).doesNotContain("<script>");
        assertThat(out).contains("ACTION=\"https://sp.example.com/acs?next=&quot;&gt;&lt;script&gt;");
    }
}
