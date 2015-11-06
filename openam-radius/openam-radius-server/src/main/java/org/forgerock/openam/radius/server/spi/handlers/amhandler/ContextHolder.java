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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
/*
 * Portions Copyright 2015 ForgeRock AS
 */
package org.forgerock.openam.radius.server.spi.handlers.amhandler;

import javax.security.auth.callback.Callback;

import org.forgerock.openam.radius.server.config.RadiusServerConstants;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessageImpl;

/**
 * Holds server side info for an authentication conversation in progress for a user via a radius client.
 * <p/>
 */
public class ContextHolder {

    /**
     * Debug logger.
     */
    private static final Debug LOG = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * The page properties callback class that encapsulates properties related to the callback set including some
     * attributes of the Callbacks element such as timeout, header, template, image, and error.
     */
    private PagePropertiesCallback callbackSetProps = null;

    /**
     * Indicates what phase of authentication we are in. The flow is the same as the order of declaration.
     */
    public static enum AuthPhase {
        /**
         * The auth phase is starting.
         */
        STARTING,

        /**
         * The auth phase is that in which the user must add input.
         */
        GATHERING_INPUT,

        /**
         * The auth phase is in its final stage.
         */
        FINALIZING,

        /**
         * The auth phase has terminated.
         */
        TERMINATED
    }

    /**
     * The name of the module instance.
     */
    private String moduleName = null;

    /**
     * The zero based index of the current module in the chain for whom we are gather values from the user. Initialized
     * to -1 so that we can centralize updating this info set without regard to whether we are handling the first set or
     * following sets.
     */
    private int chainModuleIndex = -1;

    /**
     * The current set of callbacks being fulfilled by a user through radius.
     */
    private Callback[] callbacks = null;

    /**
     * The zero based index of the current callback (field) whose requirement is being sought through a RADIUS
     * accessChallenge response excluding the undeclared PagePropertiesCallback object that is always first in the array
     * of callbacks and contains the header for the html page in which the fields for this set of callbacks are
     * presented for html clients.
     */
    private int idxOfCurrentCallback = 0;

    /**
     * The zero based index of the set of callbacks within a given module. Modules can have more than one set of
     * callback with a single set translating to a single web page when openam is used for web authentication.
     */
    private int idxOfCallbackSetInModule = 0;

    /**
     * The context object being held in cache.
     */
    private AuthContext authContext;

    /**
     * The millis value of the timeout value for the current callback set. We persist this here so that we still have
     * access to it while processing each callback requiring input from the user. Then for each callback input value
     * received we reset the count since we then know that the user is still with us. So ultimately, when using RADIUS
     * the total time that a user may have entering input values for all callbacks in a set is the number of callbacks
     * times the timeout value for that set. At creation time we instantiate to one minute so that the holder won't get
     * purged from cache between getting created and loading of the first callback set.
     */
    private Long millisExpiryForCurrentCallbacks = 60000L;

    /**
     * The time in the future when this context should be purged from cache ostensibly because the authentication
     * attempt was aborted by that user or they took too long to complete a given step. When System.currentTimeMillis is
     * greater than this value the expiration point has passed and the item should be purged. This value may change
     * multiple times during a given authentication process depending on how many pages of callbacks are incurred. Each
     * set of callbacks has its own declared number of seconds allows for response and that value will be set here when
     * that callback set is incurred.
     */
    private Long millisExpiryPoint = System.currentTimeMillis() + millisExpiryForCurrentCallbacks;

    /**
     * The key for this object in the server-side cache.
     */
    private final String cacheKey;

    /**
     * Indicates in which phase of authentication we are at any point in time.
     */
    private AuthPhase authPhase = AuthPhase.STARTING;

    /**
     * Constructs a ContextHolder with its unique key.
     *
     * @param key the unique cache key for the context holder
     */
    public ContextHolder(String key) {
        this.cacheKey = key;
    }

    /**
     * get the callback set properties.
     *
     * @return the callback properties for the current module.
     */
    public PagePropertiesCallback getCallbackSetProps() {
        return this.callbackSetProps;
    }

    /**
     * Sets the callback properties for the module currently being processed.
     *
     * @param callbackSetProps - a <code>PagePropertiesCallback</code> reference.
     */
    public void setCallbackSetProps(PagePropertiesCallback callbackSetProps) {
        this.callbackSetProps = callbackSetProps;
    }

    /**
     * Gets the name of the module.
     *
     * @return the name of the module currently being processed, or null if no module is being processed.
     */
    public String getModuleName() {
        return this.moduleName;
    }

    /**
     * Sets the name of the module currently being processed.
     *
     * @param moduleName - a non null string containing the name of the module.
     */
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Increments the chain module index to indicate that the next module in the chain is currently being processed.
     */
    public void incrementChainModuleIndex() {
        ++chainModuleIndex;
    }

