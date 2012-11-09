/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.restlet.ext.oauth2.flow;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.Test;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ErrorServerResourceTest extends AbstractFlowTest {
    @Test
    public void testCheckedScope() throws Exception {
        Set<String> allowedGrantScopes =
                OAuth2Utils.split("read list write", OAuth2Utils.getScopeDelimiter(null));
        assertThat(allowedGrantScopes).hasSize(3).containsOnly("read", "list", "write");
        Context ctx = new Context();
        OAuth2Utils.setScopeDelimiter(",", ctx);
        Set<String> defaultGrantScopes =
                OAuth2Utils.split("read,list", OAuth2Utils.getScopeDelimiter(ctx));
        assertThat(defaultGrantScopes).hasSize(2).containsOnly("read", "list");

        ErrorServerResource testable = new ErrorServerResource();
        Set<String> checkedScope =
                testable.getCheckedScope(null, allowedGrantScopes, defaultGrantScopes);
        assertThat(checkedScope).isEqualTo(defaultGrantScopes);

        checkedScope = testable.getCheckedScope("read", allowedGrantScopes, defaultGrantScopes);
        assertThat(checkedScope).containsOnly("read");

        checkedScope =
                testable.getCheckedScope("read delete", allowedGrantScopes, defaultGrantScopes);
        assertThat(checkedScope).containsOnly("read");
    }

    @Test
    public void testInvalidRealm() throws Exception {
        Reference reference = new Reference("riap://component/none/oauth2/access_token");
        Request request = new Request(Method.POST, reference);
        Response response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_NOT_FOUND);
    }

    @Test
    public void testInvalidGrantType() throws Exception {
        Reference reference = new Reference("riap://component/test/oauth2/access_token");
        Request request = new Request(Method.POST, reference);
        Response response = new Response(request);

        Form parameters = new Form();
        parameters.add(OAuth2Constants.Params.GRANT_TYPE, OAuth2Constants.SAML20.GRANT_TYPE_URI);
        request.setEntity(parameters.getWebRepresentation());

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_FORBIDDEN);

        reference = new Reference("riap://component/test/oauth2/access_token");
        request = new Request(Method.POST, reference);
        response = new Response(request);

        // handle
        getClient().handle(request, response);
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_NOT_FOUND);
        assertTrue(response.getEntity() instanceof JacksonRepresentation);
    }
}
