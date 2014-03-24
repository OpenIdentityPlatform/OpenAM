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
package org.forgerock.restlet.ext.oauth2;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Map;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.representation.InputRepresentation;
import org.testng.annotations.Test;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OAuth2UtilsTest {
    @Test
    public void testGetRequestParameter() throws Exception {
        Reference ref =
                new Reference("https://client.example.com/cb#error=access_denied&state=xyz");
        Form form = new Form(ref.getFragment());
        form.add("access_token", "value");
        ref.setFragment(form.getQueryString());
    }

    // @Test
    public void testJsonPostRequestParameter() throws Exception {
        InputStream is = OAuth2UtilsTest.class.getResourceAsStream("code.json");
        assertNotNull(is);
        InputRepresentation representation = new InputRepresentation(is, MediaType.TEXT_PLAIN);
        Request request = new Request(Method.POST, "riap://test.json", representation);
        Map<String, String> form = OAuth2Utils.ParameterLocation.HTTP_BODY.getParameters(request);
        assertEquals(form.get(OAuth2Constants.Params.RESPONSE_TYPE), "code");
    }
}
