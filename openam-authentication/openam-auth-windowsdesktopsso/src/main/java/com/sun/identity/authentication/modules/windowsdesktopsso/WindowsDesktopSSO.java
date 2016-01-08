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
 * $Id: WindowsDesktopSSO.java,v 1.7 2009/07/28 19:40:45 beomsuk Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.authentication.modules.windowsdesktopsso;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.util.DerValue;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.servlet.http.HttpServletRequest;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;

public class WindowsDesktopSSO extends AMLoginModule {
    private static final String amAuthWindowsDesktopSSO = 
        "amAuthWindowsDesktopSSO";

    private static final String[] configAttributes = {
        "iplanet-am-auth-windowsdesktopsso-principal-name",
        "iplanet-am-auth-windowsdesktopsso-keytab-file",
        "iplanet-am-auth-windowsdesktopsso-kerberos-realm",
        "iplanet-am-auth-windowsdesktopsso-kdc",
        "iplanet-am-auth-windowsdesktopsso-returnRealm",
        "iplanet-am-auth-windowsdesktopsso-lookupUserInRealm",
        "iplanet-am-auth-windowsdesktopsso-auth-level",
        "serviceSubject" };

    private static final int PRINCIPAL = 0;
    private static final int KEYTAB    = 1;
    private static final int REALM     = 2;
    private static final int KDC       = 3;
    private static final int RETURNREALM = 4;
    private static final int LOOKUPUSER = 5;
    private static final int AUTHLEVEL = 6;
    private static final int SUBJECT   = 7;
    
    private static final String ACCEPTED_REALMS_ATTR = ISAuthConstants.AUTH_ATTR_PREFIX 
            + "windowsdesktopsso-kerberos-realms-trusted";
        
    private static Hashtable configTable = new Hashtable();
    private Principal userPrincipal = null;
    private Subject serviceSubject = null;
    private String servicePrincipalName = null;
    private String keyTabFile = null;
    private String kdcRealm   = null;
    private String kdcServer  = null;
    private boolean returnRealm = false;
    private String authLevel  = null;
    private Map    options    = null;
    private String confIndex  = null;
    private boolean lookupUserInRealm = false;
    
    private Debug debug = Debug.getInstance(amAuthWindowsDesktopSSO);
    
    private Set<String> trustedKerberosRealms = Collections.EMPTY_SET;
    
    private static final String REALM_SEPARATOR = "@";

    /**
     * Constructor
     */
    public WindowsDesktopSSO() {
    }

    /**
     * Initialize parameters. 
     *
     * @param subject
     * @param sharedState
     * @param options
     */
    public void init(Subject subject, Map sharedState, Map options) {
        this.options = options;
    }

    /**
     * Returns principal of the authenticated user.
     *
     * @return Principal of the authenticated user.
     */
    public Principal getPrincipal() {
        return userPrincipal;
    }

