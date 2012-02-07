/**
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
 * $Id: WSSAuthModule.java,v 1.2 2008/11/18 00:02:25 mallas Exp $
 *
 */

package com.sun.identity.authentication.modules.wss;

import java.util.StringTokenizer;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import java.security.AccessController;
import java.security.Principal;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.AdminTokenAction;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.wss.security.UserNameToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.authentication.spi.InvalidPasswordException;


/**
 * Authentication module for web services user name token profile.
 * This module handles both password digest and plain authentication
 * mechanisms and authenticates the users configured via the
 * configured user repository.
 */

public class WSSAuthModule extends AMLoginModule {

    protected Principal userPrincipal;
    private String userId = null;
    private String userSearchAttribute = null;
    private String userPasswordAttribute = null;
    private String realm = null;
    private static final String UN_SEARCH_ATTRIBUTE = 
            "sunWebservicesUserSearchAttribute";
    private static final String REALM = "sunWebServicesUserRealm";
    
    private static final String UN_PASSWORD_ATTRIBUTE =
            "sunWebservicesUserpasswordAttribute";
    
    private static Debug debug = Debug.getInstance("WebServicesSecurity");
    private static ResourceBundle bundle = 
            ResourceBundle.getBundle("fmWSSecurity");
            
    
    public WSSAuthModule() throws LoginException {
        if(debug.messageEnabled()) {
           debug.message("WSSAuthModule()"); 
        }	
    }

    /**
     * Initialize the authentication module with it's configuration
     */
    public void init(Subject subject, Map sharedState, Map options) {
        if(debug.messageEnabled()) {
	   debug.message("WSSAuthModule initialization" + options);
        }
        userSearchAttribute = CollectionHelper.getMapAttr(options, 
                              UN_SEARCH_ATTRIBUTE, "uid");                
        userPasswordAttribute = CollectionHelper.getMapAttr(options, 
                              UN_PASSWORD_ATTRIBUTE, "userPassword");        
        realm = CollectionHelper.getMapAttr(options, REALM, "/");
        
        if(debug.messageEnabled()) {        
           debug.message("WSSAuthModule.init: User search attribute= " + 
                             userSearchAttribute + "\n" +
                          " User password attribute=" +
                            userPasswordAttribute + "\n" +
                          " Realm = " + realm);
        }
    } 

