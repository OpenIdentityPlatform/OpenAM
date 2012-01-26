/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AMAuthCallBackImpl.java,v 1.3 2008/06/25 05:42:06 qcheng Exp $
 *
 */



package com.sun.identity.authentication.spi;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class should be instantiated by components in the authentication
 * framework when callbacks to an external application's logic are required.
 * When an account is being locked or a password is being changed,
 * then some plug-in classes can be called.
 * Those plug-in classes are defined within the core authentication service
 * of each organization or realm.
 * The <code>AMAuthCallBackImpl</code> class provides 2 convenient methods
 * that correspond to the use cases mentioned above : account lockout and
 * password change . These methods
 * should be called when one of the events has happened. Each method calls
 * the private utility method, <code>processedEvent</code>, and takes care
 * of associating the appropriate event type.
 * Instances of this class should be called via the <code>getInstance</code>
 * method. It returns a singleton associated with the corresponding
 * realm or organization. The class holds a map of instantiated objects. Each
 * object holds a map of instantiated plug-in classes for a particular
 * realm or organization. These plug-in classes are accessed via the
 * <code>AMAuthCallBack</code> plug-in class, which serves as a base for the
 * custom business logic.
 *
 * Sample call :
 * <pre>
 *      callbackImpl = AMAuthCallBackImpl.getInstance( realm );
 *      callbackImpl.processedAccounttLockout( now, userDN );
 * </pre>
 *
 * @see AMAuthCallBackException
 * @see AMAuthCallBack
 */
public class AMAuthCallBackImpl {
    
    // The name of the service attribute where the classes are defined.
    static final String AUTH_PLUGIN_MODULES_ATTR =
            "sunAMUserStatusCallbackPlugins";
    
    // The name of the debug file...
    static final String DEBUG_FILE = "amAuth";
    
    // ... and the bundle.
    static final String bundleName = DEBUG_FILE;
    
    /**
     * The debug instance for this singleton. We're just reusing the one from
     * the core authentication service.
     */
    private static Debug debug = Debug.getInstance(DEBUG_FILE);
    
    /**
     * The private static field of this callback implementation. We
     * use a map of singletons in order to avoid re-creating the callback
     * objects everytime a login context is created; we use a map of
     * <realm,AMAuthCallBackImpl> since each realm can have
     * a separate set of plugin classes. We'll go thru the map of
     * callback implementations to invoke the plug-in classes.
     */
    private static Map theCallBackInst = new HashMap();
    
    /**
     * The list of callback class names for each instance of this class; when a 
     * callback is performed, the call is made to all the plug-in classes whose
     * names are present in this Set.
     */
    private Set pluginSet = Collections.EMPTY_SET;
    
    /**
     * The realm for which we created the callback object. This is
     * just a useful, private, class member which prevents us from having
     * to pass the realm in all the method calls.
     */
    private String theRealm = null;
    
    /**
     * This private constructor is used to initialize an instance of
     * an AMAuthCallBackImpl object for a particular realm. This
     * constructor is called if the callback plug-in class Set for the
     * realm in question hasn't been instantiated yet.
     * @param aRealm name of realm 
     * @exception AMAuthCallBackException if authentication fails.
     */
    private AMAuthCallBackImpl(String aRealm) throws AMAuthCallBackException {
        // We populate the private variable for the realm so we
        // have it available when methods need to reference it.
        this.theRealm = aRealm;
        SSOToken token = null;
        // Debug statement
        if (debug.messageEnabled()){
            debug.message("AMAuthCallBackImpl : in constructor." +
                    " Realm = " + theRealm);
        }
        try {
            // We retrieve the SSO token which will be used to
            // get the core authentication module's service information.
            token = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
        } catch (NullPointerException npe){
            if (debug.errorEnabled()) {
                debug.error("AMAuthCallBackImpl : constructor " +
                        "cannot get SSO Token. No callbacks will be made " +
                        "for the realm : " + theRealm, npe);
            }
        }
        // We need to get the list of plug-in class names for the
        // realm.
        pluginSet = getRealmAuthPlugIns(token, AUTH_PLUGIN_MODULES_ATTR);
        // If the plugin Set is empty, no authentication callback will be
        // performed since no plug-in classes have been provided.
    }
    
