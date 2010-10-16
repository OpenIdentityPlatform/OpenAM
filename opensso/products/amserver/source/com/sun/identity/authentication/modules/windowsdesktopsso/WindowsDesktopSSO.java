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
 */


package com.sun.identity.authentication.modules.windowsdesktopsso;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.util.DerValue;
import com.sun.identity.authentication.util.ISAuthConstants;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
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
import sun.misc.BASE64Decoder;

public class WindowsDesktopSSO extends AMLoginModule {
    private static final String amAuthWindowsDesktopSSO = 
        "amAuthWindowsDesktopSSO";

    private static final String[] configAttributes = {
        "iplanet-am-auth-windowsdesktopsso-principal-name",
        "iplanet-am-auth-windowsdesktopsso-keytab-file",
        "iplanet-am-auth-windowsdesktopsso-kerberos-realm",
        "iplanet-am-auth-windowsdesktopsso-kdc",
        "iplanet-am-auth-windowsdesktopsso-returnRealm",
        "iplanet-am-auth-windowsdesktopsso-auth-level",
        "serviceSubject" };

    private static final int PRINCIPAL = 0;
    private static final int KEYTAB    = 1;
    private static final int REALM     = 2;
    private static final int KDC       = 3;
    private static final int RETURNREALM = 4;
    private static final int AUTHLEVEL = 5;
    private static final int SUBJECT   = 6;
        
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

    private Debug debug = Debug.getInstance(amAuthWindowsDesktopSSO);

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

        if ( !getConfigParams() ) {
            initWindowsDesktopSSOAuth(options);
        }

        // retrieve the spnego token
        byte[] spnegoToken = 
            getSPNEGOTokenFromHTTPRequest(getHttpServletRequest());
        if (spnegoToken == null) {
            spnegoToken = getSPNEGOTokenFromCallback(callbacks);
        }

        if (spnegoToken == null) {
            debug.message("spnego token is not valid.");
            throw new AuthLoginException(amAuthWindowsDesktopSSO, "token",null);
        }

        if (debug.messageEnabled()) {
            debug.message("SPNEGO token: \n" + 
                DerValue.printByteArray(spnegoToken, 0, spnegoToken.length));
        }
        // parse the spnego token and extract the kerberos mech token from it
        final byte[] kerberosToken = parseToken(spnegoToken);
        if (kerberosToken == null) {
            debug.message("kerberos token is not valid.");
            throw new AuthLoginException(amAuthWindowsDesktopSSO, "token",null);
        }
        if (debug.messageEnabled()) {
            debug.message("Kerberos token retrieved from SPNEGO token: \n" + 
                DerValue.printByteArray(kerberosToken,0,kerberosToken.length));
        }

