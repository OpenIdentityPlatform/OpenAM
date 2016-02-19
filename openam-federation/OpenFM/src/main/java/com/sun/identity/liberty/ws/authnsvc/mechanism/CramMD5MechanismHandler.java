/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CramMD5MechanismHandler.java,v 1.8 2008/12/16 20:54:03 hengming Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */


package com.sun.identity.liberty.ws.authnsvc.mechanism;


import static org.forgerock.openam.utils.Time.*;

import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.sun.identity.common.PeriodicCleanUpMap;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TaskRunnable;
import com.sun.identity.common.TimerPool;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcConstants;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcService;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcUtils;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLRequest;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLResponse;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.sm.SMSEntry;

/**
 * The <code>CramMD5MechanismHandler</code> is a handler for 'CRAM-MD5'
 * mechanism.
 */
public class CramMD5MechanismHandler implements MechanismHandler {
    private static Debug debug = Debug.getInstance("libIDWSF");

    private static final String PROP_SERVER_HOST = "com.iplanet.am.server.host"; 
    private static final String serverHost = SystemPropertiesManager.get(
        PROP_SERVER_HOST, "localhost");

    private static final int MAX_RANDOM_NUM = 9999;
    private static final int NUM_RANDOM_DIGITS =
                                    Integer.toString(MAX_RANDOM_NUM).length();
    private static final String ATTR_USER_PASSWORD = "userPassword";
    private static final String COMP_AUTHN_SVC = "authnsvc";

    /**
     * The block length in characters used in generating an HMAC-MD5 digest.
     */
    private static final int BLOCK_LENGTH = 64;
    private static final byte IPAD_BYTE = 0x36;
    private static final byte OPAD_BYTE = 0x5c;
    /**
     * table to convert a nibble to a hex char.
     */
    private static char[] hexChar = { '0' , '1' , '2' , '3' ,
                                      '4' , '5' , '6' , '7' ,
                                      '8' , '9' , 'a' , 'b' ,
                                      'c' , 'd' , 'e' , 'f' };

    private static SecureRandom secureRandom = new SecureRandom();

    static final String CHALLENGE_CLEANUP_INTERVAL_PROP =
        "com.sun.identity.liberty.ws.authnsvc.challengeCleanupInterval";
    static int challenge_cleanup_interval = 60000; // millisec
    static final String STALE_TIME_LIMIT_PROP =
        "com.sun.identity.liberty.ws.soap.staleTimeLimit";
    static int stale_time_limit = 300000; // millisec
    
    private static Map challengeMap = new PeriodicCleanUpMap(
        (long) challenge_cleanup_interval, (long) stale_time_limit);

    static {
        String tmpstr =
            SystemPropertiesManager.get(CHALLENGE_CLEANUP_INTERVAL_PROP);
        if (tmpstr != null) {
            try {
                challenge_cleanup_interval = Integer.parseInt(tmpstr);
            } catch (Exception ex) {
                if (debug.warningEnabled()) {
                    debug.warning(
                        "CramMD5MechanismHandler.static:" +
                        " Unable to get stale time limit. Default" +
                        " value will be used", ex);
                }
            }
        }

        tmpstr = SystemPropertiesManager.get(STALE_TIME_LIMIT_PROP);
        if (tmpstr != null) {
            try {
                stale_time_limit = Integer.parseInt(tmpstr);
            } catch (Exception ex) {
                if (debug.warningEnabled()) {
                    debug.warning(
                         "CramMD5MechanismHandler.static:" +
                         " Unable to get stale time limit. Default " +
                         "value will be used");
                }
            }
        }
        SystemTimerPool.getTimerPool().schedule((TaskRunnable) challengeMap, 
            new Date(((currentTimeMillis() + challenge_cleanup_interval)
            / 1000) * 1000));
    }

    /**
     * Generates a SASL response according to the SASL request.
     * @param saslReq a SASL request
     * @param message a SOAP Message containing the SASL request
     * @param respMessageID messageID of SOAP Message response that will
     *                      contain returned SASL response
     * @return a SASL response
     */
    public SASLResponse processSASLRequest(SASLRequest saslReq,
                                       Message message, String respMessageID) {

        if (debug.messageEnabled()) {
            debug.message("CramMD5MechanismHandler.processSASLRequest: ");
        }

        String refToMessageID = saslReq.getRefToMessageID();
        boolean isFirstRequest = (refToMessageID == null ||
                                  refToMessageID.length() == 0);

        if (debug.messageEnabled()) {
            debug.message("CramMD5MechanismHandler.processSASLRequest: " + 
                "refToMessageID = " + refToMessageID);
        }

        SASLResponse saslResp = null;

        byte[] data = saslReq.getData();

        if (data == null) {
            if (isFirstRequest) {
                saslResp = new SASLResponse(SASLResponse.CONTINUE);
                saslResp.setServerMechanism(
                                AuthnSvcConstants.MECHANISM_CRAMMD5);
                byte[] challenge = generateChallenge();
                if (debug.messageEnabled()) {
                    debug.message("CramMD5MechanismHandler.processSASLRequest:"
                        + " add respMessageID: " + respMessageID);
                }
                challengeMap.put(respMessageID, challenge);
                saslResp.setData(challenge);
            } else {
                saslResp = new SASLResponse(SASLResponse.ABORT);
            }
        } else {
            String dataStr = null;
            try {
                dataStr = new String(data, "UTF-8");
            } catch (Exception ex) {
                debug.error("CramMD5MechanismHandler.processSASLRequest: ", ex);
            }

            if (dataStr == null) {
                saslResp = new SASLResponse(SASLResponse.ABORT);
            } else {
                saslResp = authenticate(dataStr, message);
            }

            if (isFirstRequest) {
                saslResp.setServerMechanism(AuthnSvcConstants.MECHANISM_PLAIN);
            }
        }
        return saslResp;
    }

