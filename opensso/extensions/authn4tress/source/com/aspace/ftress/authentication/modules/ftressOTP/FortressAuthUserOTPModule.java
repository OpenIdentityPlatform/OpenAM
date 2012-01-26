
/*
  * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  * - Redistributions of source code must retain the above copyright
  *   notice, this list of conditions and the following disclaimer.
  *
  * - Redistribution in binary form must reproduce the above copyright
  *   notice, this list of conditions and the following disclaimer in
  *   the documentation and/or other materials provided with the
  *   distribution.
  *
  * Neither the name of Sun Microsystems, Inc. or the names of
  * contributors may be used to endorse or promote products derived
  * from this software without specific prior written permission.
  *
  * This software is provided "AS IS," without a warranty of any
  * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
  * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
  * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
  * DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
  * DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN
  * OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA,
  * OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
  * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF
  * LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE,
  * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
  *
  * You acknowledge that Software is not designed, licensed or
  * intended for use in the design, construction, operation or
  * maintenance of any nuclear facility.
  */ 

/*
 * FortressAuthUserOTPModule.java
 *
 * Contains a class represents an AMLoginModule that handles 
 * authentication requests for OTP/EMV authentication schemes supported
 * by ActivIdentity's 4Tress AAA Server.
 *
 * Created on March 27, 2007, 10:34 AM
 *
 */

package com.aspace.ftress.authentication.modules.ftressOTP;


import java.io.IOException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import java.security.Principal;


import com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain;
import com.aspace.ftress.interfaces.ftress.DTO.ChannelCode;
import com.aspace.ftress.interfaces.ftress.DTO.UserCode;
import com.aspace.ftress.interfaces.ftress.DTO.DeviceAuthenticationRequest;
import com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode;
import com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse;

import com.aspace.ftress.interfaces.soap.Authenticator;
import com.aspace.ftress.interfaces.soap.Authenticator11SoapBindingStub;

import org.apache.axis.AxisFault;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;

import com.iplanet.am.util.Debug;
import com.iplanet.am.util.Misc;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import java.util.ResourceBundle;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A class that implements AMLoginModule.
 *
 * The class represents an AMLoginModule that handles authentication requests 
 * for OTP/EMV authentication schemes supported by ActivIdentity's 
 * 4Tress AAA Server.
 * 
 * @author Michelle Cope
 */
public class FortressAuthUserOTPModule extends AMLoginModule {
    
    
    private final static String amFortressName = "FortressAuthUserOTP";
    private static String localeName = "amAuthFortressUserOTP";
    private static Debug debug = Debug.getInstance(localeName);
    
    
    //Configuration variables.
    private Map options;
    private Map shareState;
    //private ResourceBundle bundle = null;
    private java.security.Principal userPrincipal = null;
    private String validatedUserID;
    
    
    //4Tress configuration attributes.
    public  String ftressURL;
    public  String securityDomain;
    public  String channelCode;
    public  final int AUTHENTICATION_RESPONSE_SUCCESSFUL = 1;
    public  String authenticationTypeCode;
    public  int authenticationModeSynchronous = 1;
    
    Authenticator authenticator;
    
    
    public FortressAuthUserOTPModule() {
        
    }
    
