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

package org.forgerock.openam.http;

import static com.sun.identity.shared.Constants.REST_APIS_SERVICE_NAME;
import static com.sun.identity.shared.Constants.REST_APIS_SERVICE_VERSION;
import static org.forgerock.http.swagger.OpenApiRequestFilter.API_PARAMETER;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_CREST_API;
import static org.forgerock.openam.utils.ServiceConfigUtils.getBooleanAttribute;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.security.AccessController;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

/**
 * A filter for API Descriptor requests based on whether they are enabled in the {@code RestApisService}.
 */
public class ApiDescriptorFilter implements Filter {

    private static final String DESCRIPTOR_ENABLED_ATTRIBUTE = "openam-rest-apis-descriptions-enabled";

    /**
     * A singleton for whether the API Description is currently enabled.
     */
    public enum State implements ServiceListener {
        /** Singleton instance. */
        INSTANCE;

        private final Logger logger = LoggerFactory.getLogger(State.class);
        private volatile boolean enabled;

        State() {
            try {
                setState();
                getServiceConfigManager().addListener(this);
            } catch (SSOException | SMSException e) {
                throw new IllegalStateException("Can't initialise API Descriptor state", e);
            }
        }

        /**
         * Get the current enabled state.
         * @return The state.
         */
        public boolean isEnabled() {
            return enabled;
        }

        private void setState() throws SSOException, SMSException {
            ServiceConfigManager mgr = getServiceConfigManager();
            enabled = getBooleanAttribute(mgr.getGlobalConfig(null), DESCRIPTOR_ENABLED_ATTRIBUTE);
        }

        private ServiceConfigManager getServiceConfigManager() throws SMSException, SSOException {
            SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            return new ServiceConfigManager(token, REST_APIS_SERVICE_NAME, REST_APIS_SERVICE_VERSION);
        }

        @Override
        public void schemaChanged(String serviceName, String version) {
            if (serviceName.equals(REST_APIS_SERVICE_NAME) && version.equals(REST_APIS_SERVICE_VERSION)) {
                try {
                    setState();
                } catch (SSOException | SMSException e) {
                    logger.warn("Could not update API Descriptor state", e);
                }
            }
        }

        @Override
        public void globalConfigChanged(String serviceName, String version, String groupName,
                String serviceComponent, int type) {
            schemaChanged(serviceName, version);
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
                String serviceComponent, int type) {
            // not expected - global config
        }
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        Form form = request.getForm();
        if ((form.containsKey(API_PARAMETER) || form.containsKey(PARAM_CREST_API))
                && !State.INSTANCE.isEnabled()) {
            return newResultPromise(new Response(Status.NOT_IMPLEMENTED));
        }
        return next.handle(context, request);
    }
}
