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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest;

import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.servlet.HttpContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.*;

public class RestUtilsTest {
    private static final String HEADER = "testHeader";

    @DataProvider(name = "getMimeHeaderValueData")
    public Object[][] getMimeHeaderValueData() {
        return new Object[][] {
                { new String[]{},                                   null },
                { new String[]{ "a normal value" },                 "a normal value" },
                { new String[]{ "one", "two"},                      "one" },
                { new String[]{ "=?UTF-8?B?Q2Fmw6k=?=" },           "Caf\u00e9" },
                { new String[]{ "=?UTF-8?Q?Caf=C3=A9?=" },          "Caf\u00e9" },
                { new String[]{ "abc?def=hjk"},                     "abc?def=hjk"}
        };
    }

    @Test(dataProvider = "getMimeHeaderValueData")
    public void shouldParseMimeHeaderValuesCorrectly(String[] values, String expectedResult) throws Exception {
        // Given
        ServerContext context = getHttpServerContext(values);

        // When
        String result = RestUtils.getMimeHeaderValue(context, HEADER);

        // Then
        assertThat(result).isEqualTo(expectedResult);
    }

    private ServerContext getHttpServerContext(String...values) throws Exception {
        final HttpContext httpContext = new HttpContext(json(object(
                field(HttpContext.ATTR_HEADERS, Collections.singletonMap(HEADER, Arrays.asList(values))),
                field(HttpContext.ATTR_PARAMETERS, Collections.emptyMap()))), null);
        return new ServerContext(httpContext);
    }
}