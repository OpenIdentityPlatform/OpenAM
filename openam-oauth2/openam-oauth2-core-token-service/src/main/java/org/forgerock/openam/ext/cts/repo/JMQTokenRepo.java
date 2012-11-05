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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 */

package org.forgerock.openam.ext.cts.repo;

import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.ha.FAMPersisterManager;
import com.sun.identity.sm.model.FAMRecord;
import com.sun.identity.ha.FAMRecordPersister;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.restlet.ext.oauth2.OAuth2;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class is a repository capable of storing and retrieving tokens in the JMQ/BDB session repository.
 *
 * @author Jonathan Scudder
 */
@Deprecated
public class JMQTokenRepo extends GeneralTaskRunnable implements JsonResource {

    final static Debug debug = Debug.getInstance("CTS");

    private static boolean isDatabaseUp = true;

    private static final String DB_ERROR_MSG = "CoreTokenService is not available at this moment.";

    // Time period between two successive runs of repository cleanup thread which checks and removes expired records
    private static long CLEANUPPERIOD = 5 * 60 * 1000; // 5 min in milliseconds
    private static long CLEANUPVALUE = 0;

    // TODO rename
    public static final String CLEANUP_RUN_PERIOD = "org.forgerock.ext.cts.repo.cleanupRunPeriod";

    // Time period between two successive runs of DBHealthChecker thread which checks for Database availability
    private static long HEALTHCHECKPERIOD = 1 * 60 * 1000;
    // TODO rename
    public static final String HEALTH_CHECK_RUN_PERIOD = "org.forgerock.ext.cts.repo.healthCheckRunPeriod";

    // This period is actual one that is used by the thread, smallest value of cleanUpPeriod and healthCheckPeriod
    private static long RUNPERIOD = 1 * 60 * 1000; // 1 min in milliseconds

    //
    private static long DEFAULTEXPIRYDURATION = 1 * 60 * 1000; // 1 min in milliseconds

    private String cts = "CTS";
    private String serverId;


    static {
        /*
        try {
            gracePeriod = Integer.parseInt(SystemPropertiesManager.get(CLEANUP_GRACE_PERIOD, String.valueOf(gracePeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + CLEANUP_GRACE_PERIOD + ", using default");
        }

        try {
            cleanUpPeriod = Integer.parseInt(SystemPropertiesManager.get(CLEANUP_RUN_PERIOD, String.valueOf(cleanUpPeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + CLEANUP_RUN_PERIOD + ", using default");
        }

        try {
            healthCheckPeriod = Integer.parseInt(SystemPropertiesManager.get(HEALTH_CHECK_RUN_PERIOD, String.valueOf(healthCheckPeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + HEALTH_CHECK_RUN_PERIOD + ", using default");
        }
        */

        RUNPERIOD = (CLEANUPPERIOD <= HEALTHCHECKPERIOD) ? CLEANUPPERIOD : HEALTHCHECKPERIOD;
        CLEANUPVALUE = CLEANUPPERIOD;
    }

    // The OpenAM message queue used for session and SAML2
    public FAMRecordPersister pSession = null;

    /**
     * Create the JMQ connection based on settings in system properties, and start the timer for cleanup operations.
     *
     * @throws Exception
     */
    public JMQTokenRepo() throws Exception {

        String thisSessionServerProtocol = SystemPropertiesManager.get(Constants.AM_SERVER_PROTOCOL);
        String thisSessionServer = SystemPropertiesManager.get(Constants.AM_SERVER_HOST);
        String thisSessionServerPortAsString = SystemPropertiesManager.get(Constants.AM_SERVER_PORT);
        String thisSessionURI = SystemPropertiesManager.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

        if (thisSessionServerProtocol == null
                || thisSessionServerPortAsString == null
                || thisSessionServer == null) {
            throw new IllegalStateException("JMQ session failover not correctly configured");
        }

        serverId = WebtopNaming.getServerID(thisSessionServerProtocol,
                thisSessionServer, thisSessionServerPortAsString,
                thisSessionURI);
        initPersistSession();

        SystemTimer.getTimer().schedule(this, new Date((System.currentTimeMillis() / 1000) * 1000));
    }