        // authenticate the user with the kerberos token
        try {
            authenticateToken(kerberosToken);
            debug.message("WindowsDesktopSSO authentication succeeded.");
            result = ISAuthConstants.LOGIN_SUCCEED; 
         } catch (PrivilegedActionException pe) {
             Exception e = extractException(pe);	 
             if( e instanceof GSSException) {	 
                 int major = ((GSSException)e).getMajor();	 
                 if (major == GSSException.CREDENTIALS_EXPIRED) {	 
                         debug.message("Credential expired. Re-establish credential...");	 
                 serviceLogin();	 
                 try {	 
                         authenticateToken(kerberosToken);	 
                         debug.message("Authentication succeeded with new cred.");	 
                         result = ISAuthConstants.LOGIN_SUCCEED;	 
                  } catch (Exception ee) {	 
                 debug.message("Authentication failed with new cred.");	 
                 throw new AuthLoginException(amAuthWindowsDesktopSSO,	 
                                 "auth", null, ee);	 
                 }	 
               } else {	 
                         debug.message("Authentication failed with GSSException.");	 
                  throw new AuthLoginException(amAuthWindowsDesktopSSO, "auth",	 
                         null, e);	 
               }	 
             }	 
        } catch (GSSException e ){
            int major = e.getMajor();
            if (major == GSSException.CREDENTIALS_EXPIRED) {
                debug.message("Credential expired. Re-establish credential...");
                serviceLogin();
                    try {
                    authenticateToken(kerberosToken);
                    debug.message("Authentication succeeded with new cred.");
                        result = ISAuthConstants.LOGIN_SUCCEED; 
                } catch (Exception ee) {
                        debug.message("Authentication failed with new cred.");
                        throw new AuthLoginException(amAuthWindowsDesktopSSO, 
                        "auth", null, ee);
                }
            } else {
                debug.message("Authentication failed with GSSException.");
                throw new AuthLoginException(amAuthWindowsDesktopSSO, "auth", 
                    null, e);
            }
        } catch (AuthLoginException e) {
            throw e;
        } catch (Exception e) {
            debug.message("Authentication failed with generic exception.");
            throw new AuthLoginException(amAuthWindowsDesktopSSO, "auth", 
                null, e);
        }
        return result;
    }

    private void authenticateToken(final byte[] kerberosToken) 
            throws AuthLoginException, GSSException, Exception {

        debug.message("In authenticationToken ...");
        Subject.doAs(serviceSubject, new PrivilegedExceptionAction(){
            public Object run() throws Exception {
                GSSContext context =
                    GSSManager.getInstance().createContext(
                        (GSSCredential)null);
                debug.message("Context created.");

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
                    debug.message("Cannot establish context !");
                    throw new AuthLoginException(amAuthWindowsDesktopSSO,
                        "context", null);
                } else {
                    debug.message("Context establised !");
                    GSSName user = context.getSrcName();
                    storeUsernamePasswd(user.toString(), null);

                    if (debug.messageEnabled()){
                        debug.message("User authenticated: " + user.toString());
                    }
                    if (user != null) {
                        setPrincipal(user.toString());
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
    }

    private void setPrincipal(String user) {
        String principal = user;
        if (!returnRealm) {
            int index = user.indexOf("@");
            if (index != -1) {
                principal = user.substring(0, index);
            }
        }
        userPrincipal = new WindowsDesktopSSOPrincipal(principal);
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


    private byte[] getSPNEGOTokenFromHTTPRequest(HttpServletRequest req) {
        byte[] spnegoToken = null;
        String header = req.getHeader("Authorization");
        if ((header != null) && header.startsWith("Negotiate")) {
            header = header.substring("Negotiate".length()).trim();
            BASE64Decoder decoder = new BASE64Decoder();
            try { 
                spnegoToken = decoder.decodeBuffer(header);
            } catch (Exception e) {
                debug.error("Decoding token error.");
                if (debug.messageEnabled()) {
                    debug.message("Stack trace: ", e);
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
            BASE64Decoder decoder = new BASE64Decoder();
            try {
                spnegoToken = decoder.decodeBuffer(spnegoTokenStr);
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
            debug.message("SPNEGO OID found in the Auth Token");
            tmpToken = new DerValue(tmpInput);

            // 0xa0 indicates an init token(NegTokenInit); 0xa1 indicates an 
            // response arg token(NegTokenTarg). no arg token is needed for us.

            if (tmpToken.getTag() == (byte)0xa0) {
                debug.message("DerValue: found init token");
                tmpToken = new DerValue(tmpToken.getData());
                if (tmpToken.getTag() == (byte)0x30) {
                    debug.message("DerValue: 0x30 constructed token found");
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
            debug.message("SPNEGO OID not found in the Auth Token");
            byte[] krb5Oid = new byte[KERBEROS_V5_OID.length];
            int i = 0;
            for (; i < oidArray.length; i++) {
                krb5Oid[i] = oidArray[i];
            }
            tmpInput.read(krb5Oid, i, krb5Oid.length - i);
            if (!Arrays.equals(krb5Oid, KERBEROS_V5_OID)) {
                debug.message("Kerberos V5 OID not found in the Auth Token");
                token = null;
            } else {
                debug.message("Kerberos V5 OID found in the Auth Token");
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

        if (debug.messageEnabled()){
            debug.message("WindowsDesktopSSO params: \n" + 
                "principal: " + servicePrincipalName +
                     "\nkeytab file: " + keyTabFile +
                "\nrealm : " + kdcRealm +
                "\nkdc server: " + kdcServer +
                "\ndomain principal: " + returnRealm +
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

        debug.message("Retrieved config params from cache.");
        return true;
    }

    private void initWindowsDesktopSSOAuth(Map options) 
        throws AuthLoginException{

        debug.message("Init WindowsDesktopSSO. This should not happen often.");

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
        debug.message("New Service Login ...");
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
            debug.message("Service login succeeded.");
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
}
