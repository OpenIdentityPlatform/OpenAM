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

package org.forgerock.openam.forgerockrest.utils;

import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.AbstractMap;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

public class XMLResourceExceptionHandlerTest {

    private XMLResourceExceptionHandler handler = new XMLResourceExceptionHandler();

    @Test
    public void testWrite() throws Exception {
        //given
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        doReturn(new PrintWriter(writer)).when(response).getWriter();

        String message = "I don't know where it is";
        ResourceException ex = ResourceException.getException(404, message);

        //when
        handler.write(ex, response);

        //then
        verify(response).setContentType("application/xml");
        String text = writer.getBuffer().toString();
        assertThat(text).contains("<message>" + message + "</message>");
        assertThat(text).contains("<code>404</code>");
    }

    @Test
    public void testAsXMLDOM() throws Exception {
        //given
        ResourceException ex = ResourceException.getException(404, "I don't know where it is");
        AbstractMap.SimpleEntry<String, Integer> entry = new AbstractMap.SimpleEntry<String, Integer>("a", 1);
        ex.setDetail(new JsonValue(JsonValue.array(new JsonValue(JsonValue.object(entry)))));

        //when
        String text = XMLUtils.print(handler.asXMLDOM(ex.toJsonValue().asMap()).getDocumentElement());

        //then
        assertThat(text).contains("<message>I don't know where it is</message>");
        assertThat(text).contains("<code>404</code>");
        assertThat(text).contains("<detail><entry><a>1</a></entry></detail>");
    }
}