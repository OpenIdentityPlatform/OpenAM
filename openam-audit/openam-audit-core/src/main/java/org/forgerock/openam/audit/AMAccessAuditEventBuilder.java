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
package org.forgerock.openam.audit;

import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.*;
import static org.forgerock.openam.utils.ClientUtils.getClientIPAddress;

import com.iplanet.sso.SSOToken;
import org.forgerock.audit.events.AccessAuditEventBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for OpenAM audit access events.
 *
 * @since 13.0.0
 */
public final class AMAccessAuditEventBuilder extends AccessAuditEventBuilder<AMAccessAuditEventBuilder> {

    /**
     * Provide value for "extraInfo" audit log field.
     *
     * @param values String sequence of values that should be stored in the 'extraInfo' audit log field.
     * @return this builder for method chaining.
     */
    public AMAccessAuditEventBuilder extraInfo(String... values) {
        putExtraInfo(jsonValue, values);
        return this;
    }

    /**
     * Provide value for "contextId" audit log field.
     *
     * @param value String "contextId" value.
     * @return this builder for method chaining.
     */
    public AMAccessAuditEventBuilder contextId(String value) {
        putContextId(jsonValue, value);
        return this;
    }

    /**
     * Provide value for "component" audit log field.
     *
     * @param value String "component" value.
     * @return this builder for method chaining.
     */
    public AMAccessAuditEventBuilder component(String value) {
        putComponent(jsonValue, value);
        return this;
    }

    /**
     * Sets contextId from property of {@link SSOToken}, iff the provided
     * <code>SSOToken</code> is not <code>null</code>.
     *
     * @param ssoToken The SSOToken from which the contextId value will be retrieved.
     * @return this builder
     */
    public AMAccessAuditEventBuilder contextIdFromSSOToken(SSOToken ssoToken) {
        putContextIdFromSSOToken(jsonValue, ssoToken);
        return this;
    }

    /**
     * Sets client, server and http details from HttpServletRequest.
     *
     * @param request HttpServletRequest from which client, server and http details will be retrieved.
     * @return this builder
     */
    public final AMAccessAuditEventBuilder forHttpServletRequest(HttpServletRequest request) {
        client(
                getClientIPAddress(request),
                request.getRemotePort(),
                isReverseDnsLookupEnabled() ? request.getRemoteHost() : "");
        server(
                request.getLocalAddr(),
                request.getLocalPort(),
                request.getLocalName());
        http(
                request.getMethod(),
                request.getRequestURL().toString(),
                request.getQueryString() == null ? "" : request.getQueryString(),
                getHeadersAsMap(request));
        return this;
    }

    private Map<String, List<String>> getHeadersAsMap(HttpServletRequest request) {
        Map<String, List<String>> headers = new HashMap<>();
        Enumeration headerNamesEnumeration = request.getHeaderNames();
        while (headerNamesEnumeration.hasMoreElements()) {
            String headerName = (String) headerNamesEnumeration.nextElement();
            List<String> headerValues = new ArrayList<>();
            Enumeration headersEnumeration = request.getHeaders(headerName);
            while (headersEnumeration.hasMoreElements()) {
                headerValues.add((String) headersEnumeration.nextElement());
            }
            headers.put(headerName, headerValues);
        }
        return headers;
    }

}