    /**
     * Public accessor for the singleton. Returns the instance (singleton)
     * of the callback implementation, for the appropriate realm.
     * @param aRealm the name of the realm for which to get the callbacks
     * @return AMAuthCallBackImpl the singleton instance.
     * @exception AMAuthCallBackException if there was an initialization error
     * during the construction of the singleton.
     */
    public static final AMAuthCallBackImpl getInstance(String aRealm)
            throws AMAuthCallBackException {
        if (debug.messageEnabled()) {
            debug.message("AMAuthCallBackImpl : getting instance.");
        }
        synchronized (theCallBackInst) {
            if (theCallBackInst.get(aRealm) == null) {
                theCallBackInst.put(aRealm, new AMAuthCallBackImpl(aRealm));
            }
        }
        return (AMAuthCallBackImpl) theCallBackInst.get(aRealm);
    }
    
    /**
     * Calls the plug-in class(es) when a password change has been processed
     * by an authentication module.
     * @param eventTime the time when the event occurred
     * @param userDN the user's DN for which the event occurred
     * @exception AMAuthCallBackException the exception raised by the plug-in
     */
    public void processedPasswordChange( Long eventTime, String userDN )
            throws AMAuthCallBackException {
        // We construct the map of parameters based on this type of event.
        Map eventParams = new HashMap();
        eventParams.put(AMAuthCallBack.TIME_KEY, eventTime);
        eventParams.put(AMAuthCallBack.REALM_KEY, theRealm);
        eventParams.put(AMAuthCallBack.USER_KEY, userDN);
        // Calls the general-purpose method with the event type
        // set to PASSWORD_CHANGE.
        processedEvent(AMAuthCallBack.PASSWORD_CHANGE, eventParams);
    }
    
    /**
     * Calls the plug-in class(es) when an account lockout has been processed
     * by an authentication module.
     * @param eventTime the time when the event occurred
     * @param userDN the user's DN for which the event occurred
     * @exception AMAuthCallBackException the exception raised by the plug-in
     */
    public void processedAccounttLockout( Long eventTime, String userDN)
            throws AMAuthCallBackException {
        // We construct the map of parameters based on this type of event.
        Map eventParams = new HashMap();
        eventParams.put(AMAuthCallBack.TIME_KEY, eventTime);
        eventParams.put(AMAuthCallBack.REALM_KEY, theRealm);
        eventParams.put(AMAuthCallBack.USER_KEY, userDN);
        // Calls the general-purpose method with the event type
        // set to ACCOUNT_LOCKOUT.
        processedEvent(AMAuthCallBack.ACCOUNT_LOCKOUT, eventParams);
    }
    
    /**
     * Calls the plug-in class(es) when an event has been processed
     * by an authentication module. This method is a general method
     * to be used by the more event-specific methods. There are currently
     * 2 main event types : password change and account lockout.
     * @param eventType the type of event being processed
     * @param eventParams the map of parameters for the current event
     * @exception AMAuthCallBackException the exception raised by the plug-in
     */
    public void processedEvent( int eventType, Map eventParams )
            throws AMAuthCallBackException {
        // If we do have some plugin classes then we process them
        if (pluginSet != Collections.EMPTY_SET) {
            Iterator itr = pluginSet.iterator();
            while (itr.hasNext()) {
                // Instantiating the plug-in class...
                AMAuthCallBack pluginClass = instantiateClass(
                        (String) itr.next()) ;
                // and then calling the callback method if the object
                // exists; the event type and the map of parameters are being
                // passed back for further processing by the custom plug-in.
                if (pluginClass != null)
                pluginClass.authEventCallback(eventType, eventParams);
            }
        }
        // The pluginSet is empty and therefore no callback will be made
        if (pluginSet.isEmpty() && debug.messageEnabled()) {
            debug.message("AMAuthCallBackImpl : processedEvent. " +
                    "pluginSet is empty");
        }
    }
    
