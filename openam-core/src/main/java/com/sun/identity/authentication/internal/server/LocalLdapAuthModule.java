/*
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
 * $Id: LocalLdapAuthModule.java,v 1.7 2009/01/28 05:34:52 ww203982 Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.authentication.internal.server;

import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.Server;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.ums.Guid;
import com.iplanet.ums.IUMSConstants;
import com.iplanet.ums.TemplateManager;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.authentication.internal.AuthSubject;
import com.sun.identity.authentication.internal.LoginContext;
import com.sun.identity.authentication.internal.LoginModule;
import com.sun.identity.authentication.internal.util.AuthI18n;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Set;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SSLContextBuilder;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Options;

public class LocalLdapAuthModule implements LoginModule {

    /* Naming attribute for users */
    private final static String UIDATTR = "uid";

    private final static String TEMPLATE_NAME = "BasicUser";

    private static Debug debug = Debug.getInstance("amAuthInternalLDAPModule");

    private boolean readServerConfiguration = false;

    private String baseDN = null;

    private Connection conn = null;

    private CallbackHandler cbHandler;

    private AuthSubject subject;

    private Map sharedState;

    private Map options;

    private String userDN;

    public LocalLdapAuthModule() {
        // do nothing
    }

    public void initialize(AuthSubject subject, CallbackHandler handle,
            Map sharedState, Map options) {
        this.subject = subject;
        cbHandler = handle;
        this.sharedState = sharedState;
        this.options = options;
    }

    public boolean login() throws LoginException {
        // Check if we have username and password, else get it
        String uid = (String) sharedState
                .get(ISAuthConstants.SHARED_STATE_USERNAME);
        String strPasswd = (String) sharedState
                .get(ISAuthConstants.SHARED_STATE_PASSWORD);
        if (debug.messageEnabled()) {
            debug.message("LocalLdapAuthModule::login() From shared state: "
                    + "Username: " + uid + " Password: "
                    + ((strPasswd == null) ? "<not present>" : "<present>"));
        }

        // Check if we have username and password, if not send callbacks
        if (uid == null || strPasswd == null) {
            // Request for both username and password
            Callback cbs[] = new Callback[2];
            cbs[0] = new NameCallback("User name: ");
            cbs[1] = new PasswordCallback("Password: ", false);
            try {
                if (debug.messageEnabled()) {
                    debug.message("LocalLdapAuthModule::login() Sending "
                            + "Name & Password Callback");
                }
                cbHandler.handle(cbs);
            } catch (UnsupportedCallbackException e) {
                throw (new LoginException(e.getMessage()));
            } catch (IOException ioe) {
                throw (new LoginException(ioe.getMessage()));
            }
            uid = ((NameCallback) cbs[0]).getName();
            char[] passwd = ((PasswordCallback) cbs[1]).getPassword();
            if (passwd != null) {
                strPasswd = new String(passwd);
            }
        }

        // Authenticate
        boolean authentication = false;
        userDN = getDN(uid);
        if (strPasswd != null && strPasswd.length() != 0) {
            if (authenticate(userDN, strPasswd)) {
                authentication = true;
            }
        }
        return (authentication);
    }

    public boolean abort() throws LoginException {
        return (true);
    }

    public boolean commit() throws LoginException {
        // Add the DN to the Subject
        Set principals = subject.getPrincipals();
        principals.add(new AuthPrincipal(userDN));
        return (true);
    }

    public boolean logout() throws LoginException {
        return (true);
    }

    private boolean authenticate(String dn, String passwd)
            throws LoginException {
        // LDAP connection used for authentication
        Connection localConn = null;

        String host;
        int port;
        Options ldapOptions = Options.defaultOptions();

        // Check if organization is present in options
        String orgUrl = (String) options.get(LoginContext.ORGNAME);
        if ((orgUrl == null)
                || (orgUrl.equals(LoginContext.LDAP_AUTH_URL))
                || (orgUrl.equals(LoginContext.LDAPS_AUTH_URL))
                || !(orgUrl.startsWith(LoginContext.LDAP_AUTH_URL) || orgUrl
                        .startsWith(LoginContext.LDAPS_AUTH_URL))) {
            try {
                DSConfigMgr dscm = DSConfigMgr.getDSConfigMgr();
                // We need a handle on server instance so we can know the
                // Connection type. If it is SSL, the connection needs to be
                // accordingly created. Note: The user type does not make
                // a difference, as the connection type is Server group based,
                // so passing any user type for the second argument.
                ServerInstance si = dscm.getServerInstance(DSConfigMgr.DEFAULT,
                        LDAPUser.Type.AUTH_BASIC);
                String hostName = dscm.getHostName(DSConfigMgr.DEFAULT);
                if (si.getConnectionType() == Server.Type.CONN_SSL) {
                    try {
                        ldapOptions.set(LDAPConnectionFactory.SSL_CONTEXT, new SSLContextBuilder().getSSLContext());
                    } catch (GeneralSecurityException e) {
                        debug.error("getConnection.JSSESocketFactory", e);
                        throw new LDAPServiceException(AuthI18n.authI18n
                                .getString(IUMSConstants.DSCFG_JSSSFFAIL));
                    }
                }
                if (dn != null && passwd != null) {
                    // The 389 port number passed is overridden by the
                    // hostName:port
                    // constructed by the getHostName method. So, this is not
                    // a hardcoded port number.
                    host = hostName;
                    port = 389;
                } else {
                    // Throw LoginException
                    throw new LoginException(AuthI18n.authI18n
                            .getString(IUMSConstants.DSCFG_CONNECTFAIL));
                }
            } catch (LDAPServiceException ex) {
                debug.error("Authenticate failed: " + ex);
                throw new LoginException(ex.getMessage());
            }
        } else {
            try {
                if (debug.messageEnabled()) {
                    debug.message("authenticate(): orgUrl= " + orgUrl);
                }
                // Get hostname
                int start;
                boolean useSSL = false;
                if (orgUrl.startsWith(LoginContext.LDAPS_AUTH_URL)) {
                    start = LoginContext.LDAPS_AUTH_URL.length();
                    useSSL = true;
                } else {
                    start = LoginContext.LDAP_AUTH_URL.length();
                }
                int end = orgUrl.indexOf(':', start);
                if (end == -1) {
                    end = orgUrl.indexOf('/', start);
                    if (end == -1)
                        end = orgUrl.length();
                }
                String hostName = orgUrl.substring(start, end);

                // Get port number
                String portNumber = "389";
                start = end + 1;
                if (start < orgUrl.length()) {
                    end = orgUrl.indexOf('/', start);
                    if (end == -1)
                        end = orgUrl.length();
                    portNumber = orgUrl.substring(start, end);
                }
                if (useSSL) {
                    try {
                        ldapOptions.set(LDAPConnectionFactory.SSL_CONTEXT, new SSLContextBuilder().getSSLContext());
                    } catch (GeneralSecurityException e) {
                        debug.error("authentication().JSSESocketFactory()", e);
                        throw (new LoginException(e.getMessage()));
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("before connect(), hostName=" + hostName
                            + ",port=" + portNumber);
                }
                host = hostName;
                port = Integer.parseInt(portNumber);
            } catch (Exception e) {
                debug.error("authentication", e);
                throw (new LoginException(e.getMessage()));
            }
        }


        try (ConnectionFactory factory = LDAPUtils.createFailoverConnectionFactory(host, port, dn, passwd, ldapOptions);
             Connection conn = factory.getConnection()) {
            return true;
        } catch (LdapException e) {
            throw new LoginException(e.getMessage());
        }
    }

    private String getDN(String uid) throws LoginException {
        String retVal = "";
        if (uid == null) {
            throw (new LoginException(AuthI18n.authI18n
                    .getString("com.iplanet.auth.invalid-username")));
        }

        if (LDAPUtils.isDN(uid)) {
            return uid;
        }

        String namingAttribute = UIDATTR;
        try {
            String orgName = (String) options.get(LoginContext.ORGNAME);
            if ((orgName != null) && !LDAPUtils.isDN(orgName)) {
                // Use orgname only if it a DN, else baseDN
                orgName = baseDN;
            }
            if (com.sun.identity.sm.ServiceManager.isAMSDKConfigured()) {
                namingAttribute = TemplateManager.getTemplateManager()
                    .getCreationTemplate(TEMPLATE_NAME,
                            (orgName == null) ? null : new Guid(orgName))
                    .getNamingAttribute();
            }
        } catch (Exception e) {
            // Ignore the exception and use the default naming attribute
        }

        StringBuilder filter = new StringBuilder();
        filter.append('(').append(namingAttribute).append('=').append(uid)
                .append(')');
        String[] attrs = { "noAttr" };
        ConnectionEntryReader results = null;
        try {
            // Read the serverconfig.xml for LDAP information
            if (!readServerConfiguration) {
                readServerConfig();
            }
            if (conn == null) {
                debug.warning(
                        "LocalLdapAuthModule.getDN(): lda connection is null");
                throw (new LoginException("INVALID_USER_NAME"));
            } else {
                results = conn.search(LDAPRequests.newSearchRequest(baseDN, SearchScope.WHOLE_SUBTREE,
                        filter.toString(), attrs));
            }

            if (results.hasNext()) {
                SearchResultEntry entry = results.readEntry();
                retVal = entry.getName().toString();
            }

            if (retVal == null || retVal.equals("")) {
                throw new LoginException("INVALID_USER_NAME");
            }
            return retVal;

        } catch (LdapException | SearchResultReferenceIOException ex) {
            throw new LoginException(ex.getMessage());
        } finally {
            IOUtils.closeIfNotNull(conn);
            conn = null;
        }
    }

    private void readServerConfig() throws LoginException {
        if (readServerConfiguration)
            return;

        try {
            DSConfigMgr cfgMgr = DSConfigMgr.getDSConfigMgr();
            conn = cfgMgr.getNewBasicConnectionFactory().getConnection();
            ServerInstance si = cfgMgr.getServerInstance(DSConfigMgr.DEFAULT, LDAPUser.Type.AUTH_BASIC);
            baseDN = si.getBaseDN();
            readServerConfiguration = true;
        } catch (LDAPServiceException | LdapException ex) {
            throw new LoginException(ex.getMessage());
        }
    }
}
