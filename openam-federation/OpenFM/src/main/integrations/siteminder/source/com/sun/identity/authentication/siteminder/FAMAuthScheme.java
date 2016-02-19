/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FAMAuthScheme.java,v 1.5 2008/08/19 19:11:40 veiming Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.authentication.siteminder;

import java.util.Date;
import java.io.*;
import java.text.DateFormat;
import com.netegrity.policyserver.smapi.SmAuthScheme;
import com.netegrity.policyserver.smapi.SmAuthQueryResponse;
import com.netegrity.policyserver.smapi.SmAuthenticationResult;
import com.netegrity.policyserver.smapi.APIContext;
import com.netegrity.policyserver.smapi.SmAuthStatus;
import com.netegrity.policyserver.smapi.AppSpecificContext;
import com.netegrity.policyserver.smapi.SmAuthenticationContext;
import com.netegrity.policyserver.smapi.SmJavaApiException;
import com.netegrity.policyserver.smapi.UserCredentialsContext;
import com.netegrity.policyserver.smapi.SmAuthQueryCode;
import com.netegrity.policyserver.smapi.UserContext;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOToken;
        


/**
 * The class <code>FAMAuthScheme</code> implements siteminer authentication
 * scheme <code>SMAuthScheme</code>. The FAMAuthScheme generates siteminder
 * session by consuming an OpenAM session.
 * Siteminder is the trade mark of Computer Associates, the usage of the
 * Siteminder API is subject to Siteminder License terms.
 */

public class FAMAuthScheme implements SmAuthScheme {
    private static final int SCHEME_VERSION = 
                         SmAuthQueryResponse.SMAUTH_API_VERSION_V3;
    private static final String SCHEME_DESCRIPTION = 
                         "OpenSSO Auth Scheme";


    final static String  FMPREFIX = "FMTOKEN";
    static int fmprefixLen = FMPREFIX.length();
    PrintStream  logw = null;

    /**
     * Returns information about the authentication scheme.
     *
     * @param parameter - The parameter string as specified for the 
     *                    authentication scheme.
     * @param secret - The secret string as specified for the authentication 
     *                 scheme.
     * @param request - The request code, SMAUTH_QUERY_DESCRIPTION or 
     *                  SMAUTH_QUERY_CREDENTIALS_REQ.
     * @param response - Holds methods by which query() returns the 
     *                   requested information.
     * @return SMAUTH_SUCCESS if successful 
     *           otherwise SMAUTH_FAILURE
     */

    public SmAuthStatus
    query(String parameter,
          String secret,
          SmAuthQueryCode request,
          SmAuthQueryResponse response) {

        if (null == response) {
            return SmAuthStatus.SMAUTH_FAILURE;
        }

        if (SmAuthQueryCode.SMAUTH_QUERY_DESCRIPTION == request) {
            response.setResponseBuffer(SCHEME_DESCRIPTION);
            response.setResponseCode(SCHEME_VERSION);
        } else if (SmAuthQueryCode.SMAUTH_QUERY_CREDENTIALS_REQ == request) {
            response.setResponseCode(SmAuthQueryResponse.SMAUTH_CRED_BASIC);               
        }
        else {
            return SmAuthStatus.SMAUTH_FAILURE;
        }

        return SmAuthStatus.SMAUTH_SUCCESS;
    }


    /**
     * SiteMinder invokes this method so the authentication scheme can
     * perform its own initialization procedure. This method is invoked once
     * for each authentication scheme instance when it is first loaded.
     *
     * @param parameter The parameter string as specified for the 
     *                    authentication scheme.
     * @param secret  The secret string as specified for the authentication 
     *                 scheme.
     * @return: If successful returns SMAUTH_SUCCESS, 
     *           otherwise returns SMAUTH_FAILURE
     */

    public SmAuthStatus init(String parameter, String secret) {
        try {
            logw = new PrintStream(new FileOutputStream("/tmp/FMAuth"));
            logw.println("RYA:init");
        } catch (Exception ex) {
        }
        return SmAuthStatus.SMAUTH_SUCCESS;
    }


    /**
     * SiteMinder invokes this method during shutdown so the authentication
     * scheme can perform its own rundown procedure. This method is invoked
     * once for each authentication scheme instance during SiteMinder shutdown.
     *
     * @param parameter  The parameter string as specified for the 
     *                   authentication scheme.
     * @param secret  The secret string as specified for the authentication 
     *                scheme
     * @return: If successful returns SMAUTH_SUCCESS, otherwise 
     *          returns SMAUTH_FAILURE
     */

