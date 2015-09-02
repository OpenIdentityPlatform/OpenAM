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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.forgerockrest.utils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.AbstractMap;

import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.caf.authentication.framework.AuditTrail;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.testng.annotations.Test;

public class XMLResourceExceptionHandlerTest {

    private XMLResourceExceptionHandler handler = new XMLResourceExceptionHandler();

    @Test
    public void testWrite() throws Exception {
        //given
        MessageContext context = mock(MessageContext.class);
        AuditTrail mockAudit = mock(AuditTrail.class);
        Response response = new Response();
        doReturn(mockAudit).when(context).getAuditTrail();
        doReturn(response).when(context).getResponse();

        String message = "I don't know where it is";
        ResourceException ex = new NotFoundException(message);
        AuthenticationException ex2 = new AuthenticationException(ex);

        //when
        handler.write(context, ex2);

        //then
        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND);
        
        String text = response.getEntity().getString();
        assertThat(text).contains("<message>" + message + "</message>");
        assertThat(text).contains("<code>404</code>");
    }

    @Test
    public void testAsXMLDOM() throws Exception {
        //given
        ResourceException ex = new NotFoundException("I don't know where it is");
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