    /**
     * initAuthConfig() obtains and processes the 4Tress configuration attributes
     * for the authentication module.
     *
     * @throws AuthLoginException If any attribute is invalid
     */
    private void initAuthConfig() throws AuthLoginException {
        if (options != null) {
            if (debug.messageEnabled()) {
                debug.message("FortressAuthUserOTPModule: Retrieve Config."); 
            }
          
            String authLevel = 
        	Misc.getServerMapAttr(options, 
                    "Fortress-am-auth-AMUserOTPModule-auth-level");
            
            if (authLevel != null) {
                try {
                    
                    int tmp = Integer.parseInt(authLevel);
                    setAuthLevel(tmp);
                    
                } catch (Exception e) {
                    debug.error("Invalid auth level " + authLevel, e);
                    
                    String [] args5 = 
                        {"4Tress User OTP Module Authentication Level"};
                    
                    throw new AuthLoginException(localeName, 
                            "no_value_specified", 
                            args5);
                }
            } 
         
            ftressURL = Misc.getServerMapAttr(options,
                              "fortress-am-auth-4Tress-endpoint-url");
            securityDomain = Misc.getMapAttr(options,
                              "fortress-am-auth-4Tress-security-domain");
   	    channelCode = Misc.getMapAttr(options,
                              "fortress-am-auth-4Tress-channel-code");
            authenticationTypeCode = Misc.getMapAttr(options,
                              "fortress-am-auth-type-code");
            String authenticationModeString = Misc.getServerMapAttr(options,
                              "fortress-am-auth-mode-synchronous"); 
            
            if (authenticationModeString != null) {
               try {
                    authenticationModeSynchronous = 
                            Integer.parseInt(authenticationModeString);
                    
                } catch (Exception e) {
                    
                    if (debug.messageEnabled())
                        debug.error("Unallowed value for mode synchronous" + e);
                }
               
            } else {
                
                String [] args = {"AUTHENTICATION_MODE_STRING"};
                throw new AuthLoginException(localeName, "no_auth_mode", args);
            }
            
            if (ftressURL != null) {
                try {
                    URL url = new URL(ftressURL);
                } catch (Exception e) {
                    
                    if (debug.messageEnabled())
                        debug.error("Invalid URL syntax");
                    String [] args2 = {ftressURL};
                    throw new AuthLoginException(localeName, 
                            "invalid_url", 
                            args2, 
                            e);
                
                }
            }else {
                
                String [] args = {"4Tress URL Endpoint"};
                throw new AuthLoginException(localeName, 
                        "no_value_specified", 
                        args);
            
            }
            
            
            if (securityDomain == null || 
                   channelCode == null ||
                   authenticationTypeCode == null){
                String [] args3 = {"4Tress Security Domain", 
                "4Tress Channel Code", 
                "4Tress Authentication Type Code"
                };
                
                throw new AuthLoginException(localeName, "missing_value", args3);
            }    
            
            if (debug.messageEnabled()) {
                debug.message("AuthN Module Initialized.\n4TressEndPointUrl=" + 
                        ftressURL +
                    "\n\t4TressAuthenticationModeString = " + 
                        authenticationModeString +
                    "\n\t4Tress Security Domain Parameter=" + securityDomain +
                    "\n\t4Tress Channel Code=" + channelCode +
                    "\n\t4Tress Authentication Type Code Parameter=" + 
                        this.authenticationTypeCode);
            }
            
        } else {
          
            if (debug.messageEnabled()) {
                debug.error("Initialization Options Parameter is null." + 
                        "Failed to Initialized Authentication Module");
            }
            String [] args4 = {amFortressName};
            throw new AuthLoginException(localeName, "no_options", args4);
            
        }
    }
    
    /** 
     * Initializes the Login Module.
     *
     * @see com.sun.identity.authentication.spi.AMLoginModule#init(Subject, Map, Map)
     */
    public void init(Subject subject, Map sharedState, Map options) {
        
        if (debug.messageEnabled()) {
                debug.message("FortressAMLoginModule initialization");
                debug.message("Modified Version");
        }
        
        
    	java.util.Locale locale = getLoginLocale();
        
        /** 
         * Change: Removed because in FAM 8.0v1 Build 1, error thrown:
         * java.lang.NoSuchFieldError: amCache
         */
    	//bundle = amCache.getResBundle(localeName, locale);
        this.shareState = sharedState;
        this.options = options;
        
    	if (debug.messageEnabled()) {
            debug.message("FortressAMLoginModule initialization completed");
        }
        
        
    }
    
    
    /**
     * Internal method to convert a char array to an equivalent java.lang.String
     * @param tmpPassword The password as a char[] type
     * @param callback The password callback
     * @return A java.lang.String representation of tmpPassword
     */
    private String charToString(char[] tmpPassword, Callback callback) {
        
        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }
        