    private SASLResponse authenticate(String data, Message message) {

        int index = data.indexOf(' ');
        if (index == -1) {
            return new SASLResponse(SASLResponse.ABORT);
        }

        String userName = data.substring(0, index);
        String clientDigest = data.substring(index + 1);

        String password = getUserPassword(userName);

        if (password == null) {
            if (debug.messageEnabled()) {
                debug.message(
                    "CramMD5MechanismHandler.authenticate: can't get password");
            }
            return new SASLResponse(SASLResponse.ABORT);
        }

        String refToMessageID = message.getCorrelationHeader()
                                       .getRefToMessageID();

        if (refToMessageID == null || refToMessageID.length() == 0) {
            if (debug.messageEnabled()) {
                debug.message(
                    "CramMD5MechanismHandler.authenticate: no refToMessageID");
            }
            return new SASLResponse(SASLResponse.ABORT);
        }

        byte[] challengeBytes = null;
        if (debug.messageEnabled()) {
            debug.message("CramMD5MechanismHandler.authenticate:" +
                " remove refToMessageID: " + refToMessageID);
        }

        challengeBytes = (byte[])challengeMap.remove(refToMessageID);
        
        if (challengeBytes == null) {
            if (debug.messageEnabled()) {
                debug.message(
                    "CramMD5MechanismHandler.authenticate: no challenge found");
            }
            return new SASLResponse(SASLResponse.ABORT);
        }

        byte[] passwordBytes = null;
        try {
            passwordBytes = password.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ueex) {
            debug.error("CramMD5MechanismHandler.authenticate:", ueex);
            return new SASLResponse(SASLResponse.ABORT);
        }

        String serverDigest = null;
        try {
            serverDigest = generateHMACMD5(passwordBytes, challengeBytes);
        } catch(NoSuchAlgorithmException nsaex) {
            debug.error("CramMD5MechanismHandler.authenticate:", nsaex);
            return new SASLResponse(SASLResponse.ABORT);
        }

        if (!clientDigest.equals(serverDigest)) {
            if (debug.messageEnabled()) {
                debug.message(
                    "CramMD5MechanismHandler.authenticate: digests not equal");
            }
            return new SASLResponse(SASLResponse.ABORT);
        }

        if (debug.messageEnabled()) {
            debug.message(
                "CramMD5MechanismHandler.authenticate: digests equal");
        }

        String authModule =
            AuthnSvcService.getCramMD5MechanismAuthenticationModule();

        if (debug.messageEnabled()) {
            debug.message("PlainMechanismHandler.authenticate: " + 
                "authModule = " + authModule);
        }

        AuthContext authContext = null;
        try {
            authContext = new AuthContext(SMSEntry.getRootSuffix());
            authContext.login(AuthContext.IndexType.MODULE_INSTANCE,
                authModule);
        } catch (AuthLoginException le) {
            debug.error("CramMD5MechanismHandler.authenticate: ", le);
            return new SASLResponse(SASLResponse.ABORT);
        }

        if (authContext.hasMoreRequirements()) {
            Callback[] callbacks = authContext.getRequirements();

            if (callbacks != null) {
                fillInCallbacks(callbacks, userName, password);
                authContext.submitRequirements(callbacks);
            }
        }

        AuthContext.Status loginStatus = authContext.getStatus();
        if (debug.messageEnabled()) {
            debug.message(
                "CramMD5MechanismHandler.authenticate: login status = " + 
                loginStatus);
        }
        if (loginStatus != AuthContext.Status.SUCCESS) {
            return new SASLResponse(SASLResponse.ABORT);
        }

        try {
            SSOToken token = authContext.getSSOToken();
            String userDN = token.getPrincipal().getName();

            try {
                SSOTokenManager.getInstance().destroyToken(token);
            } catch (SSOException ssoex) {
                if (AuthnSvcUtils.debug.warningEnabled()) {
                    AuthnSvcUtils.debug.warning(
                        "PlainMechanismHandler.authenticate:", ssoex);
                }
            }

            SASLResponse saslResp = new SASLResponse(SASLResponse.OK);

            if (!AuthnSvcUtils.setResourceOfferingAndCredentials(
                saslResp, message, userDN)) {
                return new SASLResponse(SASLResponse.ABORT);
            }
            return saslResp;
        } catch (Exception ex) {
            debug.error("CramMD5MechanismHandler.authenticate: ", ex);
            return new SASLResponse(SASLResponse.ABORT);
        }

    }