    /**
     * Returns the plug-in classes defined in the core auth service by the
     * attribute <code>iplanet-am-auth-callback-plugins</code>, and returns
     * all these plug-in class names as a Set.
     *
     * @param internalToken the single sign on token used for privileged
     *        operations.
     * @param attrName the attribute name used to locate the class names in the
     *        realm service template.
     * @return Set of class names which will be used to instantiate
     *         plug-in callback objects, or an empty Set if no service
     *         configuration was found or an exception occurred.
     */
    private Set getRealmAuthPlugIns(SSOToken internalToken,
            String attrName) {
        Set resultSet = Collections.EMPTY_SET;
        try {
            // We retrieve the service config manager in order to get
            // to the plug-in classes.
            ServiceConfigManager scm = new ServiceConfigManager(
                    ISAuthConstants.AUTH_SERVICE_NAME, internalToken);
            // We check if there's a service configuration defined for
            // the realm (theRealm)...
            ServiceConfig sc = scm.getOrganizationConfig(theRealm, null);
            // ... in which case we get the values associated with the
            // plug-in class attribute.
            if (sc != null) {
                Map attributes = sc.getAttributes();
                resultSet = (Set) attributes.get(AUTH_PLUGIN_MODULES_ATTR);
            } else {
                // If there isn't any service configuration defined for the
                // organization then we default to the service's global
                // attributes. This implies that the business logic will
                // default to those classes defined in the global service.
                ServiceConfig gc = scm.getGlobalConfig(null);
                if (gc != null) {
                    Map attributes = gc.getAttributes();
                    resultSet = (Set) attributes.get(AUTH_PLUGIN_MODULES_ATTR);
                }
                // If there's no organization or global config for the
                // service (weird case) then we default to an empty
                // Set -- ie no plug-in classes defined.
            }
        } catch (SMSException smse) {
            if (debug.errorEnabled()) {
                debug.error("AMAuthCallBackImpl getRealmAuthPlugIns : " +
                        "SMS error", smse);
            }
        } catch (SSOException ssoe) {
            if (debug.errorEnabled()) {
                debug.error("AMAuthCallBackImpl getRealmAuthPlugIns : " +
                        "SSO error", ssoe);
            }
        } finally {
            return resultSet;
        }
    }
    
    /**
     * Instantiates a class for the callback map
     * based on the class name provided.
     * @param className the name of the class to be instantiated for the
     * callback map
     * @return AMAuthCallBack a callback object instantiated based on the class
     * name
     */
    private AMAuthCallBack instantiateClass(String className) {
        try {
            if (debug.messageEnabled()) {
                debug.message("AMAuthCallBackImpl : instantiateClass. " +
                        "Class name is : " + className);
            }
            return ((AMAuthCallBack) Class.forName(className).newInstance());
        } catch (ClassNotFoundException cnfe) {
            debug.error("AuthCallBackImpl.instantiateClass(): Unable to " +
                    "locate class " + className, cnfe);
        } catch (InstantiationException ie) {
            debug.error("AuthCallBackImpl.instantiateClass(): Unable to " +
                    "instantiate class " + className, ie);
        } catch (IllegalAccessException iae) {
            debug.error("AuthCallBackImpl.instantiateClass(): Problem " +
                    "with the Security Manager for class " + className, iae);
        } catch (Exception e) {
            debug.error("AMCallBackImpl.instantiateClass(): Unknown " +
                    "error for class " + className, e);
        }
        // We return null if we ever get to here (class instantiation failed).
        return null;
    }
}
