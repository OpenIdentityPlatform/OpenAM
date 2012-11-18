/*
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
 *
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
 * information: "Portions Copyrighted [2012] [ForgeRock Inc]".
 *
 */
package org.forgerock.openam.ext.cts.repo;

import com.iplanet.dpro.session.exceptions.StoreException;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.coretoken.interfaces.AMTokenRepository;
import com.sun.identity.coretoken.interfaces.OAuth2TokenRepository;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.CTSPersistentStore;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import com.sun.identity.sm.model.TokenDataEntry;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.session.ha.amsessionstore.store.opendj.EmbeddedSearchResultIterator;
import org.opends.server.core.AddOperation;
import org.opends.server.core.DeleteOperation;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.*;
import org.restlet.Request;

import java.util.*;

public class OpenDJTokenRepo implements JsonResource {


    final static Debug debug = Debug.getInstance("CTS");
    private OAuth2TokenRepository cts = null;

    /**
     * Create the CTSPersistentStore connection based on settings in system properties, and start the timer for cleanup operations.
     *
     * @throws Exception
     */
    public OpenDJTokenRepo() throws Exception {

        try {
            cts = CTSPersistentStore.getInstance();
        } catch (StoreException e){
            throw new JsonResourceException(JsonResourceException.UNAVAILABLE, "Core Token Service is Unavailable");
        }
    }

    /**
     * Handles a JSON resource request by dispatching to the method corresponding with the
     * method member of the request. If the request method is not one of the standard JSON
     * resource request methods, a {@code JsonResourceException} is thrown.
     * <p/>
     * This method catches any thrown {@code JsonValueException}, and rethrows it as a
     * {@link JsonResourceException#BAD_REQUEST}. This allows the use of JsonValue methods
     * to validate the content of the request.
     *
     * @param request the JSON resource request.
     * @return the JSON resource response.
     * @throws if there is an exception handling the request.
     */
    @Override
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        try {
            try {
                JsonValue result = null;
                switch (request.get("method").required().asEnum(SimpleJsonResource.Method.class)) {
                    case create:
                        try{
                            result = cts.oauth2Create(request);
                            if (OAuth2Utils.logStatus) {
                                String[] obs = {"CREATED_TOKEN", request.toString()};
                                OAuth2Utils.logAccessMessage("CREATED_TOKEN", obs, OAuth2Utils.getSSOToken(Request.getCurrent()));
                            }
                            return  result;
                        } catch(JsonResourceException e){
                            OAuth2Utils.DEBUG.error("Create Token failed", e);
                            if (OAuth2Utils.logStatus) {
                                String[] obs = {"FAILED_CREATE_TOKEN", request.toString()};
                                OAuth2Utils.logErrorMessage("FAILED_CREATE_TOKEN", obs, OAuth2Utils.getSSOToken(Request.getCurrent()));
                            }
                            throw e;
                        }
                    case read:
                        return cts.oauth2Read(request);
                    case update:
                        return cts.oauth2Update(request);
                    case delete:
                        try{
                            result = cts.oauth2Delete(request);
                            if (OAuth2Utils.logStatus) {
                                String[] obs = {"FAILED_DELETE_TOKEN", request.toString()};
                                OAuth2Utils.logAccessMessage("FAILED_DELETE_TOKEN", obs, OAuth2Utils.getSSOToken(Request.getCurrent()));
                            }
                            return  result;
                        } catch(JsonResourceException e){
                            OAuth2Utils.DEBUG.error("Delete Token failed", e);
                            if (OAuth2Utils.logStatus) {
                                String[] obs = {"DELETE_FAILED", request.toString()};
                                OAuth2Utils.logErrorMessage("DELETE_FAILED", obs, OAuth2Utils.getSSOToken(Request.getCurrent()));
                            }
                            throw e;
                        }
                    case query:
                        return cts.oauth2Query(request);
                    default:
                        throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
                }
            } catch (JsonValueException jve) {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, jve);
            }
        } catch (Exception e1) {
            try {
                //onException(e1); // give handler opportunity to throw its own exception
                throw e1;
            } catch (Exception e2) {
                if (e2 instanceof JsonResourceException) { // no rethrowing necessary
                    throw (JsonResourceException) e2;
                } else { // need to rethrow as resource exception
                    throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e2);
                }
            }
        }
    }
}
