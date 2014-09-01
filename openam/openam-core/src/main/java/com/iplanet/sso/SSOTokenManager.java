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
 * $Id: SSOTokenManager.java,v 1.7 2009/02/18 23:59:36 qcheng Exp $
 *
 */

package com.iplanet.sso;

import com.iplanet.services.util.I18n;
import com.iplanet.sso.providers.dpro.SSOProviderBundle;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.shared.debug.Debug;
import java.security.Principal;
import java.util.Set;

/**
 * SSOTokenManager is the final class that is the mediator between the SSO APIs
 * and SSO providers. When an SSO client makes an API invocation,
 * SSOTokenManager will delegate that call to the SSO provider/plug-in. The SSO
 * provider will execute the call and return the results to SSOTokenManager,
 * which in turn returns the results to the SSO client. This decouples the SSO
 * clients from the actual SSO providers. You should be able to replace the SSO
 * provider without having to modify the SSO client. However, the clients can
 * invoke the class methods on the objects returned by the SSOTokenManager. 
 * <p>
 * SSOTokenManager is a singleton class; there can be, at most, only one
 * instance of SSOTokenManager in any given JVM. <p> SSOTokenManager currently
 * supports only two kinds of provider, one for Grappa and another for Sun
 * OpenSSO. In the future, this will be extended to support 
 * <p> It is assumed that the provider classes or the JAR file is in the 
 * CLASSPATH so that they can be found automatically. Providers can be 
 * configured using <code>providerimplclass</code> property. 
 * This property must be set to the complete (absolute) package name of the 
 * main class of the provider. For example, if the provider class is
 * com.iplanet.sso.providers.dpro.SSOProviderImpl, that entire class name
 * including package prefixes MUST be specified. The main class MUST implement
 * the com.iplanet.sso.SSOProvider interface and MUST have a public no-arg
 * default constructor.
 * <p>
 * The class <code>SSOTokenManager</code> is a <code>final</code> class that
 * provides interfaces to create and validate <code>SSOToken</code>s.
 * <p>
 * It is a singleton class; an instance of this class can be obtained by calling
 * <code>SSOTokenManager.getInstance()</code>.
 * <p>
 * Having obtained an instance of <code>SSOTokenManager</code>, its methods
 * can be called to create <code>SSOToken</code>, get <code>SSOToken</code>
 * given the <code>SSOTokenID</code> in string format, and to validate
 * <code>SSOToken</code>s.
 *
 * @supported.api
 */
public class SSOTokenManager {
    
    /*
     * SSOTokenManager is not a real provider but implements SSOProvider for
     * consistency in the methods.
     */

    /**
     * Grappa SSOProvider class that will be used by default if
     * providerimplclass property is not present.
     */
    static final String GRAPPA_PROVIDER_PACKAGE = 
        "com.sun.identity.authentication.internal";

    static SSOProvider grappaProvider = null;

    /**
     * DPRO SSOProvider class
     */
    static SSOProvider dProProvider = null;