    /**
     * Processes the authentication request.
     *
     * @param callbacks
     * @param state
     * @return  -1 as succeeded; 0 as failed.
     * @exception AuthLoginException upon any failure.
     */
    public int process(Callback[] callbacks, int state) 
            throws AuthLoginException {
        int result = ISAuthConstants.LOGIN_IGNORE;

        // Check to see if the Rest Auth Endpoint has signified that IWA has failed.
        HttpServletRequest request = getHttpServletRequest();
        if (request != null && hasWDSSOFailed(request)) {
            return ISAuthConstants.LOGIN_IGNORE;
        }

        if ( !getConfigParams() ) {
            initWindowsDesktopSSOAuth(options);
        }

        // retrieve the spnego token
        byte[] spnegoToken = getSPNEGOTokenFromHTTPRequest(request);
        if (spnegoToken == null) {
            spnegoToken = getSPNEGOTokenFromCallback(callbacks);
        }

        if (spnegoToken == null) {
            debug.error("spnego token is not valid.");
            throw new AuthLoginException(amAuthWindowsDesktopSSO, "token",null);
        }

        if (debug.messageEnabled()) {
            debug.message("SPNEGO token: \n" + 
                DerValue.printByteArray(spnegoToken, 0, spnegoToken.length));
        }
        // parse the spnego token and extract the kerberos mech token from it
        final byte[] kerberosToken = parseToken(spnegoToken);
        if (kerberosToken == null) {
            debug.error("kerberos token is not valid.");
            throw new AuthLoginException(amAuthWindowsDesktopSSO, "token",null);
        }
        if (debug.messageEnabled()) {
            debug.message("Kerberos token retrieved from SPNEGO token: \n" + 
                DerValue.printByteArray(kerberosToken,0,kerberosToken.length));
        }

        // authenticate the user with the kerberos token
        try {
            authenticateToken(kerberosToken, trustedKerberosRealms);
            if (debug.messageEnabled()){
                debug.message("WindowsDesktopSSO kerberos authentication passed succesfully.");
            }
            result = ISAuthConstants.LOGIN_SUCCEED; 
         } catch (PrivilegedActionException pe) {
             Exception e = extractException(pe);	 
             if( e instanceof GSSException) {	 
                 int major = ((GSSException)e).getMajor();	 
                 if (major == GSSException.CREDENTIALS_EXPIRED) {	 
                         debug.message("Credential expired. Re-establish credential...");	 
                 serviceLogin();	 
                 try {   
                     authenticateToken(kerberosToken, trustedKerberosRealms);  
                     if (debug.messageEnabled()){
                       debug.message("Authentication succeeded with new cred.");    
                           result = ISAuthConstants.LOGIN_SUCCEED;
                     }
                 } catch (Exception ee) {   
                       debug.error("Authentication failed with new cred.Stack Trace", ee); 
                       throw new AuthLoginException(amAuthWindowsDesktopSSO,    
                                "auth", null, ee);  
                }   
              } else {  
                     debug.error("Authentication failed with PrivilegedActionException wrapped GSSException. Stack Trace", e);  
                     throw new AuthLoginException(amAuthWindowsDesktopSSO, "auth",  
                              null, e);
              }     
            }   
       } catch (GSSException e1 ){
           int major = e1.getMajor();
           if (major == GSSException.CREDENTIALS_EXPIRED) {
               debug.message("Credential expired. Re-establish credential...");
               serviceLogin();
                   try {
                   authenticateToken(kerberosToken, trustedKerberosRealms);
                   if (debug.messageEnabled()){
                       debug.message("Authentication succeeded with new cred.");
                           result = ISAuthConstants.LOGIN_SUCCEED; 
                   }
               } catch (Exception ee) {
                       debug.error("Authentication failed with new cred. Stack Trace", ee);
                       throw new AuthLoginException(amAuthWindowsDesktopSSO, 
                       "auth", null, ee);
               }
           } else {
               debug.error("Authentication failed with GSSException. Stack Trace", e1);
               throw new AuthLoginException(amAuthWindowsDesktopSSO, "auth", 
                   null, e1);
           }
       } catch (AuthLoginException e2) {
           debug.error("Authentication failed with AuthLoginException. Stack Trace", e2);
           throw e2;
       } catch (Exception e3) {
           debug.error("Authentication failed with generic exception. Stack Trace", e3);
           throw new AuthLoginException(amAuthWindowsDesktopSSO, "auth", 
               null, e3);
       }
        return result;
    }

