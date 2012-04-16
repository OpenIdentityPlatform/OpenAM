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
 * $Id: NT.java,v 1.6 2009/03/03 06:00:35 si224302 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.modules.nt;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.security.Principal;
import java.util.ResourceBundle;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

public class NT extends AMLoginModule {
    private static boolean hasInitialized = false;
    private static String baseDir;
    private static com.sun.identity.shared.debug.Debug debug = null;
    private static String smbPath;
    private static final String charSet = "ISO8859_1";
    private static final String amAuthNT = "amAuthNT";

    private ResourceBundle bundle = null;
    private Map options;
    private Map sharedState;
    private String host;
    private String domain;
    private NTPrincipal userPrincipal;
    private String userTokenId = "";
    private String userName = null;
    private String smbConfFileName; 
    //Screen State

    private boolean getCredentialsFromSharedState;

    public NT() {
    }
 
    /**
     * TODO-JAVADOC
     */
    public void init(Subject subject, Map sharedState, Map options) {
        java.util.Locale locale = getLoginLocale();
        bundle = amCache.getResBundle(amAuthNT, locale);
        
        if (debug.messageEnabled()) {
            debug.message("NT resbundle locale="+locale);
        }

        this.sharedState = sharedState;
        this.options = options;

        if (options != null) {
            host = CollectionHelper.getServerMapAttr(
                options, "iplanet-am-auth-nt-host");
            domain = CollectionHelper.getServerMapAttr(
                options, "iplanet-am-auth-nt-domain");
            smbConfFileName = CollectionHelper.getServerMapAttr(
                options, "iplanet-am-auth-samba-config-file-name");
            String authLevel = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-nt-auth-level");
            if (authLevel != null) {
                 try {
                      setAuthLevel(Integer.parseInt(authLevel));
                 } catch (Exception e) {
                      debug.error("Unable to set auth level " + authLevel);
                 }
            }
        }
    }