    public int process(Callback[] callbacks, int state) 
                 throws AuthLoginException {

        HttpServletRequest request = getHttpServletRequest();
        
        String userName = ( (NameCallback) callbacks[0]).getName();
        String digest = null;
        String nonce = null;
        String timestamp = null;
        String passwdCallback = charToString(((PasswordCallback)
                    callbacks[1]).getPassword(), callbacks[1]);
        if((passwdCallback == null) || (passwdCallback.length() == 0)) {
            throw new InvalidPasswordException(
                    bundle.getString("invalidPassword"));
        }
        
        boolean isPasswordDigest = false;
        if(passwdCallback.indexOf(";") != -1) {
           isPasswordDigest = true;
        }
        
        if(isPasswordDigest) {
           if(debug.messageEnabled()) {
              debug.message("WSSAuthModule.process: In password digest");
           }  
           StringTokenizer st = new StringTokenizer(passwdCallback, ";");
           while(st.hasMoreTokens()) {
               String tmp = st.nextToken();
               if(tmp.indexOf("PasswordDigest") != -1) {
                  digest = tmp.substring(("PasswordDigest=".length()));
               } else if(tmp.indexOf("Nonce=") != -1) {
                  nonce = tmp.substring(("Nonce=".length()));
               } else if(tmp.indexOf("Timestamp=") != -1) {
                  timestamp = tmp.substring(("Timestamp=".length()));
               }
           }
           
           if(debug.messageEnabled()) {
              debug.message("WSSAuthModule: Digest =" + digest +
                      " Nonce = " + nonce +
                      " Timestamp = " + timestamp);
           }
           AMIdentity amIdentity = searchUser(userSearchAttribute, userName);
           try {
               Set attrValues = amIdentity.getAttribute(userPasswordAttribute);
               if(attrValues == null || attrValues.isEmpty()) {
                  throw new AuthLoginException(
                          bundle.getString("nullUserPassword"));
               }
               String configAttrValue = (String)attrValues.iterator().next();
               String calculatedDigest = 
                   UserNameToken.getPasswordDigest(configAttrValue, 
                   nonce, timestamp);
               if(calculatedDigest.equals(digest)) {
                  userId = amIdentity.getUniversalId();
                  if(debug.messageEnabled()) {
                     debug.message("WSSAuthModule: Login succeeded for " 
                             + userId);
                  }
                  return ISAuthConstants.LOGIN_SUCCEED; 
               } else {
                  if(debug.messageEnabled()) {
                     debug.message("WSSAuthModule: Digest does not match" +
                             " Expected digest: " + calculatedDigest +
                             " Digest sent: " + digest); 
                  } 
               }
           } catch (Exception e) {
               debug.error("WSSAuthModule.process: exception", e);
               throw new AuthLoginException(
                       bundle.getString("authenticationFailed"));    
           }
               
           
        } else {
           AMIdentity amIdentity = searchUser(userSearchAttribute, userName);           
           if(amIdentity != null) {
              try {
                  Set attrValues = amIdentity.getAttribute(
                          userPasswordAttribute);
                  if(attrValues == null || attrValues.isEmpty()) {
                     throw new AuthLoginException(
                             bundle.getString("nullUserPassword"));                             
                  }
                  String configAttrValue = (String)attrValues.iterator().next();
                  if(passwdCallback.equals(configAttrValue)) {
                     userId = amIdentity.getUniversalId();
                     if(debug.messageEnabled()) {
                        debug.message("WSSAuthModule. Authentication" +
                                " succeeded for " + userId);
                     }
                     return ISAuthConstants.LOGIN_SUCCEED;         
                  }
              } catch (Exception ex) {
                  debug.error("WSSAuthModule.process: Idrepo exception", ex);
                  throw new AuthLoginException(
                          bundle.getString("authenticationFailed"));
              }                                            
           } else {
              throw new InvalidPasswordException (
                      bundle.getString("noUserFound"));
           }
        }
        throw new InvalidPasswordException(
                          bundle.getString("authenticationFailed"));
        

    }
    
     private String charToString(char[] tmpPassword, Callback cbk) {
        if (tmpPassword == null) {
            // treat a NULL password as an empty password
            tmpPassword = new char[0];
        }
        char[] pwd = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, pwd, 0, tmpPassword.length);
        ((PasswordCallback) cbk).clearPassword();
        return new String(pwd);
    }
     
    private static SSOToken getAdminToken() {                      
         return (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
    }
    
    private AMIdentity searchUser(String attr,
            String attrValue) throws AuthLoginException {
        try {
            if(debug.messageEnabled()) {
               debug.message("WSSAuthModule. Search attr:" + attr +
                    " Attr value: " + attrValue);
            }
            AMIdentityRepository idRepo                        
                     = new AMIdentityRepository(getAdminToken(), realm);               
            IdSearchControl control = new IdSearchControl();
            control.setAllReturnAttributes(true);
            control.setTimeOut(0);

            Map kvPairMap = new HashMap();
            Set set = new HashSet();
            set.add(attrValue);
            kvPairMap.put(attr, set);
            control.setSearchModifiers(IdSearchOpModifier.OR, kvPairMap);

            IdSearchResults results = idRepo.searchIdentities(IdType.USER,
                    "*", control);
            Set users = results.getSearchResults();
            if(users == null || users.isEmpty()) {
               if(debug.messageEnabled()) {
                  debug.message("WSSAuthModule. No user found with "
                          + attrValue);
               }
               throw new InvalidPasswordException(
                       bundle.getString("noUsersFound"));
            }
            return (AMIdentity)users.iterator().next();       
        } catch (Exception ex) {
            debug.error("WSSAuthModule.searchUser: ", ex);
            throw new AuthLoginException(bundle.getString("userSearchFailed"));
        }
    }

    /**
     * Returns the authenticated principal.
     * This is consumed by the authentication framework to set the 
     * principal
     */
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userId != null) {
            userPrincipal = new WSSUserPrincipal(userId);
            return userPrincipal;
        } else {
            return null;
        }
    }
}