    public SmAuthStatus release(String parameter, String secret) {
        return SmAuthStatus.SMAUTH_SUCCESS;
    }


    /**
     * SiteMinder invokes this method to authenticate user credentials.
     *
     * @param parameter  The parameter string as specified for the 
     *                   authentication scheme.
     * @param secret The secret string as specified for the authentication 
     *               scheme
     * @param challengeReason The reason for the original authentication
     *                        challenge, or 0 if unknown.
     * @param context Contains request context and methods to return 
     *                message buffers.
     *
     * @return: an SmAuthenticationResult object.
     */

    public SmAuthenticationResult
    authenticate(String parameter,
                 String secret,
                 int challengeReason,
                 SmAuthenticationContext context)
    {
        logw.println("RYA:authenticate() start reason="+challengeReason);
        // cannot do authentication without the authentication context
        if (null == context)
        {
            logw.println("RYA:authenticate() 1111");
            return new SmAuthenticationResult(
                   SmAuthStatus.SMAUTH_NO_USER_CONTEXT, 
                   SmAuthenticationResult.REASON_NONE);
        }

        // If the scheme is designed not to disambiguate users,
        //it should return SmAuthApi_NoUserContext.
        UserContext theUserContext = context.getUserContext();
        UserCredentialsContext testUserCredentialsContext = 
                               context.getUserCredentialsContext();

        String uid = null;
        String cookie = null;
        if (theUserContext != null) 
        {
            logw.println("UserContext is...");
            logw.println("    isUC="+theUserContext.isUserContext());
            logw.println("    username="+theUserContext.getUserName());
            uid = theUserContext.getUserName();
            logw.println("    userpath="+theUserContext.getUserPath());
            logw.println("    dirpath="+theUserContext.getDirPath());
            logw.println("    dirserver="+theUserContext.getDirServer());
            logw.println("    dirnamespace="+theUserContext.getDirNameSpace());
            logw.println("    sessionid="+theUserContext.getSessionID());
            //logw.println("    DnProp="+theUserContext.getDnProp());
        }
        if (null != testUserCredentialsContext)
        {
            cookie = testUserCredentialsContext.getPassword();
            logw.println("TestUserCredentialContext is...");
            logw.println("    username="+testUserCredentialsContext.getUserName());
            logw.println("    passwd="+testUserCredentialsContext.getPassword());
            logw.println("    dirpath="+testUserCredentialsContext.getDirPath());
            logw.println("    dirserver="+testUserCredentialsContext.getDirServer());
            logw.println("    dirnamespace="+testUserCredentialsContext.getDirNameSpace());
        }

        // Pass1 : Disambiguation Phase
        if ((null == theUserContext) || !theUserContext.isUserContext())
        {
            logw.println("RYA:authenticate() 222 usercontext=" + theUserContext);
            if (uid.startsWith(FMPREFIX)) {
                String fmuser = verifyFMToken(cookie);
                if (fmuser != null) {
                    context.setUserText(fmuser);
                    return new SmAuthenticationResult(
                            SmAuthStatus.SMAUTH_SUCCESS_USER_DN,
                            SmAuthenticationResult.REASON_NONE);
                } 
            }
            return new SmAuthenticationResult(
                   SmAuthStatus.SMAUTH_NO_USER_CONTEXT, 
                   SmAuthenticationResult.REASON_NONE);
        }

        // Pass2 : Authentication Phase

        // Reject the user if the password is not entered.
        UserCredentialsContext theUserCredentialsContext = 
                context.getUserCredentialsContext();

        if (null != theUserCredentialsContext)
        {
            logw.println("UserCredentialContext is...");
            uid = theUserCredentialsContext.getUserName();
            logw.println("    username="+theUserCredentialsContext.getUserName());
            logw.println("    passwd="+theUserCredentialsContext.getPassword());
            logw.println("    dirpath="+theUserCredentialsContext.getDirPath());
            logw.println("    dirserver="+theUserCredentialsContext.getDirServer());
            logw.println("    dirnamespace="+theUserCredentialsContext.getDirNameSpace());
        }
        if (null == theUserCredentialsContext)
        {
            logw.println("RYA:authenticate() 333");
            return new SmAuthenticationResult(SmAuthStatus.SMAUTH_REJECT, 
                SmAuthenticationResult.REASON_NONE);
        }

        String thePassword = theUserCredentialsContext.getPassword();

        if (thePassword.length() <= 0)
        {
            logw.println("RYA:authenticate() 444");
            return
                new SmAuthenticationResult(SmAuthStatus.SMAUTH_REJECT, 
                SmAuthenticationResult.REASON_NONE);
        }

        // Check if the user account is disabled.
        try
        {
            if (0 != Integer.parseInt(theUserContext.getProp("disabled")))
            {
                context.setUserText("User account is disabled.");

                return new SmAuthenticationResult(SmAuthStatus.SMAUTH_REJECT,
                        SmAuthenticationResult.REASON_USER_DISABLED);
            }
        }
        catch (NumberFormatException exc)
        {
            // Do nothing -- the user is not disabled
        }

        // authenticate the user
        String authUserText;

        if (!uid.startsWith(FMPREFIX)) 
        {
            try
            {
                authUserText = theUserContext.authenticateUser(thePassword);
                logw.println("RYA:authenticate() after calling authenticateUser " +
                        "password="+thePassword + " res="+authUserText + ":");
                if (theUserContext != null) 
                {
                    logw.println("AFTER UserContext is...");
                    logw.println("    isUC="+theUserContext.isUserContext());
                    logw.println("    username="+theUserContext.getUserName());
                    logw.println("    userpath="+theUserContext.getUserPath());
                    logw.println("    dirpath="+theUserContext.getDirPath());
                    logw.println("    dirserver="+theUserContext.getDirServer());
                    logw.println("    dirnamespace="+theUserContext.getDirNameSpace());
                    logw.println("    sessionid="+theUserContext.getSessionID());
                    //logw.println("    DnProp="+theUserContext.getDnProp());
                } 
            }
            catch (Throwable exc)
            {
            // insure subsequent code knows the authentication attempt failed
                authUserText = null;
            }
        } else
        {
            String fmuser = verifyFMToken(thePassword);
            if (fmuser != null) {
                authUserText = "";
                logw.println("RYA : FMToken is valid : not calling SM authenticate()");
            } else {
                authUserText = null;
                logw.println("RYA : FMToken is invalid : REJECT");
            }
        }

        if (null == authUserText)
        {
            context.setErrorText("Unable to authenticate user " 
                    + theUserContext.getUserName());

            return new SmAuthenticationResult(
                SmAuthStatus.SMAUTH_REJECT, SmAuthenticationResult.REASON_NONE);
        }

        // Set the time stamp of the user's last authentication.
        // For demonstration purposes, we will store the last login
        // in the "PIN" user property as a printable date&time string.

        String timeString = DateFormat.getDateTimeInstance(
               DateFormat.MEDIUM,DateFormat.MEDIUM).format(org.forgerock.openam.utils.Time.newDate());

        // put single quotes around the string if the user directory is "ODBC:"
        String theNameSpace = theUserContext.getDirNameSpace();

        if ((theNameSpace != null) && theNameSpace.equals("ODBC:"))
        {
            timeString = "'" + timeString + "'" ;
        }

        if (0 != theUserContext.setProp("pin", timeString))
        {
            context.setUserText("Failed to set the time stamp for " +
                    "user's profile attribute " + parameter);
        }

        // If a parameter is supplied, set it as app specific data
        if (parameter != null)
        {
            try
            {
                APIContext apiContext = context.getAPIContext();
                AppSpecificContext appContext = apiContext.getAppSpecificContext();
                appContext.setData(parameter.getBytes());
            }
            catch (NullPointerException exc)
            {
                context.setUserText("Failed to modify application specific context");
            }
            catch (SmJavaApiException exc)
            {
                context.setUserText("Failed to modify application specific context");
            }
        }

        return
            new SmAuthenticationResult(SmAuthStatus.SMAUTH_ACCEPT,
            SmAuthenticationResult.REASON_NONE);
    }

    private String verifyFMToken(String cookie)
    {
        cookie = cookie.substring(fmprefixLen, cookie.length());
        logw.println("Check if FM Token is valid"+cookie);
        logw.println(" Checking.. 1");
        SSOToken token = null;
        try 
        {
            SSOTokenManager manager = SSOTokenManager.getInstance();        
            token = manager.createSSOToken(cookie);          
            if (!manager.isValidToken(token)) {
               logw.println("FM Token is invalid");
               return null;
            }
            logw.println("Token is VALID :"+token.getPrincipal().getName());
            return token.getPrincipal().getName();
        } catch (Throwable ex2) {            
            logw.println("FM token is invalid" + ex2.getMessage());
            ex2.printStackTrace();
        }
        return null;
    }
}