    /** Debug class that can be used by SSOProvider implementations */
    public static Debug debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);

    // Singleton instance of SSOTokenManager
    private static SSOTokenManager instance = null;

    /**
     * Returns the singleton instance of
     * <code>SSOTokenManager</code>.
     * 
     * @return The singleton <code>SSOTokenManager</code> instance
     * @throws SSOException
     *             if unable to get the singleton <code>SSOTokenManager</code>
     *             instance.
     * @supported.api
     */
    public static SSOTokenManager getInstance() throws SSOException {

        /*
         * We will use the double-checked locking pattern. Rarely entered block.
         * Push synchronization inside it. This is the first check.
         */

        if (instance == null) {
            /*
             * Only 1 thread at a time gets past the next point. Rarely executed
             * synchronization statement and hence synchronization penalty is
             * not paid every time this method is called.
             */

            synchronized (SSOTokenManager.class) {
                /*
                 * If a second thread was waiting to get here, it will now find
                 * that the instance has already been initialized, and it will
                 * not re-initialize the instance variable. This is the
                 * double-check.
                 */

                if (instance == null) {
                    /*
                     * Here is the critical section that lazy initializes the
                     * singleton variable.
                     */
                    debug.message(
                            "Constructing a new instance of SSOTokenManager");
                    instance = new SSOTokenManager();
                }
            }
        }
        return (instance);
    }

    /**
     * Since this class is a singleton, the constructor is suppressed. This
     * constructor will try to find the provider jar files, load them, then find
     * the provider mainclass, instantiate it and store it in provider.
     * Providers can be configured using <code>providerimplclass</code> Java
     * property. This property must be set to the complete (absolute) package
     * name of the main class of the provider. The main class MUST implement the
     * com.iplanet.sso.SSOProvider interface and MUST have a public no-arg
     * default constructor.
     */
    private SSOTokenManager() throws SSOException {
        Throwable dProException = null;
        // Obtain the Grappa provider class
        try {
            grappaProvider = new 
                com.sun.identity.authentication.internal.AuthSSOProvider();
            if (debug.messageEnabled()) {
                debug.message("Obtained Grappa SSO Provider");
            }
        } catch (Throwable e) {
            debug.error("Unable to obtain Grappa SSO provider", e);
            dProException = e;
        }

        // Obtain the DPRO provide class
        try {
            dProProvider = new com.iplanet.sso.providers.dpro.SSOProviderImpl();
            if (debug.messageEnabled()) {
                debug.message("Obtained DPRO SSO Provider");
            }
        } catch (Throwable e) {
            debug.error("DPRO SSO Provider Exception", e);
            dProException = e;
        }

        if (dProProvider == null && grappaProvider == null) {
            debug.error("Unable to obtain either GRAPPA or DPRO SSO providers");
            I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);
            String rbName = i18n.getResBundleName();
            if (dProException instanceof ClassNotFoundException)
                throw new SSOException(rbName,
                        IUMSConstants.SSO_NOPROVIDERCLASS, null);
            else if (dProException instanceof InstantiationException)
                throw new SSOException(rbName,
                        IUMSConstants.SSO_NOPROVIDERINSTANCE, null);
            else if (dProException instanceof IllegalAccessException)
                throw new SSOException(rbName, IUMSConstants.SSO_ILLEGALACCESS,
                        null);
            else
                throw new SSOException(dProException);
        }
    }

    /**
     * Get provider based on SSOToken provided
     * @param token Single signon SSOToken
     * @exception SSOException in case of erros when getting the provider
     */
    protected static SSOProvider getProvider(SSOToken token)
            throws SSOException {
        if (token == null) {
            throw new SSOException(SSOProviderBundle.rbName, "ssotokennull",
                    null);
        }
        String packageName = token.getClass().getName();
        if (packageName.startsWith(GRAPPA_PROVIDER_PACKAGE)) {
            if (grappaProvider == null) {
                I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);
                throw new SSOException(i18n.getResBundleName(),
                        IUMSConstants.SSO_NOPROVIDERCLASS, null);
            }
            return (grappaProvider);
        }
        return (dProProvider);
    }

    /**
     * Creates a single sign on token from <code>HttpServletRequest</code>
     * 
     * @param request
     *            The <code>HttpServletRequest</code> object which contains
     *            the session string.
     * @return single sign on <code>SSOToken</code>
     * @exception SSOException
     *                if the single sign on token cannot be created.
     * @exception UnsupportedOperationException
     *                if this is an unsupported operation.
     * @supported.api
     */
    public SSOToken createSSOToken(
            javax.servlet.http.HttpServletRequest request)
            throws UnsupportedOperationException, SSOException {
        if (dProProvider != null)
            return (dProProvider.createSSOToken(request));
        else
            return (grappaProvider.createSSOToken(request));
    }

    /**
     * Creates a single sign on token after authenticating
     * the principal with the given password. This method of creating a single
     * sign on token should only be used for command line applications and it is
     * forbidden to use this single sign on token in any other context (e.g.
     * policy, federation, etc.). A token created with this method is only valid
     * within the context of the calling application. Once the process exits the
     * token will be destroyed. If token is created using this constructor then
     * ONLY these methods of single sign on token is supported -
     * 
     * <pre>
     *  getAuthType(), 
     *  getHostName(), 
     *  getIPAddress(), 
     *  setProperty(String name, String value), 
     *  getProperty(String name), 
     *  isValid(), 
     *  validate(). 
     * </pre>
     * 
     * @param user
     *            Principal representing a user or service
     * @param password
     *            The password supplied for the principal
     * @return single sign on token
     * @exception SSOException
     *                if the single sign on token cannot be created.
     * @exception UnsupportedOperationException
     *                if this is an unsupported operation.
     * @deprecated This method has been deprecated. Please use the regular LDAP
     *             authentication mechanism instead. More information on how to
     *             use the authentication programming interfaces as well as the
     *             code samples can be obtained from the "Authentication
     *             Service" chapter of the OpenSSO Developer's Guide.
     */
    public SSOToken createSSOToken(Principal user, String password)
            throws UnsupportedOperationException, SSOException {
        if (dProProvider != null)
            return (dProProvider.createSSOToken(user, password));
        else
            return (grappaProvider.createSSOToken(user, password));
    }

    /**
     * Creates a single sign on token from the single sign
     * on token ID. Note:-If you want to do Client's IP address validation for
     * the single sign on token then use
     * <code>creatSSOToken(String, String)</code> OR
     * <code>createSSOToken(HttpServletRequest)</code>.
     * 
     * @param tokenId
     *            Token ID of the single sign on token
     * @return single sign on token
     * @exception SSOException
     *                if the single sign on token cannot be created.
     * @exception UnsupportedOperationException
     * @supported.api
     */
    public SSOToken createSSOToken(String tokenId)
            throws UnsupportedOperationException, SSOException {
        if (dProProvider != null)
            return (dProProvider.createSSOToken(tokenId));
        else
            return (grappaProvider.createSSOToken(tokenId));
    }

    /**
     * Creates a single sign on token from the single sign
     * on token ID.
     * 
     * @param tokenId
     *            Token ID of the single sign on token
     * @param clientIP
     *            Client IP address. This must be the IP address of the
     *            client/user who is accessing the application.
     * @return single sign on token
     * @exception SSOException
     *                if the single sign on token cannot be created.
     * @exception UnsupportedOperationException
     * @supported.api
     */
    public SSOToken createSSOToken(String tokenId, String clientIP)
            throws UnsupportedOperationException, SSOException {
        if (dProProvider != null)
            return (dProProvider.createSSOToken(tokenId, clientIP));
        else
            return (grappaProvider.createSSOToken(tokenId, clientIP));
    }

    /**
     * Returns true if a single sign on token is valid.
     * 
     * @param token
     *            The single sign on token object to be validated.
     * @return true if the single sign on token is valid.
     * @supported.api
     */
    public boolean isValidToken(SSOToken token) {
        try {
            return (getProvider(token).isValidToken(token));
        } catch (SSOException ssoe) {
            return (false);
        }
    }

    /**
     * Returns true if the single sign on token is valid.
     * 
     * @param token
     *            The single sign on token object to be validated.
     * @exception SSOException
     *                if the single sign on token is not valid.
     * @supported.api
     */
    public void validateToken(SSOToken token) throws SSOException {
        getProvider(token).validateToken(token);
    }

    /**
     * Destroys a single sign on token.
     * 
     * @param token
     *            The single sign on token object to be destroyed.
     * @exception SSOException
     *                if there was an error while destroying the token, or the
     *                corresponding session reached its maximum session/idle
     *                time, or the session was destroyed.
     * @supported.api
     */
    public void destroyToken(SSOToken token) throws SSOException {
        getProvider(token).destroyToken(token);
    }

    /**
     * Refresh the Session corresponding to the single
     * sign on token from the Session Server. This method should only be used
     * when the client cannot wait the "session cache interval" for updates on
     * any changes made to the session properties in the session server. If the
     * client is remote, calling this method results in an over the wire request
     * to the session server.
     * 
     * @param token
     *            single sign on token
     * @exception SSOException
     *                if the session reached its maximum session time, or the
     *                session was destroyed, or there was an error while
     *                refreshing the session.
     * @supported.api
     */
    public void refreshSession(SSOToken token) throws SSOException {
        try {
            getProvider(token).refreshSession(token);
        } catch (Exception e) {
            debug.error("Error in refreshing the session from session server");
            throw new SSOException(e);
        }
    }

    /**
     * Destroys a single sign on token.
     * 
     * @param destroyer
     *            The single sign on token object used to authorize the
     *            operation
     * @param destroyed
     *            The single sign on token object to be destroyed.
     * @throws SSOException
     *             if the there was an error during communication with session
     *             service.
     * @supported.api
     */
    public void destroyToken(SSOToken destroyer, SSOToken destroyed)
            throws SSOException {
        getProvider(destroyer).destroyToken(destroyer, destroyed);
    }

    /**
     * Returns a list of single sign on token objects
     * which correspond to valid Sessions accessible to requester. Single sign
     * on tokens returned are restricted: they can only be used to retrieve
     * properties and destroy sessions they represent.
     * 
     * @param requester
     *            The single sign on token object used to authorize the
     *            operation
     * @param server
     *            The server for which the valid sessions are to be retrieved
     * @return Set The set of single sign on tokens representing valid Sessions.
     * @throws SSOException
     *             if the there was an error during communication with session
     *             service.
     * @supported.api
     */
    public Set getValidSessions(SSOToken requester, String server)
            throws SSOException {
        return getProvider(requester).getValidSessions(requester, server);
    }
}