    /**
     * Initialize new FAMRecord persister.
     */
    private void initPersistSession() {
        try {
            pSession = FAMPersisterManager.getInstance().getFAMRecordPersister();
            isDatabaseUp = true;
        } catch (Exception e) {
            isDatabaseUp = false;
            debug.error(DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        }
    }

    /**
     * Creates a new token, stores it and returns the token ID. If the request includes an ID then that is the ID that is used for
     * the created object; if the ID is in the payload then that is used, and failing that an ID will be generated by
     * this component.
     *
     * @param request the JSON request
     * @return the JSON response, including the ID that may have been generated during creation
     * @throws JsonResourceException
     * @see // TODO link to json request/response documentation
     */
    protected JsonValue create(JsonValue request) throws JsonResourceException {
        if (!isDatabaseUp) {
            throw new JsonResourceException(JsonResourceException.UNAVAILABLE);
        }

        // TODO: validate request
        if (request == null) {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, "Request was null");
        }
        if (request.get("value").isNull()) {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, "No value to store was found in the request");
        }

        // Get id from the request, not the value
        String requestId = request.get("id").required().asString();

        String primaryKey = null;
        String secondaryKey = request.get("value").get(OAuth2.StoredToken.PARENT).asString();
        long expiryTime = request.get("value").get(OAuth2.StoredToken.EXPIRY_TIME).required().asLong();

        // Generate the token ID or set to the value provided in the request or payload
        if (requestId != null) {
            primaryKey = requestId;
            request.get("value").required().put("id", primaryKey);
        } else {
            primaryKey = UUID.randomUUID().toString();
            //TODO request.get("value").put("id", primaryKey);
        }

        // Set expiry time to default
        // TODO make default expiry time configurable
        if (expiryTime <= 0) {
            expiryTime = System.currentTimeMillis() + DEFAULTEXPIRYDURATION;
        }

