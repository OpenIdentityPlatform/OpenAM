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
 * $Id: AuthContext.java,v 1.10 2009/01/28 05:34:52 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;

import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.LDAPDN;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SecureRandomManager;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.server.AuthSPrincipal;
import com.sun.identity.authentication.internal.util.AuthI18n;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.ServiceManager;


/**
 * The AuthContext provides the implementation for authenticating users using
 * the JAAS technology. It complements <code>LoginContext
 * </code> provided by
 * JAAS by supporting organization environments that cannot handle sessions, for
 * example, HTTP/HTML.
 * <p>
 * A typical caller instantiates this class and starts the login process. The
 * caller then obtains an array of <code>Callback</code> objects, which
 * contains the information required by the authentication plug-in module. The
 * caller requests information from the user. On receiving the information from
 * the user, the caller submits the same to this class. If more information is
 * required, the above process continues until all the information required by
 * the plug-ins has been supplied. The caller then checks if the user has
 * successfully been authenticated. If successfully authenticated, the caller
 * can then get the <code>
 * Subject</code> for the user; if not successfully
 * authenticated, the caller obtains the LoginException.
 *
 * @supported.api
 */
public final class AuthContext extends Object {

    /**
     * This login status indicates that the login process
     * has not started yet. Basically, it means that the method
     * <code>startLogin</code> has not been called.
     *
     * @supported.api
     */
    public static final int AUTH_NOT_STARTED = 1;

    /**
     * This login status indicates that the login process
     * is in progress. Basically, it means that the <code>startLogin</code>
     * method has been called and that this object is waiting for the user to
     * send authentication information.
     *
     * @supported.api
     */
    public static final int AUTH_IN_PROGRESS = 2;

    /**
     * This login status indicates that the login process
     * has succeeded.
     *
     * @supported.api
     */
    public static final int AUTH_SUCCESS = 3;

    /**
     * This login status indicates that the login process
     * has failed.
     *
     * @supported.api
     */
    public static final int AUTH_FAILED = 4;

    /**
     * This login status indicates that the user has been
     * successfully logged out.
     *
     * @supported.api
     */
    public static final int AUTH_COMPLETED = 5;

    /*
     * Protected variables used locally
     */
    protected final String authComponentName = "Authentication";

    protected final static String authKeyName = "authContext";

    // Debug class
    protected final static String authDebugName = "amAuthInternal";

    protected static Debug authDebug = Debug.getInstance(authDebugName);

    protected String organizationName = null;

    protected String applicationName = null;

    protected int loginStatus;

    protected LoginException loginException;

    protected Callback[] informationRequired;

    protected Callback[] submittedInformation;

    protected AuthLoginThread loginThread;

    protected LoginContext loginContext;

    protected SSOToken token;

    protected static I18n myAuthI18n = AuthI18n.authI18n;

    private static boolean isEnableHostLookUp = Boolean.valueOf(
            SystemProperties.get(Constants.ENABLE_HOST_LOOKUP)).booleanValue();

    //
    // overall, AuthContext is a "conduit" between the application and the
    // login module. the Principal implementation must be agreed upon at
    // those two endpoints; AuthContext just passes the Subject that contains
    // the Principal(s).
    //

    /**
     * Constructor to get an instance of
     * <code>AuthContext</code>. Caller would then use
     * <code>getRequirements()</code> and <code>submitRequirements()</code>
     * to pass the credentials needed for authentication by the plugin modules.
     * 
     * @throws LoginException
     *
     * @supported.api
     */
    public AuthContext() throws LoginException {
        // initialize
        this("");
    }

    /**
     * Constructor to get an authenticated instance
     * of this class given the <code>java.security.Principal</code> the user
     * would like to be authenticated as, and the <code>password</code> for
     * the user.
     * 
     * @param principal
     *            name of the user to be authenticated
     * @param password
     *            password for the user
     * @throws LoginException
     *
     * @supported.api
     */
    public AuthContext(Principal principal, char[] password)
            throws LoginException {
        this(null, principal, password);
    }

    /*
     * Constructor for DPro to provide hostname and port for LDAP
     * authentication.
     */
    public AuthContext(Principal principal, char[] password, String hostname,
            int port) throws LoginException {
        this(LoginContext.LDAP_AUTH_URL + hostname + ":" + port, principal,
                password);
    }

