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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.rest.fluent;

import static org.forgerock.openam.audit.AuditConstants.NO_REALM;
import static org.forgerock.openam.rest.fluent.JsonUtils.jsonFromFile;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import javax.security.auth.Subject;

import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.audit.AMAccessAuditEventBuilder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.rest.resource.AuditInfoContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RequestAuditContext;
import org.forgerock.services.context.SecurityContext;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

public class AuditTestUtils {

    public static AuditEventFactory mockAuditEventFactory() {
        AuditEventFactory auditEventFactory = mock(AuditEventFactory.class);
        when(auditEventFactory.accessEvent(NO_REALM)).thenAnswer(new Answer<AMAccessAuditEventBuilder>() {
            @Override
            public AMAccessAuditEventBuilder answer(InvocationOnMock invocation) throws Throwable {
                return new AMAccessAuditEventBuilder();
            }
        });
        return auditEventFactory;
    }

    public static Context mockAuditContext() throws Exception {
        final Context httpContext = new HttpContext(
                jsonFromFile("/org/forgerock/openam/rest/fluent/httpContext.json"),
                AbstractAuditFilterTest.class.getClassLoader());
        final Subject callerSubject = new Subject();
        final Context securityContext = new SecurityContext(httpContext, null, null);
        final Context subjectContext = new SSOTokenContext(mock(Debug.class), null, securityContext) {
            @Override
            public Subject getCallerSubject() {
                return callerSubject;
            }

            @Override
            public SSOToken getCallerSSOToken() {
                SSOToken token = mock(SSOToken.class);
                try {
                    given(token.getProperty(Constants.AM_CTX_ID)).willReturn("TRACKING_ID");
                    given(token.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("USER_ID");
                } catch (SSOException e) {
                    // won't happen - it's a mock
                }
                return token;
            }
        };
        final Context clientContext = ClientContext.newInternalClientContext(subjectContext);
        return new RequestAuditContext(new AuditInfoContext(clientContext, AuditConstants.Component.AUDIT));
    }
}