    private void authenticateToken(final byte[] kerberosToken, final Set<String> trustedRealms) 
            throws AuthLoginException, GSSException, Exception {

        debug.message("In authenticationToken ...");
        Subject.doAs(serviceSubject, new PrivilegedExceptionAction(){
            public Object run() throws Exception {
                GSSContext context =
                    GSSManager.getInstance().createContext(
                        (GSSCredential)null);
                if (debug.messageEnabled()){
                    debug.message("Context created.");
                }
                byte[] outToken = context.acceptSecContext(
                    kerberosToken, 0,kerberosToken.length);
                if (outToken != null) {
                    if (debug.messageEnabled()) {
                        debug.message(
                            "Token returned from acceptSecContext: \n"
                            + DerValue.printByteArray(
                                outToken, 0, outToken.length));
                    }
                }
                if (!context.isEstablished()) {
                    debug.error("Cannot establish context !");
                    throw new AuthLoginException(amAuthWindowsDesktopSSO,
                        "context", null);
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("Context established !");
                    }
                    GSSName user = context.getSrcName();
                    final String userPrincipalName = user.toString();

                    // If the whitelist is empty, do not enforce it. This prevents issues with upgrading, and is the
                    // expected default behaviour.
                    if (!trustedRealms.isEmpty()) {
                        boolean foundTrustedRealm = false;
                        for (final String trustedRealm : trustedRealms) {
                            if (isTokenTrusted(userPrincipalName, trustedRealm)) {
                                foundTrustedRealm = true;
                                break;
                            }
                        }
                        if (!foundTrustedRealm) {
                            debug.error("Kerberos token for " + userPrincipalName + " not trusted");
                            final String[] data = {userPrincipalName};
                            throw new AuthLoginException(amAuthWindowsDesktopSSO, "untrustedToken", data);
                        }
                    }
                    
                    // Check if the user account from the Kerberos ticket exists 
                    // in the realm. The "Alias Search Attribute Names" will be used to
                    // perform the search.
                    if (lookupUserInRealm) {
                        String org = getRequestOrg();
                        String userValue = getUserName(userPrincipalName);
                        String userName = searchUserAccount(userValue, org);
                        if (userName != null && !userName.isEmpty()) {
                            storeUsernamePasswd(userValue, null);
                        } else {
                            String data[] = {userValue, org};
                            debug.error("WindowsDesktopSSO.authenticateToken: "
                                    + ": Unable to find the user " + userValue);
                            throw new AuthLoginException(amAuthWindowsDesktopSSO,
                                    "notfound", data);
                        }
                    }
                    
                    if (debug.messageEnabled()){
                        debug.message("WindowsDesktopSSO.authenticateToken:"
                                + "User authenticated: " + user.toString());
                    }
                    if (user != null) {
                        setPrincipal(userPrincipalName);
                    }
                }
                context.dispose();
                return null;
            }
        });
    }

     /**	 
      * Iterate until we extract the real exception	 
      * from PrivilegedActionException(s).	 
      */	 
     private static Exception extractException(Exception e) {	 
         while (e instanceof PrivilegedActionException) {	 
             e = ((PrivilegedActionException)e).getException();	 
         }	 
         return e;	 
     }

    /**
     * TODO-JAVADOC
     */
    public void destroyModuleState() {
        userPrincipal = null;
    }

    /**
     * TODO-JAVADOC
     */
    public void nullifyUsedVars() {
        serviceSubject = null;
        servicePrincipalName = null;
        keyTabFile = null;
        kdcRealm = null;
        kdcServer = null;
        authLevel = null;
        options = null;
        confIndex = null;
        trustedKerberosRealms = Collections.EMPTY_SET;
    }

    private void setPrincipal(String user) {
        userPrincipal = new WindowsDesktopSSOPrincipal(getUserName(user));
    }

    private String getUserName(String user) {
        String userName = user;
        if (!returnRealm) {
            int index = user.indexOf(REALM_SEPARATOR);
            if (index != -1) {
                userName = user.toString().substring(0, index);
            }
        }
        return userName;
    }
    
    private static byte[] spnegoOID = {
            (byte)0x06, (byte)0x06, (byte)0x2b, (byte)0x06, (byte)0x01,
            (byte)0x05, (byte)0x05, (byte)0x02 };

    // defined but not used.
    private static byte[] MS_KERBEROS_OID =  {
            (byte)0x06, (byte)0x09, (byte)0x2a, (byte)0x86, (byte)0x48,
            (byte)0x82, (byte)0xf7, (byte)0x12, (byte)0x01, (byte)0x02,
            (byte)0x02 };
    private static byte[] KERBEROS_V5_OID = {
            (byte)0x06, (byte)0x09, (byte)0x2a, (byte)0x86, (byte)0x48,
            (byte)0x86, (byte)0xf7, (byte)0x12, (byte)0x01, (byte)0x02,
            (byte)0x02 };

    /**
     * Checks the request for an attribute "http-auth-failed".
     *
     * @param request THe HttpServletRequest.
     * @return If the attribute is present and set to true true is returned otherwise false is returned.
     */
    private boolean hasWDSSOFailed(HttpServletRequest request) {
        return Boolean.valueOf((String) request.getAttribute("http-auth-failed"));
    }

    private byte[] getSPNEGOTokenFromHTTPRequest(HttpServletRequest req) {
        byte[] spnegoToken = null;
        if (req != null) {
            String header = req.getHeader("Authorization");
            if ((header != null) && header.startsWith("Negotiate")) {
                header = header.substring("Negotiate".length()).trim();
                try {
                    spnegoToken = Base64.decode(header);
                } catch (Exception e) {
                    debug.error("Decoding token error.");
                    if (debug.messageEnabled()) {
                        debug.message("Stack trace: ", e);
                    }
                }
            }
        }
        return spnegoToken;
    }

    private byte[] getSPNEGOTokenFromCallback(Callback[] callbacks) {
        byte[] spnegoToken = null;
        if (callbacks != null && callbacks.length != 0) {
            String spnegoTokenStr =
                ((HttpCallback)callbacks[0]).getAuthorization();
            try {
                spnegoToken = Base64.decode(spnegoTokenStr);
            } catch (Exception e) {
                debug.error("Decoding token error.");
                if (debug.messageEnabled()) {
                    debug.message("Stack trace: ", e);
                }
            }
        }

        return spnegoToken;
    }

    private byte[] parseToken(byte[] rawToken) {
        byte[] token = rawToken;
        DerValue tmpToken = new DerValue(rawToken);
        if (debug.messageEnabled()) {
            debug.message("token tag:" + DerValue.printByte(tmpToken.getTag()));
        }
        if (tmpToken.getTag() != (byte)0x60) {
            return null;
        }

        ByteArrayInputStream tmpInput = new ByteArrayInputStream(
                tmpToken.getData());

        // check for SPNEGO OID
        byte[] oidArray = new byte[spnegoOID.length];
        tmpInput.read(oidArray, 0, oidArray.length);
        if (Arrays.equals(oidArray, spnegoOID)) {
            if (debug.messageEnabled()) {
                debug.message("SPNEGO OID found in the Auth Token");
            }
            tmpToken = new DerValue(tmpInput);

            // 0xa0 indicates an init token(NegTokenInit); 0xa1 indicates an 
            // response arg token(NegTokenTarg). no arg token is needed for us.

            if (tmpToken.getTag() == (byte)0xa0) {
                if (debug.messageEnabled()) {
                    debug.message("DerValue: found init token");
                }
                tmpToken = new DerValue(tmpToken.getData());
                if (tmpToken.getTag() == (byte)0x30) {
                    if (debug.messageEnabled()) {
                        debug.message("DerValue: 0x30 constructed token found");
                    }
                    tmpInput = new ByteArrayInputStream(tmpToken.getData());
                    tmpToken = new DerValue(tmpInput);

                    // In an init token, it can contain 4 optional arguments:
                    // a0: mechTypes
                    // a1: contextFlags
                    // a2: octect string(with leading char 0x04) for the token
                    // a3: message integrity value

                    while (tmpToken.getTag() != (byte)-1 &&
                           tmpToken.getTag() != (byte)0xa2) {
                        // look for next mech token DER
                        tmpToken = new DerValue(tmpInput);
                    }
                    if (tmpToken.getTag() != (byte)-1) {
                        // retrieve octet string
                        tmpToken = new DerValue(tmpToken.getData());
                        token = tmpToken.getData();
                    }
                }
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("SPNEGO OID not found in the Auth Token");
            }
            byte[] krb5Oid = new byte[KERBEROS_V5_OID.length];
            int i = 0;
            for (; i < oidArray.length; i++) {
                krb5Oid[i] = oidArray[i];
            }
            tmpInput.read(krb5Oid, i, krb5Oid.length - i);
            if (!Arrays.equals(krb5Oid, KERBEROS_V5_OID)) {
                if (debug.messageEnabled()) {
                    debug.message("Kerberos V5 OID not found in the Auth Token");
                }
                token = null;
            } else {
                if (debug.messageEnabled()) {
                    debug.message("Kerberos V5 OID found in the Auth Token");
                }
            }
        }
        return token;
    }

    private boolean getConfigParams() {
        // KDC realm in service principal must be uppercase.
        servicePrincipalName = getMapAttr(options, PRINCIPAL);
        keyTabFile = getMapAttr(options, KEYTAB);
        kdcRealm = getMapAttr(options, REALM);
        kdcServer = getMapAttr(options, KDC);
        authLevel = getMapAttr(options, AUTHLEVEL);
        returnRealm = 
            Boolean.valueOf(getMapAttr(options,RETURNREALM)).booleanValue();
        lookupUserInRealm = 
            Boolean.valueOf(getMapAttr(options,LOOKUPUSER)).booleanValue();
        trustedKerberosRealms = getAcceptedKerberosRealms(options);

        if (debug.messageEnabled()){
            debug.message("WindowsDesktopSSO params: \n" + 
                "principal: " + servicePrincipalName +
                     "\nkeytab file: " + keyTabFile +
                "\nrealm : " + kdcRealm +
                "\nkdc server: " + kdcServer +
                "\ndomain principal: " + returnRealm +
                "\nLookup user in realm:" + lookupUserInRealm +
                "\nAccepted Kerberos realms: " + trustedKerberosRealms +    
                "\nauth level: " + authLevel);
        }

        confIndex = getRequestOrg() + "/" +
            options.get(ISAuthConstants.MODULE_INSTANCE_NAME);
        Map configMap = (Map)configTable.get(confIndex);
        if (configMap == null) {
            return false;
        }
        

        String principalName = 
            (String)configMap.get(configAttributes[PRINCIPAL]);
        String tabFile = (String)configMap.get(configAttributes[KEYTAB]);
        String realm = (String)configMap.get(configAttributes[REALM]);
        String kdc = (String)configMap.get(configAttributes[KDC]);

        if (principalName == null || tabFile == null || 
            realm == null || kdc == null ||
            ! servicePrincipalName.equalsIgnoreCase(principalName) ||
            ! keyTabFile.equals(tabFile) ||
            ! kdcRealm.equals(realm) ||
            ! kdcServer.equalsIgnoreCase(kdc)) {
            return false;
        }

        serviceSubject = (Subject)configMap.get(configAttributes[SUBJECT]);
        if (serviceSubject == null) {
            return false;
        }
        if (debug.messageEnabled()){
            debug.message("Retrieved config params from cache.");
        }
        return true;
    }

    private void initWindowsDesktopSSOAuth(Map options) 
        throws AuthLoginException{
        
        if (debug.messageEnabled()){
            debug.message("Init WindowsDesktopSSO. This should not happen often.");
        }
        verifyAttributes();
        serviceLogin();

        // save the service subject and the other configuration data
        // into configTable for other auth requests in the same org
        Map configMap = (Map)configTable.get(confIndex);
        if (configMap == null) {
            configMap = new HashMap();
        }

        configMap.put(configAttributes[SUBJECT], serviceSubject);
        configMap.put(configAttributes[PRINCIPAL], servicePrincipalName);
        configMap.put(configAttributes[KEYTAB], keyTabFile);
        configMap.put(configAttributes[REALM], kdcRealm);
        configMap.put(configAttributes[KDC], kdcServer);

        configTable.put(confIndex, configMap);
    }

    private synchronized void serviceLogin() throws AuthLoginException{
        if (debug.messageEnabled()){
            debug.message("New Service Login ...");
        }
        System.setProperty("java.security.krb5.realm", kdcRealm);
        System.setProperty("java.security.krb5.kdc", kdcServer); 
        System.setProperty("java.security.auth.login.config", "/dev/null");

        try {
            Configuration config = Configuration.getConfiguration();
            WindowsDesktopSSOConfig wtc = null;
            if (config instanceof WindowsDesktopSSOConfig) {
                wtc = (WindowsDesktopSSOConfig) config;
                wtc.setRefreshConfig("true");
            } else {
                wtc = new WindowsDesktopSSOConfig(config);
            }
            wtc.setPrincipalName(servicePrincipalName);
            wtc.setKeyTab(keyTabFile);
            Configuration.setConfiguration(wtc);

            // perform service authentication using JDK Kerberos module
            LoginContext lc = new LoginContext(
                WindowsDesktopSSOConfig.defaultAppName);
            lc.login();

            serviceSubject = lc.getSubject();
            if (debug.messageEnabled()){
                debug.message("Service login succeeded.");
            }
        } catch (Exception e) {
            debug.error("Service Login Error: ");
            if (debug.messageEnabled()) {
                debug.message("Stack trace: ", e);
            }
            throw new AuthLoginException(amAuthWindowsDesktopSSO, 
                "serviceAuth", null, e);
        }
    }        

    private String getMapAttr(Map options, int index) {
        return CollectionHelper.getMapAttr(options, configAttributes[index]);
    }

    private void verifyAttributes() throws AuthLoginException {
        if (servicePrincipalName == null || servicePrincipalName.length() == 0){
            throw new AuthLoginException(amAuthWindowsDesktopSSO, 
                "nullprincipal", null);
        }
        if (keyTabFile == null || keyTabFile.length() == 0){
            throw new AuthLoginException(amAuthWindowsDesktopSSO, 
                "nullkeytab", null);
        }
        if (kdcRealm == null || kdcRealm.length() == 0){
            throw new AuthLoginException(amAuthWindowsDesktopSSO, 
                "nullrealm", null);
        }
        if (kdcServer == null || kdcServer.length() == 0){
            throw new AuthLoginException(amAuthWindowsDesktopSSO,
                "nullkdc", null);
        }
        if (authLevel == null || authLevel.length() == 0){
            throw new AuthLoginException(amAuthWindowsDesktopSSO, 
                "nullauthlevel", null);
        }

        if (!(new File(keyTabFile)).exists()) {
            // ibm jdk needs to skip "file://" part in parameter
            if (!(new File(keyTabFile.substring(7))).exists()) {
                throw new AuthLoginException(amAuthWindowsDesktopSSO, 
                "nokeytab", null);
            }
        }
        
        try {
            setAuthLevel(Integer.parseInt(authLevel));
        } catch (Exception e) {
            throw new AuthLoginException(amAuthWindowsDesktopSSO, 
                "authlevel", null, e);
        }
    }


     /**
     * Searches for an account with user Id userID in the organization organization
     * @param attributeValue The attributeValue to compare when searching for an
     *  identity in the organization
     * @param organization organization or the organization name where the identity will be
     *  looked up
     * @return the attribute value for the identity searched. Empty string if not found or
     *  null if an error occurs
     */
    private String searchUserAccount(String attributeValue, String organization) 
            throws AuthLoginException {

        String classMethod = "WindowsDesktopSSO.searchUserAccount: ";

        if (organization.isEmpty()) {
            organization = "/";
        }
        
        if (debug.messageEnabled()) {
            debug.message(classMethod + " searching for user " + attributeValue
                    + " in the organization =" + organization);
        }

        // And the search criteria
        IdSearchControl searchControl = new IdSearchControl();
        searchControl.setMaxResults(1);
        searchControl.setTimeOut(3000);

        searchControl.setSearchModifiers(IdSearchOpModifier.OR, buildSearchControl(attributeValue));
        searchControl.setAllReturnAttributes(false);

        try {
            AMIdentityRepository amirepo = new AMIdentityRepository(getSSOSession(), organization);

            IdSearchResults searchResults = amirepo.searchIdentities(IdType.USER, "*", searchControl);
            if (searchResults.getErrorCode() == IdSearchResults.SUCCESS && searchResults != null) {
                Set<AMIdentity> results = searchResults.getSearchResults();
                if (!results.isEmpty()) {
                    if (debug.messageEnabled()) {
                        debug.message(classMethod + results.size() + " result(s) obtained");
                    }
                    AMIdentity userDNId = results.iterator().next();
                    if (userDNId != null) {
                        if (debug.messageEnabled()) {
                             debug.message(classMethod + "user = " + userDNId.getUniversalId());
                             debug.message(classMethod + "attrs =" + userDNId.getAttributes(
                                     getUserAliasList()));
                        }
                        return attributeValue.trim();
                    }
                }
            }
        } catch (IdRepoException idrepoex) {
            String data[] = {attributeValue, organization};
            throw new AuthLoginException(amAuthWindowsDesktopSSO, 
                "idRepoSearch", data, idrepoex);
        } catch (SSOException ssoe) {
            String data[] = {attributeValue, organization};
            throw new AuthLoginException(amAuthWindowsDesktopSSO, 
                "ssoSearch", data, ssoe);
        }
        if (debug.messageEnabled()) {
                    debug.message(classMethod + " No results were found !");
        }
        return null;
    }

    private Map<String, Set<String>> buildSearchControl(String value)
            throws AuthLoginException {     
        Map<String, Set<String>> attr = new HashMap<String, Set<String>>();
        Set<String> userAttrs = getUserAliasList();
        for (String userAttr : userAttrs) {
            attr.put(userAttr, addToSet(new HashSet<String>(), value));
        }
        return attr;
    }

    private static Set<String> addToSet(Set<String> set, String attribute) {
        set.add(attribute);
        return set;
    }
    
    private static Set<String> getAcceptedKerberosRealms(Map options) {
        Set<String> result = Collections.EMPTY_SET;
        final Object tmp = options.get(ACCEPTED_REALMS_ATTR);
        if (tmp != null) {
            result = Collections.unmodifiableSet((Set<String>)tmp);
        }
        return result;
    }    
    
    private static boolean isTokenTrusted(final String UPN, final String realm) {
        boolean trusted = false;
        if (UPN != null ) {
            final int param_index = UPN.indexOf(REALM_SEPARATOR);
            if (param_index != -1) {
                final String realmPart = UPN.substring(param_index + 1);
                if (realmPart.equalsIgnoreCase(realm)) {
                    trusted = true;
                }
            }
        }
        return trusted;
    }
}
