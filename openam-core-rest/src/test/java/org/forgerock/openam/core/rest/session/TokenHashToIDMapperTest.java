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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.session;


import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.core.rest.session.TokenHashToIDMapper.END_USER_SESSION_ID_COOKIE;
import static org.forgerock.openam.core.rest.session.TokenHashToIDMapper.AM_COOKIE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iplanet.sso.SSOException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.services.baseurl.BaseURLConstants;
import org.forgerock.services.context.Context;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TokenHashToIDMapperTest {

    private Context context;
    private HttpContext httpContext;
    private String agentSession = "agentSessionId";
    private String defaultSession = "defaultSessionId";
    private String agentSessionHash;
    private String defaultSessionHash;
    private Map<String, List<String>> headers;

    private TokenHashToIDMapper mapper;

    @BeforeMethod
    public void setUp() throws Exception {

        mapper = new TokenHashToIDMapper();

        context = mock(Context.class);
        headers = new HashMap<>();
        headers.put("cookie", Collections.<String>emptyList());
        headers.put(END_USER_SESSION_ID_COOKIE, Arrays.asList(agentSession));
        headers.put(AM_COOKIE.toLowerCase(), Arrays.asList(defaultSession));

        agentSessionHash =  Hex.encodeHexString(mapper.hash(agentSession));
        defaultSessionHash =  Hex.encodeHexString(mapper.hash(defaultSession));
    }

    private void setupHttpContext() {
        httpContext = new HttpContext(json(object(
                field(BaseURLConstants.ATTR_HEADERS, headers),
                field(BaseURLConstants.ATTR_PARAMETERS, Collections.emptyMap()))), null);
        given(context.asContext(HttpContext.class)).willReturn(httpContext);
    }

    @Test
    public void shouldThrowSSOExceptionWhenAgentSessionMappingFails() throws SSOException {
        //given
        headers.put(END_USER_SESSION_ID_COOKIE, Arrays.asList("wrongSessionId"));
        setupHttpContext();

        //when
        try {
            mapper.map(context, agentSessionHash);
            fail("Expected SSOException was not thrown");
        } catch (SSOException e) {
        }
    }

    @Test
    public void shouldThrowSSOExceptionWhenDefaultSessionMappingFails() throws SSOException {
        //given
        headers.put(END_USER_SESSION_ID_COOKIE, Collections.<String>emptyList());
        headers.put(AM_COOKIE.toLowerCase(), Arrays.asList("wrongSessionId"));
        setupHttpContext();

        //when
        try {
            mapper.map(context, defaultSessionHash);
            fail("Expected SSOException was not thrown");
        } catch (SSOException e) {
        }
    }

    @Test
    public void shouldReturnAgentSessionWhenPresent() throws SSOException {
        //given
        headers.put(END_USER_SESSION_ID_COOKIE, Arrays.asList(agentSession));
        setupHttpContext();

        //when
        String token = mapper.map(context, agentSessionHash);

        //then
        assertEquals(token, agentSession);
    }

    @Test
    public void shouldReturnDefaultSessionWhenAgentSessionNotPresent() throws SSOException {
        //given
        headers.remove(END_USER_SESSION_ID_COOKIE);
        headers.put(AM_COOKIE.toLowerCase(), Arrays.asList(agentSession));
        setupHttpContext();

        //when
        String token = mapper.map(context, agentSessionHash);

        //then
        assertEquals(token, agentSession);
    }
}