        try {
            byte[] blob = SessionUtils.encode(request.get("value").getObject());
            FAMRecord famRec = new FAMRecord(cts, FAMRecord.WRITE, primaryKey, expiryTime, secondaryKey, 0, null, blob);
            FAMRecord retRec = pSession.send(famRec);

            JsonValue retValue = new JsonValue(new HashMap<String, Object>());
            retValue.put("_id", primaryKey);
            retValue.put("_rev", null); // TODO: relevance of revision
            return retValue;

        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            // TODO revamp logging
            //logDBStatus();
            debug.error(DB_ERROR_MSG, e);
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        } catch (Exception e) {
            debug.error("Failed to create object", e);
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        }
    }

    /**
     * Reads a token from the JMQ based on the request provided, locating token by id. If a token is to be retrieved
     * using the secondary key then query must be used instead.
     *
     * @param request the json request object
     * @return the json response including a value representing the entire token
     * @throws JsonResourceException
     * @see // TODO link to json request/response documentation
     */
    protected JsonValue read(JsonValue request) throws JsonResourceException {
        if (!isDatabaseUp) {
            throw new JsonResourceException(JsonResourceException.UNAVAILABLE);
        }

        // Read the object using the primary key
        // If trying to read based on the secondary key, use a query instead
        String primaryKey = request.get("id").required().asString();

        try {
            FAMRecord famRec = new FAMRecord(cts, FAMRecord.READ, primaryKey, 0, null, 0, null, null);
            FAMRecord retRec = pSession.send(famRec);
            if (null == retRec) {
                throw new JsonResourceException(JsonResourceException.NOT_FOUND, "Object not found with id: " + primaryKey);
            }
            byte[] blob = retRec.getSerializedInternalSessionBlob();
            Object retObj = SessionUtils.decode(blob);
            Map tokenObj = (Map) retObj; // TODO: check cast  // Jeff: This is InternalSession or Binary Data !

            JsonValue retValue = new JsonValue(tokenObj);
            retValue.put("_id", primaryKey);
            retValue.put("_rev", null); // TODO: relevance of revision
            // TODO: confirm Json response object
            return retValue;

        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            //logDBStatus();
            debug.error(DB_ERROR_MSG, e);
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        } catch (Exception e) {
            debug.error("Failed to read object", e);
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        }

    }

    /**
     * Updates a token in the store. Note that this will overwrite the previous token completely. In the event that the
     * original token has not been stored, the token will be created.
     *
     * @param request the JSON request object including the entire token object to be stored
     * @return JSON response including the ID of the token
     * @throws JsonResourceException
     * @see // TODO link to JSON request/response documentation
     */
    protected JsonValue update(JsonValue request) throws JsonResourceException {
        // Update is run in the same way as a create
        return create(request);
    }

    /**
     * Deletes a token from the store based on the ID in the request.
     *
     * @param request the JSON request object including the ID of the token to delete
     * @return null
     * @throws JsonResourceException
     * @see // TODO link to JSON request/response documentation
     */
    protected JsonValue delete(JsonValue request) throws JsonResourceException {
        if (!isDatabaseUp) {
            throw new JsonResourceException(JsonResourceException.UNAVAILABLE);
        }

        String primaryKey = request.get("id").required().asString();

        try {
            FAMRecord famRec = new FAMRecord(cts, FAMRecord.DELETE, primaryKey, 0, null, 0, null, null);
            FAMRecord retRec = pSession.send(famRec);

            JsonValue retValue = new JsonValue(null);
            return retValue;

        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            //logDBStatus();
            debug.error(DB_ERROR_MSG, e);
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        } catch (Exception e) {
            debug.error("Failed to read object", e);
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        }

    }

    /**
     * Applies a patch to a token in the store by retrieving, patching, then updating. This method does not currently
     * support changing the primary or secondary key of the token.
     *
     * @param request the JSON request including the patch object
     * @return simple JSON response of _id and _rev
     * @throws JsonResourceException
     * @see // TODO link to JSON request/response documentation
     */
    protected JsonValue patch(JsonValue request) throws JsonResourceException {
        String primaryKey = request.get("id").asString();
        JsonValue patch = request.get("value");

        JsonValue readRequest = new JsonValue(new HashMap<String, Object>());
        readRequest.put("id", primaryKey);
        readRequest.put("method", "read");

        // Read the token
        JsonValue readResponse = handle(readRequest);
        JsonValue originalToken = readResponse.get("value");

        // Patch it
        JsonPatch.patch(originalToken, patch);

        // Update the token
        JsonValue updateRequest = new JsonValue(new HashMap<String, Object>());
        updateRequest.put("id", primaryKey);
        updateRequest.put("value", originalToken);
        // TODO handle case where primary key or secondary key changes

        JsonValue retValue = new JsonValue(new HashMap<String, Object>());
        retValue.put("_id", primaryKey);
        retValue.put("_rev", null); // TODO: relevance of revision
        return retValue;
    }

    // From interface // TODO: annotate
    protected JsonValue action(JsonValue request) throws JsonResourceException {
        if (!isDatabaseUp) {
            throw new JsonResourceException(JsonResourceException.UNAVAILABLE);
        }
        throw new JsonResourceException(JsonResourceException.FORBIDDEN);
    }

    // From interface // TODO: annotate
    protected JsonValue query(JsonValue request) throws JsonResourceException {
        if (!isDatabaseUp) {
            throw new JsonResourceException(JsonResourceException.UNAVAILABLE);
        }
        // TODO TODO TODO
        throw new JsonResourceException(JsonResourceException.FORBIDDEN);
    }

    /**
     * Triggers deletion of all expired resources in the store.
     *
     * @throws JsonResourceException
     */
    protected void deleteExpired() throws JsonResourceException {
        if (!isDatabaseUp) {
            throw new JsonResourceException(JsonResourceException.UNAVAILABLE);
        }

        long date = System.currentTimeMillis() / 1000;

        try {
            FAMRecord famRec = new FAMRecord(cts, FAMRecord.DELETEBYDATE, null, date, null, 0, null, null);
            FAMRecord retRec = pSession.send(famRec);

        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            //logDBStatus();
            debug.error(DB_ERROR_MSG, e);
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        } catch (Exception e) {
            debug.error("Failed to read object", e);
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
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
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        try {
            try {
                switch (request.get("method").required().asEnum(SimpleJsonResource.Method.class)) {
                    case create:
                        return create(request);
                    case read:
                        return read(request);
                    case update:
                        return update(request);
                    case delete:
                        return delete(request);
                    case patch:
                        return patch(request);
                    case query:
                        return query(request);
                    case action:
                        return action(request);
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


    // Overrides from GeneralTaskRunnable
    // TODO javadoc

    public boolean addElement(Object o) {
        throw new IllegalStateException("Not supported");
    }

    public boolean removeElement(Object o) {
        throw new IllegalStateException("Not supported");
    }

    public boolean isEmpty() {
        throw new IllegalStateException("Not supported");
    }

    public long getRunPeriod() {
        return RUNPERIOD;
    }

    public void run() {
        String classMethod = "JMQTokenRepository.run: ";
        try {

            if (debug.messageEnabled()) {
                debug.message(classMethod + "Cleaning expired tokens");
            }

            // TODO synchronize??
            /*
             * Clean up is done based on the cleanUpPeriod even though the
             * thread runs based on the runPeriod.
             */
            if (CLEANUPVALUE <= 0) {
                deleteExpired();
                CLEANUPVALUE = CLEANUPPERIOD;
            }
            CLEANUPVALUE = CLEANUPVALUE - RUNPERIOD;

            /*
             * HealthChecking is done based on the runPeriod but only when
             * the Database is down.
             */
            if (!isDatabaseUp) {
                initPersistSession();
                //logDBStatus();
            }
        } catch (Exception e) {
            debug.error("JMQTokenRepo: Exception during run.", e);
        }
    }
}