    private static void fillInCallbacks(Callback[] callbacks,
                                        String username,
                                        String password) {
        if (debug.messageEnabled()) {
            debug.message("CramMD5MechanismHandler.fillInCallbacks:");
        }

        for(int i = 0; i < callbacks.length; i++) {
            Callback callback = callbacks[i];

            if (callback instanceof NameCallback) {
                ((NameCallback)callback).setName(username);
            } else if (callback instanceof PasswordCallback) {
                ((PasswordCallback)callback).setPassword(
                                                  password.toCharArray());
            } 
        }
    }

    private static byte[] generateChallenge() {
        StringBuffer sb = new StringBuffer();
        sb.append("<");

        // append random digits
        int randomInt = secureRandom.nextInt(MAX_RANDOM_NUM);
        String randomIntString = Integer.toString(randomInt);
        for(int i=randomIntString.length(); i<NUM_RANDOM_DIGITS; i++) {
            sb.append("0");
        }

        sb.append(randomIntString).append(".");

        // append timestamp
        sb.append(currentTimeMillis()).append("@");

        // append hostname
        sb.append(serverHost).append(">");

        try {
            return sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException ueex) {
            return sb.toString().getBytes();
        }
    }

    private static String getUserPassword(String userName) {

        try {

            SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
		AdminTokenAction.getInstance());
	    AMIdentityRepository idRepo = new AMIdentityRepository(adminToken,
                SMSEntry.getRootSuffix());
	    IdSearchControl searchControl = new IdSearchControl();
	    searchControl.setTimeOut(0);
	    searchControl.setMaxResults(0);
	    searchControl.setAllReturnAttributes(false);
	    IdSearchResults searchResults = idRepo.searchIdentities(
		IdType.USER, userName, searchControl);
	    Set users = searchResults.getSearchResults();

            if (users == null || users.isEmpty()) {
                if (debug.messageEnabled()) {
                    debug.message("CramMD5MechanismHandler.getUserPassword: " +
                        "no user found");
                }
                return null;
            }

            if (users.size() > 1) {
                if (debug.messageEnabled()) {
                    debug.message("CramMD5MechanismHandler.getUserPassword: " +
                        "more than 1 user found");
                }
                return null;
            }

            AMIdentity user = (AMIdentity)users.iterator().next();
            Set passwords = user.getAttribute("userPassword");
            if (passwords == null || passwords.isEmpty()) {
                if (debug.messageEnabled()) {
                    debug.message("CramMD5MechanismHandler.getUserPassword: " +
                        "user has no password");
                }
                return null;
            }
            if (passwords.size() > 1) {
                if (debug.messageEnabled()) {
                    debug.message("CramMD5MechanismHandler.getUserPassword: " +
                        "user has more than 1 passwords");
                }
                return null;
            }

            String password = (String)passwords.iterator().next();
            if (password.startsWith("{CLEAR}")) {
                password = password.substring(7);
            }
            return password;
        } catch (Exception ex) {
            AuthnSvcUtils.debug.error(
                "CramMD5MechanismHandler.getUserPassword: ", ex);
            return null;
        }
    }

    private static String generateHMACMD5(byte[] passwordBytes,
                                          byte[] challengeBytes)
        throws NoSuchAlgorithmException
    {
        MessageDigest messagedigest = MessageDigest.getInstance("MD5");
        if(passwordBytes.length > BLOCK_LENGTH) {
            passwordBytes = messagedigest.digest(passwordBytes);
        }

        byte abyte2[] = new byte[BLOCK_LENGTH];
        byte abyte3[] = new byte[BLOCK_LENGTH];
        for(int i = 0; i < passwordBytes.length; i++) {

            abyte2[i] = (byte)(passwordBytes[i] ^ IPAD_BYTE);
            abyte3[i] = (byte)(passwordBytes[i] ^ OPAD_BYTE);
        }
        for(int i = passwordBytes.length; i < BLOCK_LENGTH; i++) {
            abyte2[i] = 0 ^ IPAD_BYTE;
            abyte3[i] = 0 ^ OPAD_BYTE;
        }


        messagedigest.update(abyte2);
        messagedigest.update(challengeBytes);
        byte digestBytes[] = messagedigest.digest();

        messagedigest.update(abyte3);
        messagedigest.update(digestBytes);
        digestBytes = messagedigest.digest();

    
        return toHexString(digestBytes);
    }

    private static String toHexString ( byte[] b ) {

        StringBuffer sb = new StringBuffer( b.length * 2 );
        for ( int i=0; i<b.length; i++ ) {

            sb.append( hexChar [( b[i] & 0xf0 ) >>> 4] );
            sb.append( hexChar [b[i] & 0x0f] );
        }
        return sb.toString();
    }

}