    /**
     * Get the chain module index.
     *
     * @return The zero based index of the current module in the chain for whom we are gathering values from the user.
     * -1 indicates that there is no module set. This is so that we can centralize updating this info set without regard
     * to whether we are handling the first set or following sets.
     */
    public int getChainModuleIndex() {
        return this.chainModuleIndex;
    }

    /**
     * Gets the array of callback objects for the chain.
     *
     * @return an array of <code>Callback</code> objects.
     */
    public Callback[] getCallbacks() {
        return this.callbacks;
    }

    /**
     * Sets the array of <code>Callback</code> objects for the chain.
     *
     * @param callbacks an array of <code>Callback</code> objects.
     */
    public void setCallbacks(Callback[] callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * Get the index of the current callback.
     *
     * @return the index of the current callback.
     */
    public int getIdxOfCurrentCallback() {
        return this.idxOfCurrentCallback;
    }

    /**
     * set the current callback index.
     *
     * @param idxOfCurrentCallback The index of the current callback.
     */
    public void setIdxOfCurrentCallback(int idxOfCurrentCallback) {
        this.idxOfCurrentCallback = idxOfCurrentCallback;
    }

    /**
     * Increment the current callback index.
     */
    public void incrementIdxOfCurrentCallback() {
        ++idxOfCurrentCallback;
    }

    /**
     * gets the index of the callback set in the module.
     *
     * @return the index of the callback set in the module.
     */
    public int getIdxOfCallbackSetInModule() {
        return this.idxOfCallbackSetInModule;
    }

    /**
     * Set the index of the callback set in the module.
     *
     * @param idxOfCallbackSetInModule the index of the callback set in the module.
     */
    public void setIdxOfCallbackSetInModule(int idxOfCallbackSetInModule) {
        this.idxOfCallbackSetInModule = idxOfCallbackSetInModule;
    }

    /**
     * Increments the index of the callback set in the module.
     */
    public void incrementIdxOfCallbackSetInModule() {
        ++this.idxOfCallbackSetInModule;
    }

    /**
     * get the auth context.
     *
     * @return a <code>AuthContext</code> object.
     */
    public AuthContext getAuthContext() {
        return this.authContext;
    }

    /**
     * set the auth context.
     *
     * @param authContext a <code>AuthContext</code> object
     */
    public void setAuthContext(AuthContext authContext) {
        this.authContext = authContext;
    }

    /**
     * get the number of milliseconds before the current callback expires.
     *
     * @return a <code>Long</code> representing the number of milliseconds until the current callback expires.
     */
    public Long getMillisExpiryForCurrentCallbacks() {
        return this.millisExpiryForCurrentCallbacks;
    }

    /**
     * Sets the period in milliseconds until the current callback expire.
     *
     * @param millisExpiryForCurrentCallbacks a <code>Long</code> representing the number of milliseconds until the
     *                                        current callback expires.
     */
    public void setMillisExpiryForCurrentCallbacks(Long millisExpiryForCurrentCallbacks) {
        this.millisExpiryForCurrentCallbacks = millisExpiryForCurrentCallbacks;
    }

    /**
     * gets the point at which the current context holder expires.
     *
     * @return the difference, measured in milliseconds, between the context holder's expiry time and midnight, January
     * 1, 1970
     */
    public Long getMillisExpiryPoint() {
        return this.millisExpiryPoint;
    }

    /**
     * Sets the point at which the current context holder expires.
     *
     * @param millisExpiryPoint the difference, measured in milliseconds, between the context holder's expiry time and
     *                          midnight, January 1, 1970
     */
    public void setMillisExpiryPoint(Long millisExpiryPoint) {
        this.millisExpiryPoint = millisExpiryPoint;
    }

    /**
     * get the cache key of this context holder.
     *
     * @return a <code>String</code> object containing the key for the context holder.
     */
    public String getCacheKey() {
        return this.cacheKey;
    }

    /**
     * get the auth phase of the context holder.
     *
     * @return an <code>AuthPhase</code> object.
     */
    public AuthPhase getAuthPhase() {
        return this.authPhase;
    }

    /**
     * Set the auth phase of the context holder.
     *
     * @param authPhase an <code>AuthPhase</code> object.
     */
    public void setAuthPhase(AuthPhase authPhase) {
        this.authPhase = authPhase;
    }

    /**
     * Obtain the universal ID from the SSO token held by the auth context. If there is no SSO token available or there
     * is no universal id available then null will be returned.
     *
     * @return the universalId, or null if none is available.
     */
    public String getUniversalId() {
        LOG.message("Entering ContextHolder.getUniversalId()");
        String universalId = null;
        try {
            SSOToken token = this.authContext.getSSOToken();
            if (token != null) {
                universalId = token.getProperty(Constants.UNIVERSAL_IDENTIFIER);
            } else {
                LOG.message("No SSO token available from the auth context.");
            }
        } catch (L10NMessageImpl e) {
            LOG.warning("Could not get universal ID from the SSOToken.", e);
        }
        LOG.message("Leaving ContextHolder.getUniversalId()");
        return universalId;
    }
}