    /**
     * Constructor to get an instance of this class
     * given the organization name <code>orgName</code> the user would like to
     * access, the <code>java.security.Principal
     * </code>the user would like to
     * be authenticated as, and the <code>password</code> for the user.
     * 
     * @param orgName
     *            name of the user's organization
     * @param principal
     *            name of the user to be authenticated
     * @param password
     *            password for the user
     * @throws LoginException
     *
     * @supported.api
     */
    public AuthContext(String orgName, Principal principal, char[] password)
            throws LoginException {
        // Make sure principal and password are not null
        if (principal == null)
            throw (new LoginException(myAuthI18n
                    .getString("com.iplanet.auth.invalid-username")));
        if (password == null)
            throw (new LoginException(myAuthI18n
                    .getString("com.iplanet.auth.invalid-password")));

        AuthSubject subject = new AuthSubject();
        
        if (orgName != null)
            organizationName = orgName;
        reset(subject);

        // Set the username and password in LoginContext's sharedState
        loginContext.updateSharedState(principal.getName(), password);

        boolean gotName = false;
        boolean gotPassword = false;
        Callback[] callbacks;

        if (authDebug.messageEnabled()) {
            authDebug.message("Instantiated AuthContext with parameters "
                    + "organization name: "
                    + organizationName
                    + "; "
                    + ((principal == null) ? "principal is null"
                            : "principal: ")
                    + principal
                    + "; "
                    + ((password.length == 0) ? "password is empty\n"
                            : "password present\n"));
        }
        this.startLogin();

        //
        // assume that there are requirements, and they are NameCallback and
        // PasswordCallback. then submit those.
        //
        while (this.hasMoreRequirements()) {
            authDebug.message("AuthContext::init() Has requirements");
            callbacks = this.getRequirements();
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    authDebug.message("Got NameCallback");
                    NameCallback nc = (NameCallback) callbacks[i];
                    Set sops = subject.getPrincipals();
                    AuthSPrincipal[] aps = (AuthSPrincipal[]) sops
                            .toArray(new AuthSPrincipal[0]);
                    if (aps.length == 1) {
                        nc.setName(aps[0].getName());
                        authDebug.message("Set namecallback name = "
                                + aps[0].getName());
                        gotName = true;
                    }
                } else if (callbacks[i] instanceof PasswordCallback) {
                    authDebug.message("Got PasswordCallback");
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    pc.setPassword(password);
                    gotPassword = true;
                } else if (callbacks[i] instanceof TextOutputCallback) {
                    authDebug.message(
                            "AuthContext::init() Got TextOutputCallback");
                } else if (callbacks[i] instanceof TextInputCallback) {
                    authDebug.message(
                            "AuthContext::init() Got TextInputCallback");
                } else if (callbacks[i] instanceof ChoiceCallback) {
                    authDebug.message("AuthContext::init() Got ChoiceCallback");
                    ChoiceCallback cc = (ChoiceCallback) callbacks[i];
                    cc.setSelectedIndex(0);
                } else {
                    authDebug.message(
                            "AuthContext::init() Got Unknown Callback");
                }

            }
            this.submitRequiredInformation(callbacks);
        }

        // Debug messages
        if (authDebug.messageEnabled() && gotName && gotPassword) {
            authDebug.message(
                    "AuthContext::init() Got name and password callbacks");
        }
        if (authDebug.messageEnabled()) {
            authDebug.message("AuthContext::init() Login status: "
                    + this.getLoginStatus());
        }

        // Check login status
        if (getLoginStatus() == AUTH_FAILED) {
            throw (getLoginException());
        }
    }

    /**
     * Constructor to get an instance of this class given the organization name
     * <code>orgName</code> the user would like to access, and the principal's
     * <code>subject</code> the user would like to be authenticated as.
     */
    protected AuthContext(String orgName, AuthSubject subject)
            throws LoginException {
        String orgname = orgName;

        if (authDebug.messageEnabled()) {
            authDebug.message("Instantiating AuthContext with parameters "
                    + "organization name: "
                    + orgName
                    + "; "
                    + ((subject == null) ? "subject is null" : "subject: "
                            + subject));
        }

        if (orgName != null) {
            if (orgName.startsWith("auth://")) {
                int i2, offset;
                String subsample;
                String appName = null;

                offset = 7; // char count of "auth://"
                subsample = orgName.substring(offset);
                // the org + appname, supposedly
                i2 = subsample.indexOf("/");
                if (i2 != -1) {
                    //
                    // from offset to i2 should be the orgName
                    //
                    orgname = subsample.substring(0, i2);
                    authDebug.message("AuthContext::init() auth:// "
                            + "form, orgname = " + orgname);

                    //
                    // get past the "/" after the orgName; look for appName
                    //
                    subsample = subsample.substring(i2 + 1);
                    if (subsample.length() > 0) {
                        //
                        // the next check could be for a "?", this is for
                        // possible
                        // future use where parameters such as
                        // "?userid=<userid>&password=<pswd>" could be passed
                        //
                        i2 = subsample.indexOf("?");
                        if (i2 != -1) {
                            //
                            // parameters specified; pick off appName first
                            //
                            appName = subsample.substring(0, i2);

                            //
                            // the rest assumes the userid and password
                            // parameters as
                            // described above. To be implmented
                            //
                            // subsample = subsample.substring(i2+1);
                        } else {
                            //
                            // Only appName was provided, no user name and
                            // password
                            //
                            appName = subsample;
                        }
                    } else {
                        //
                        // no appName, just OrgName and "/" at the end
                        //
                        appName = null;
                    }
                } else {
                    //
                    // means just the orgName was specified
                    //
                    orgname = subsample;
                }
                if (appName != null) {
                    applicationName = appName;
                }
            } else if (orgName.startsWith("local://")) {
                authDebug.message("local form AuthContext specified; "
                        + orgName);
                int offset = 8; // char count of "local://"
                orgname = orgName.substring(offset); // just the org,
                                                        // hopefully
            }
        }

        this.organizationName = orgname;
        reset(subject);
    }

    // An alternate form of the <code>orgName</code> is
    // "auth://<orgName>/<appName>"
    //
    // note that a private form of orgName is
    // "local://...". this is for administrative-type
    // configuration information for install commands,
    // for example.
    //
    /**
     * Constructor to get an instance of this class
     * given the organization name <code>orgName</code>. The plug-in modules
     * would then query for the user name and related information.
     * 
     * @param orgName organization name.
     * @throws LoginException
     *
     * @supported.api
     */
    public AuthContext(String orgName) throws LoginException {
        this(orgName, null);
        authDebug.message("Instantiated AuthContext with organization name: "
                + orgName);
    }

    /**
     * Constructor to re-create a limited instance of this class given the
     * ByteArray, which was originally obtained using the
     * <code>toByteArray()</code> method. Using this constructor, the only
     * methods that will provide valid return values are
     * <code>getSubject()</code>, <code>getLoginStatus()</code>,
     * <code>getAuthPrincipal()</code>, and <code>getAuthPrincipals()</code>.
     */
    protected AuthContext(byte[] bArray) throws LoginException {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bArray);
            ObjectInputStream bin = new ObjectInputStream(bis);

            String readOrgName = (String) bin.readObject();
            int readStatus = bin.readInt();
            AuthSubject readSubject = (AuthSubject) bin.readObject();

            this.organizationName = readOrgName;
            reset(readSubject);
            setLoginStatus(readStatus); // change status from starting
        } catch (IOException e) {
            authDebug.message("AuthContext::bArray constructor():IOException"
                    + e);
            throw (new LoginException(e.getMessage()));
        } catch (ClassNotFoundException e) {
            authDebug.message(
                    "AuthContext::bArray constructor():ClassNotFoundException"
                            + e);
            throw (new LoginException(e.getMessage()));
        }
    }

    /**
     * Method to reset this instance of <code>AuthContext</code> object, so
     * that a new login process can be initiated. Authenticates the user to the
     * same organization or resource this object was instantiated with. If this
     * object was instantiated with a <code>
     * Subject</code>, it will be
     * ignored.
     */
    protected void reset() throws LoginException {
        authDebug.message("AuthContext::reset()");
        reset(null);
        authDebug.message("AuthContext::reset() exiting");
    }

    /**
     * Method to reset this instance of <code>AuthContext</code> object, so
     * that a new login process can be initiated for the given
     * <code>Subject</code>. Authenticates the user to the same organization
     * or resource this object was instantiated with.
     */
    protected void reset(AuthSubject subject) throws LoginException {

        if (authDebug.messageEnabled()) {
            authDebug.message("AuthContext::reset(" + organizationName + ", "
                    + ((subject == null) ? "null" : subject.toString()) + ")");
        }

        loginStatus = AUTH_NOT_STARTED;
        informationRequired = null;
        submittedInformation = null;
        loginException = null;
        loginThread = new AuthLoginThread(this);
        authDebug.message("AuthLoginThread isAlive = " + loginThread.isAlive());

        String contextName = null;

        if (applicationName == null) {
            contextName = organizationName;
        } else {
            contextName = organizationName + "%" + applicationName;
        }

        authDebug
                .message("AuthContext::reset:using contextName=" + contextName);
        if (subject == null) {
            loginContext = new LoginContext(contextName, loginThread);
        } else {
            loginContext = new LoginContext(contextName, subject, loginThread);
        }

        if (authDebug.messageEnabled()) {
            authDebug
                    .message("Successfully reset AuthContext for organization: "
                            + organizationName
                            + ((subject == null) ? " with no subject name "
                                    : " with subjects: " + subject));
        }
    }

    /**
     * Returns the set of Principals the user has been authenticated as. This
     * can be invoked only after successful authentication. If the
     * authentication fails, this will return <code>null</code>.
     */
    protected AuthSubject getSubject() {
        authDebug.message("AuthContext::getSubject()");
        return (loginContext.getSubject());
    }

    /**
     * Method to start the login process. This method will
     * read the plug-ins configured for the application and initialize them.
     * 
     * @throws LoginException
     *
     * @supported.api
     */
    public void startLogin() throws LoginException {

        authDebug.message("AuthContext::startLogin() called");

        // Make sure we are the current state
        if (getLoginStatus() != AUTH_NOT_STARTED) {

            authDebug.message("AuthContext::startLogin called "
                    + "when the current login state is" + getLoginStatus());

            throw (new LoginException(myAuthI18n
                    .getString("authError-invalidMethod" + getLoginStatus())));
        }

        // Change the login status
        loginStatus = AUTH_IN_PROGRESS;

        // Initiate the login
        authDebug.message("AuthContext::startLogin() "
                + "starting a new thread to run the login process");

        try {
            loginThread.start();
        } catch (Exception ex) {
            authDebug.message("exception starting thread: " + ex);
            throw (new LoginException(ex.getMessage()));
        }
    }

    /**
     * Returns true if the login process requires more
     * information from the user to complete the authentication.
     * 
     * @return true if the login process requires more information from the user
     *         to complete the authentication.
     *
     * @supported.api
     */
    public boolean hasMoreRequirements() {
        authDebug.message("AuthContext::requiresMoreInformation()");

        if (getRequirements() == null)
            return (false);
        else
            return (true);
    }

    /**
     * Returns an array of <code>Callback</code> objects
     * that must be populated by the user and returned back. These objects are
     * requested by the authentication plug-ins, and these are usually displayed
     * to the user. The user then provides the requested information for it to
     * be authenticated.
     * 
     * @return an array of <code>Callback</code> objects that must be
     *         populated by the user and returned back.
     *
     * @supported.api
     */
    public Callback[] getRequirements() {
        authDebug.message("AuthContext::getInformationRequired()");

        // Check the status of LOGIN
        if (getLoginStatus() != AUTH_IN_PROGRESS) {

            authDebug.message("AuthContext:getInformationRequired() "
                    + "called when the current login state is: "
                    + getLoginStatus());

            // Login has completed, could be either success or failure
            return (null);
        }

        // Check if information required is present
        while ((informationRequired == null)
                && (getLoginStatus() == AUTH_IN_PROGRESS)) {
            // wait for required information to be available
            try {
                authDebug.message("AuthContext::getInformationRequired"
                        + "() waiting for Callback array");

                synchronized (loginThread) {
                    if ((informationRequired == null)
                            && (getLoginStatus() == AUTH_IN_PROGRESS)) {
                        loginThread.wait();
                    }
                }

                authDebug.message("AuthContext::getInformationRequired"
                        + "() returned from waiting for Callback array");

            } catch (InterruptedException ie) {
                // do nothing
            }
        }
        return (informationRequired);
    }

    /**
     * Submits the populated <code>Callback</code>
     * objects to the authentication plug-in modules. Called after
     * <code>getInformationRequired</code> method and obtaining user's
     * response to these requests.
     * 
     * @param info
     *            array of <code>Callback</code> objects.
     *
     * @supported.api
     */
    public void submitRequiredInformation(Callback[] info) {
        authDebug.message("AuthContext::submitRequestedInformation()");

        informationRequired = null;

        // Set the submitted info & wake up the callback hander thread
        synchronized (loginThread) {
            submittedInformation = info;
            loginThread.notify();
        }
        authDebug.message("AuthContext::submitRequestedInformation"
                + "() sending notify to sleeping threads");
    }

    /**
     * Logs the user out.
     * 
     * @throws LoginException
     *
     * @supported.api
     */
    public void logout() throws LoginException {
        authDebug.message("AuthContext::logout()");
        loginContext.logout();

        authDebug.message("Called LoginContext::logout()");
        loginStatus = AUTH_COMPLETED;
    }

    /**
     * Returns login exception, if any, during the
     * authentication process. Typically set when the login fails.
     * 
     * @return login exception.
     *
     * @supported.api
     */
    public LoginException getLoginException() {
        authDebug.message("AuthContext::getLoginException()");
        return (loginException);
    }

    /**
     * Returns the current state of the login process.
     * Possible states are listed above.
     * 
     * @return the current state of the login process.
     *
     * @supported.api
     */
    public int getLoginStatus() {
        authDebug.message("AuthContext::getLoginStatus()");
        return (loginStatus);
    }

    /**
     * Method to set the login status. Used internally and not visible outside
     * this package.
     */
    protected void setLoginStatus(int status) {
        authDebug.message("AuthContext::setLoginStatus()");
        loginStatus = status;
    }

    /**
     * Returns the (first) <code>AuthPrincipal</code> in
     * the <code>Subject</code>. Returns the first <code>Principal</code>,
     * if more than one exists.
     * 
     * @return the (first) <code>AuthPrincipal</code> in the
     *         <code>Subject</code>.
     *
     * @supported.api
     */
    public Principal getPrincipal() {
        Set sop = getSubject().getPrincipals();
        if (authDebug.messageEnabled()) {
            authDebug.message("AuthContext::getAuthPrincipal(): " + sop);
        }
        Iterator items = sop.iterator();
        if (items.hasNext()) {
            return ((Principal) items.next());
        } else {
            return null;
        }
    }

    /**
     * Method to get the (first) <code>AuthPrincipal</code> in the
     * <code>Subject</code>. Returns the first <code>Principal</code>, if
     * more than one exists.
     * 
     * @deprecated Use getPrincipal() instead
     */
    public AuthPrincipal getAuthPrincipal() {
        authDebug.message("AuthContext::getAuthPrincipal()");

        Set sop = getSubject().getPrincipals();
        Iterator items = sop.iterator();
        if (items.hasNext())
            return ((AuthPrincipal) items.next());
        else
            return null;
    }

    /**
     * Method to get the set of <code>AuthPrincipal</code>s in the
     * <code>Subject</code>.
     */
    protected Set getPrincipals() {
        authDebug.message("AuthContext::getAuthPrincipals()");
        return (getSubject().getPrincipals());
    }

    /**
     * Method to retrieve a Byte array of serializable portions of the
     * <code>AuthContext</code>.
     */
    protected byte[] toByteArray() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream bout = new ObjectOutputStream(bos);

            bout.writeObject(((organizationName == null) ? " "
                    : organizationName));
            bout.writeInt(loginStatus);
            bout.writeObject(loginContext.getSubject());
            byte[] bytestuff = bos.toByteArray();
            return (bytestuff);
        } catch (IOException iox) {
            if (authDebug.messageEnabled()) {
                authDebug.message("AuthContext:toByteArray():IOException", iox);
            }
        } catch (Exception e) {
            if (authDebug.messageEnabled()) {
                authDebug.message("AuthContext:toByteArray():Exception", e);
            }
        }
        return null;
    }

    /**
     * Method to get organization name that was set during
     * construction of this instance.
     * 
     * @return organization name; <code>null</code> if it was not initialized
     *         during construction of this instance
     *
     * @supported.api
     */
    public String getOrganizationName() {
        if (organizationName == null) {
            String rootSuffix = organizationName = ServiceManager.getBaseDN();
            if ((rootSuffix != null) && (organizationName != null)) {
                rootSuffix = new DN(rootSuffix).toRFCString().toLowerCase();
                organizationName = new DN(organizationName).toRFCString()
                        .toLowerCase();
            }
        }
        return organizationName;
    }

    protected String getApplicationName() {
        return applicationName;
    }

    /**
     * Method to get the Single-Sign-On (SSO) Token. This
     * token can be used as the authenticated token.
     * 
     * @return single-sign-on token.
     * @throws InvalidAuthContextException
     *
     * @supported.api
     */
    public SSOToken getSSOToken() throws InvalidAuthContextException {
        if (token != null) {
            return (token);
        }

        token = new AuthSSOToken(this);
        try {
            // Set Organization
            if (getOrganizationName() != null) {
                token.setProperty(Constants.ORGANIZATION,
                    getOrganizationName());
            }

            // Set Host name
            InetAddress address = InetAddress.getLocalHost();
            String ipAddress = address.getHostAddress();
            String strHostName = address.getHostName();

            if (authDebug.messageEnabled()) {
                authDebug.message("Complete Host : " + address.toString());
                authDebug.message("getSSOToken : HOST Name : " + strHostName);
                authDebug.message("getSSOToken : IP : " + ipAddress);
            }

            if (ipAddress != null) {
                if (isEnableHostLookUp) {
                    if (strHostName != null) {
                        token.setProperty("HostName", strHostName);
                    }
                } else {
                    token.setProperty("HostName", ipAddress);
                }
                token.setProperty("Host", ipAddress);
            }

            // Set AuthType
            token.setProperty("AuthType", "ldap");

            // Set Principal
             String principal = getPrincipal().getName();
             if (principal != null) {
                 token.setProperty("Principal", principal);
                 // Set Universal Identifier
                 String username = principal;
                 if (DN.isDN(principal)) {
                     // Get the username
                     username = LDAPDN.explodeDN(principal, true)[0];
                 }
                 // Since internal auth will be used during install time
                 // and during boot strap for users "dsame" and "amadmin"
                 // the IdType will be hardcoded to User
                 StringBuilder uuid = new StringBuilder(100);
                 uuid.append("id=").append(username)
                 .append(",ou=user,").append(getOrganizationName());
                 token.setProperty(Constants.UNIVERSAL_IDENTIFIER,
                     uuid.toString());
             }

            // Set AuthLevel
            token.setProperty("AuthLevel", Integer.toString(0));

            //Set ContextId 
            SecureRandom secureRandom = 
                SecureRandomManager.getSecureRandom();
            String amCtxId = 
                Long.toHexString(secureRandom.nextLong());
            token.setProperty(Constants.AM_CTX_ID, amCtxId);

            if (authDebug.messageEnabled()) {
                authDebug.message("SSOToken : Organization : "
                        + token.getProperty("Organization"));
                authDebug.message("SSOToken : Principal : "
                        + token.getProperty("Principal"));
                authDebug.message("SSOToken : HostName : "
                        + token.getProperty("HostName"));
                authDebug.message("SSOToken : Host : "
                        + token.getProperty("Host"));
                authDebug.message("SSOToken : getIPAddress : "
                        + token.getIPAddress());
                authDebug.message("SSOToken : getHostName : "
                        + token.getHostName());
                authDebug.message("SSOToken : ContextId : "
                        + token.getProperty(Constants.AM_CTX_ID));
            }

        } catch (Exception e) {
            if (authDebug.warningEnabled()) {
                authDebug.warning("getSSOToken: setProperty exception : ", e);
            }
        }

        return (token);
    }
}
