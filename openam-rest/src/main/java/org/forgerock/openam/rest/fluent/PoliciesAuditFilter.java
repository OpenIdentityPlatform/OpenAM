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

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.Response;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Filter which will audit any policy requests that pass through it. Acts exactly as the {@link AuditFilter} and
 * exists solely to provide an implementation of the {@link #getDetail(Response)}} method.
 *
 * @since 13.0.0
 */
public class PoliciesAuditFilter extends AuditFilter {

    /**
     * Guiced constructor.
     *
     * @param debug Debug instance.
     * @param auditEventPublisher AuditEventPublisher to which publishing of events can be delegated.
     * @param auditEventFactory AuditEventFactory for audit event builders.
     */
    @Inject
    public PoliciesAuditFilter(@Named("frRest") Debug debug, AuditEventPublisher auditEventPublisher,
                       AuditEventFactory auditEventFactory) {
        super(debug, auditEventPublisher, auditEventFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue getDetail(Response response) {
        Reject.ifNull(response, "response cannot be null.");

        JsonValue detail;
        try {
            //Currently the only action org.forgerock.openam.entitlement.rest.PolicyResource implements is the
            //policy evaluation action, so if we can do .asSubclass(ActionResponse.class), then the response
            //is a policy evaluation response. In the event of future amendments to the action implementation
            //within org.forgerock.openam.entitlement.rest.PolicyResource, a more sophisticated method for
            //performing this check may be needed.
            response.getClass().asSubclass(ActionResponse.class);
            detail = ((ActionResponse) response).getJsonContent();
        } catch (ClassCastException cce) {
            detail = null;
        }
        return detail;
    }
}