        char[] pwd = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, pwd, 0, tmpPassword.length);
        ((PasswordCallback) callback).clearPassword();
        return new String(pwd);
        
    }
    
    /**
     * Returns an int value which indicates a success authentication (-1) 
     * or failed authentication (not -1).
     *
     * This method will interface with 4Tress AAA Server to perform the 
     * authentication. The method extracts a set of configuration attributes 
     * from the 4Tress AM AuthN module configuration attribute map;
     * initializes a
     * com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequest object
     * which is sent to 4Tress; and then processes the returned
     * com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.
     * 
     * @param callbacks The callbacks argument specifies all 
     *                  callbacks that must be processed.
     * @param state The state value specifies the order of state
     * @return An int value indicating the outcome of the authentication request
     * @throws AuthLoginException If any 4Tress Configuration Attributes are 
     *                              invalid or authentication fails.
     * @see com.sun.identity.authentication.spi.AMLoginModule#process(Callback,int)
     */
    public int process(Callback[] callbacks, int state)
            throws AuthLoginException {
         
        boolean success;
        String userName;
        String userPassword;
        int currentState = state;
        int newState = 0;
        
        try {
            initAuthConfig();
            
                
                if (callbacks != null) {
                    userName = ((NameCallback)callbacks[0]).getName();
                    userPassword = charToString(((PasswordCallback)callbacks[1]).getPassword(),
                                    callbacks[1]);
                    
                } else {
                    
                    String [] args = {"Callback Handler"};
                    throw new AuthLoginException(localeName, 
                            "callback_error",
                            args);
               
                }
                   
                    SecurityDomain securityDomain = new SecurityDomain();
                    
                    securityDomain.setDomain(this.securityDomain.trim());
                    //Additional For 4Tress 3.5
                    securityDomain.setCode(this.securityDomain.trim());
                    
                    
                    ChannelCode cCode = new ChannelCode();
                    cCode.setCode(this.channelCode.trim());
                    
                    DeviceAuthenticationRequest authRequest = 
                            new DeviceAuthenticationRequest();

                    UserCode userCode = new UserCode();
                    userCode.setCode(userName);
                    
                    
                    AuthenticationTypeCode atCode = new AuthenticationTypeCode();
                    atCode.setCode(authenticationTypeCode.trim());
                    
                    
                    authRequest.setAuthenticateNoSession(false);
                    authRequest.setOneTimePassword(userPassword);
                    authRequest.setUserCode(userCode);
                    authRequest.setAuthenticationTypeCode(atCode);
                    authRequest.setAuthenticationMode(authenticationModeSynchronous);
                    
                        
                    if (debug.messageEnabled()) {
                        
                            debug.message("4Tress UserCode is " + 
                                    authRequest.getUserCode().toString());
                            debug.message("4Tress AuthTypeCode is " + 
                                    authRequest.getAuthenticationTypeCode());
                            debug.message("4Tress Authentication Mode is " + 
                                    authRequest.getAuthenticationMode());
                            debug.message("4Tress Securitydomain is " + 
                                    securityDomain.getDomain());
                            debug.message("4Tress Channel Code " +
                                    cCode.getCode());
                    }

                    AuthenticationResponse authResponse = null;
                     
                    if (debug.messageEnabled()) {
                    debug.message(amFortressName + 
                            ": Sending 4Tress Authentication Request");
                    }
                    
                    authResponse = 
                            getAuthenticator().primaryAuthenticateDevice(cCode, 
                                                authRequest,
                                                securityDomain);
                    
                    
                    success = 
                            (authResponse.getResponse() == AUTHENTICATION_RESPONSE_SUCCESSFUL);
                    
                    if (debug.messageEnabled()) {
                        debug.message(amFortressName + ": Retrieved response" +
                                "Response was" + success);
                    }
           
                    if (!success) {
                        throw new AuthLoginException(amFortressName +
                                authResponse.getMessage());
                    
                    } else {
                        
                        if (debug.messageEnabled()) {
                            debug.message(this.amFortressName +
                                    "Successfully Authenticated");
                        }
                        
                        validatedUserID = userName;
                        return -1;
                    }
                
            
        } catch (Throwable e) {
            debug.error(amFortressName + "Failed to process AuthN Request ", e);
            throw new AuthLoginException(localeName, "Process_Failed", null);
        }
        
     }
     
   /**
    * Retrieves a reference to the 4Tress AAA Server endpoint that handles
    * authentication.
    *
    * @return A reference to the 4Tress AAA Server Authentication endpoint.
    * @throws MalformedURLException If the url endpoint is invalid.
    */
   protected Authenticator getAuthenticator() throws MalformedURLException,
           AxisFault {
        
        if (authenticator == null) {
            authenticator = 
                new Authenticator11SoapBindingStub(new java.net.URL(ftressURL),
                    null);
        }
        
        return authenticator;
    }

    /*Returns the user Principal object
     *
     *@return User Principal
     *@see com.sun.identity.authentication.spi.AMLoginModule#getPrincipal()
     */
    public java.security.Principal getPrincipal() {
        
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (validatedUserID != null) {
            userPrincipal = new FortressModulePrincipal(validatedUserID);
            return userPrincipal;
        } else {
            return null;
        }
        
    }
    
    /**
     * Destroys state fields that should not be maintained across 
     * authentication requests
     * 
     * @see com.sun.identity.authentication.spi.AMLoginModule#destroyModuleState()
     */
     public void destroyModuleState() {
         
        validatedUserID = null;
        userPrincipal = null;
        
    }
    
    
}