    static {
        if (debug == null) {
            debug =  com.sun.identity.shared.debug.Debug.getInstance(amAuthNT);
            debug.message("NT constructor called");
        }
        String base = SystemProperties.get(SystemProperties.CONFIG_PATH);
        String deployURL = SystemProperties.get(
            Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        baseDir = base + "/" + deployURL;
        smbPath = baseDir + "/bin/smbclient";
        File file = new File (smbPath);
        if(!file.exists()) {
            debug.error ("smbclient file not found");
             hasInitialized = false;
        }
        hasInitialized = true;
    }

    /**
     * TODO-JAVADOC
     */
    public int process(Callback[] callbacks, int state) 
        throws AuthLoginException {
        if (!hasInitialized) {
            throw  new AuthLoginException(amAuthNT, "NTSMB", null);
        }
        if (host == null || host.length() == 0) {
            debug.message ("NT Host cannot be null ");
            throw new AuthLoginException(amAuthNT, "Hosterror", null);
        }
        if (domain == null || domain.length() == 0) {
            debug.message ("NT Domain cannot be null ");   
            throw new AuthLoginException(amAuthNT, "Domainerror", null);
        }
        try {
            if (!host.equals(new String(host.getBytes("ASCII"), "ASCII"))) {
                throw new AuthLoginException(amAuthNT, 
                        "NTHostnameNotASCII", null);
            }
            if (!domain.equals(new String(domain.getBytes("ASCII"), "ASCII"))) {
                throw new AuthLoginException(amAuthNT, 
                        "NTDomainnameNotASCII", null);
            }
        } catch (UnsupportedEncodingException ueex) {
            throw new AuthLoginException(amAuthNT, "NTInputNotASCII", null);
        }

        String userPassword = null;
        if (callbacks !=null && callbacks.length == 0) {                
            userName = (String) sharedState.get(getUserKey());
            userPassword = (String) sharedState.get(getPwdKey());
            if (userName == null || userPassword == null) {
                return ISAuthConstants.LOGIN_START;
            }
            getCredentialsFromSharedState = true;
        } else { 
        
            userName =  ((NameCallback)callbacks[0]).getName();
            userPassword = 
                charToString(((PasswordCallback)callbacks[1]).getPassword(),
                             callbacks[1]);
            if (userName == null || userName.length() == 0) {
                debug.message ("UserId cannot be null");
                throw new AuthLoginException(amAuthNT, "UserIderror", null);
            }
            if (userPassword == null || userPassword.length() == 0) {
                debug.message ("Password cannot be null");
                setFailureID(userName);
                throw new AuthLoginException(amAuthNT, "Passworderror", null);
            }
        }

        // store username, password both in success and failure case        
        storeUsernamePasswd(userName, userPassword);

        try {
            if (!userName.equals(new String(userName.getBytes("ASCII"),
                "ASCII"))) {
               if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                    return ISAuthConstants.LOGIN_START;
               }
                throw new AuthLoginException(amAuthNT, 
                        "NTUsernameNotASCII", null);
            }
            if (!userPassword.equals(new String(userPassword.getBytes("UTF-8"), 
            "UTF-8"))) {
               if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                    return ISAuthConstants.LOGIN_START;
               }
                setFailureID(userName);
                throw new AuthLoginException(amAuthNT, 
                        "NTPasswordNotASCII", null);
            }
        } catch (UnsupportedEncodingException ueex) {
           if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                getCredentialsFromSharedState = false;
                return ISAuthConstants.LOGIN_START;
           }
            throw new AuthLoginException(amAuthNT, "NTInputNotASCII", null);
        }
        
        if (debug.messageEnabled()) {
            debug.message ("userName='"+ userName + "' host='" + host + "'");
            debug.message ("domain='" + domain + "'");
        }
        File tmpFile = null;
        try {
            // Create the tmpFile
            tmpFile = File.createTempFile(userName,"pwd");
            FileOutputStream fw = new FileOutputStream(tmpFile);
            OutputStreamWriter dos = new OutputStreamWriter(fw, "ISO-8859-1");
            dos.write("username = " + userName + "\n");
            dos.write("password = " + userPassword);
            dos.flush();
            dos.close();
            fw.close();
            
            Runtime rt = Runtime.getRuntime();
            int c;
            StringBuilder buftxt = new StringBuilder(80);

            String[] progarr = null;
            if ((smbConfFileName != null) && (smbConfFileName.length() > 0)) {
                    progarr = new String[9];
                progarr[7] = "-s";
                progarr[8] = smbConfFileName; 
            } else {
                progarr = new String[7];
            }
            progarr[0] = smbPath;
            progarr[1] = "-W";
            progarr[2] = domain;
            progarr[3] = "-L";
            progarr[4] = host;
            progarr[5] = "-A";
            progarr[6] = tmpFile.getAbsolutePath();

            Process smbconn = rt.exec(progarr);

            BufferedReader smbout = new BufferedReader(
                new InputStreamReader(smbconn.getInputStream(), charSet));
            while ((c= smbout.read()) > -1) {
                char chtxt = ((char)c);
                buftxt.append(chtxt);
            }
            smbout.close();
            String out = buftxt.toString();

            if (out.indexOf ("Usage:") != -1) {
                   if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                           return ISAuthConstants.LOGIN_START;
                   }
                if (debug.messageEnabled()) {
                    debug.message("smbclient usage error");
                }
                setFailureID(userName);
                throw new AuthLoginException(amAuthNT, "NTSMBUsage", null);
            } else if(out.indexOf("failed") != -1) {
                   if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                           return ISAuthConstants.LOGIN_START;
                   }
                if (debug.messageEnabled()) {
                    debug.message("NT authentication failed" + out);
                }
                setFailureID(userName);
                throw new AuthLoginException(amAuthNT, "NTLoginFailed", null);
            } else if (out.indexOf ("timeout") != -1) {
                   if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                           return ISAuthConstants.LOGIN_START;
                   }
                if (debug.messageEnabled()) {
                    debug.message("smbclient timeout error");
                }
                setFailureID(userName);
                throw new AuthLoginException(amAuthNT, "NTSMBTimeout", null);
            } else {
                int exitValue = smbconn.waitFor();
                if (debug.messageEnabled()) {
                    debug.message("Exit value of samba client: " + exitValue);
                }
                if (exitValue != 0) {
                       if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                        getCredentialsFromSharedState = false;
                               return ISAuthConstants.LOGIN_START;
                       }
                    setFailureID(userName);
                    throw new AuthLoginException(
                        amAuthNT, "NTAuthFailed", null);
                }
                userTokenId = userName;
                return ISAuthConstants.LOGIN_SUCCEED;
            }
        } catch (Exception ex) {
           if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                getCredentialsFromSharedState = false;
                return ISAuthConstants.LOGIN_START;
           }

            if (debug.messageEnabled()) {
                debug.message("NT authentication failed" + ex.getMessage());
            }
            setFailureID(userName);
            throw new AuthLoginException(amAuthNT, "NTAuthFailed", null, ex);
        }
        finally {
            //Deletes the password file in any situation
            if(tmpFile !=null) {
                try {
                    tmpFile.delete();
                } catch(Exception e){}
            } 
       }
    }

    private String charToString (char [] tmpPassword, Callback cbk ) {
        if (tmpPassword == null) {
            // treat a NULL password as an empty password
            tmpPassword = new char[0];
        }
        char[] pwd = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0,
                         pwd, 0, tmpPassword.length);
        ((PasswordCallback)cbk).clearPassword();
        return new String(pwd);
    }

    /**
     * TODO-JAVADOC
     */
    public Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userTokenId != null) {
            userPrincipal = new NTPrincipal(userTokenId);
            return userPrincipal;
        } else {
            return null;
        } 
    }

    /**
     * TODO-JAVADOC
     */
    public void destroyModuleState() {
        userTokenId = null;
        userPrincipal = null;
    }

    /**
     * TODO-JAVADOC
     */
    public void nullifyUsedVars() {
        bundle = null;
        options = null;
        sharedState = null;
        host = null;
        domain = null;
        userName = null;
        smbConfFileName = null; 
    }
}
