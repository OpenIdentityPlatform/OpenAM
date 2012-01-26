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
 * $Id: YubikeyLoginModule.java,v 1.2 2009/08/03 22:27:08 superpat7 Exp $
 *
 */

package com.sun.identity.authentication.modules.yubikey;


import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import javax.security.auth.*;
import javax.security.auth.callback.*;

/**
 * @author Pat Patterson
 */
public class YubikeyLoginModule extends AMLoginModule {
    // TODO - read authSvcUrl, clientId and yubikeyIdAttrName from configuration
    private String authSvcUrl = "https://api.yubico.com/wsapi/verify";
    private int clientId = 1;
    private String yubikeyIdAttrName = "employeeNumber";

    private java.security.Principal userPrincipal = null;
    private String userName = null;
    private String otp = null;
    private static int YUBIKEY_ID_LEN = 12;
    
    private boolean getCredentialsFromSharedState;
    private Map sharedState;

    protected String validatedUserID;
    protected static Debug debug = Debug.getInstance("YubikeyLoginModule");

    /**
     * initialize this object
     *
     * @param subject
     * @param sharedState
     * @param options
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        this.sharedState = sharedState;
        if (debug.messageEnabled()) {
            debug.message("YubikeyLoginModule.init()");
        }
    }
    
    /**
     * This method authenticates the subject.
     * We require the user to supply a username so we can check that this is the
     * correct Yubikey for that user, otherwise, anyone could pick up someone's
     * Yubikey and log in as that person without even knowing who's key it was.
     *
     * @param callbacks the array of callbacks from the module configuration file
     * @param state the current state of the authentication process
     * @throws AuthLoginException if an error occurs
     */
    @Override
    public int process(Callback[] callbacks,int state)
        throws AuthLoginException {

        if (state == ISAuthConstants.LOGIN_START) {
            try {
                if (callbacks !=null && callbacks.length == 0) {
                    userName = (String) sharedState.get(getUserKey());
                    otp = (String) sharedState.get(getPwdKey());
                    if (userName == null || otp == null) {
                        return ISAuthConstants.LOGIN_START;
                    }
                    getCredentialsFromSharedState = true;
                } else {
                    // this module is married to the module properties file
                    // therefore the number of callbacks must match
                    if(callbacks.length != 2) {
                        throw new AuthLoginException("fatal configuration error, "+
                            "wrong number of callbacks");
                    }
                    userName = ((NameCallback)callbacks[0]).getName();
                    if(userName == null || userName.equals("")) {
                        throw new AuthLoginException("Username cannot be empty");
                    }

                    otp = new String(((PasswordCallback)callbacks[1]).getPassword());
                    if(otp == null || otp.equals("")) {
                        throw new AuthLoginException("OTP cannot be empty");
                    }
                }

                // Check that presented Yubikey ID matches saved ID for user
                //
                // First, set up the search
                AMIdentityRepository idrepo = getAMIdentityRepository(
                    getRequestOrg());

                // We want the Yubikey ID attribute
                IdSearchControl ctrl = new IdSearchControl();
                Set attributeNames = new HashSet<String>();
                attributeNames.add(yubikeyIdAttrName);
                ctrl.setReturnAttributes(attributeNames);

                // Now do the search
                IdSearchResults results = null;
                try {
                    results = idrepo.searchIdentities(IdType.USER, userName, ctrl);
                } catch (IdRepoException ex) {
                    throw new AuthLoginException("Exception looking up username: " +
                        userName, ex);
                } catch (SSOException ex) {
                    throw new AuthLoginException("Exception looking up username: " +
                        userName, ex);
                }

                // Get the results
                Set resultSet = results.getSearchResults();

                // Should be only one result
                if ( resultSet.size() != 1 ) {
                    throw new AuthLoginException("Found " + resultSet.size() +
                        " users with name " + userName);
                }

                // Get the user object
                AMIdentity user = (AMIdentity)resultSet.iterator().next();

                // attrMap is a Map of attribute names to sets of values
                Map attrMap = (Map)results.getResultAttributes().get(user);

                // attrSet is the set of values for the Yubikey ID attribute
                Set attrSet = (Set)attrMap.get(yubikeyIdAttrName);

                // Assume there is only one Yubikey ID per user
                String savedId = (String)attrSet.iterator().next();

                // Check that the OTP matches the ID on record for this user
                if ( ! otp.startsWith(savedId)) {
                    throw new AuthLoginException("Presented Yubikey ID (" +
                        otp.substring(0, YUBIKEY_ID_LEN) + ") for user " +
                        userName + "does not match saved ID (" +  savedId + ")");
                }

                // Now check that OTP is valid
                YubikeyWebServiceClient yubikeyWSC =
                    new YubikeyWebServiceClient(authSvcUrl, clientId);
                boolean verified = false;
                try {
                    verified = yubikeyWSC.validateToken(otp);
                } catch(Exception ex) {
                    throw new AuthLoginException("Exception receiving response", ex);
                }

                if(! verified) {
                    throw new AuthLoginException("Login failure");
                }

                // If we get here then all is well
                validatedUserID = userName;
            }  catch ( AuthLoginException ale ) {
                if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                    getCredentialsFromSharedState = false;
                    return ISAuthConstants.LOGIN_START;
                }
                throw ale;
            }
        }

        return ISAuthConstants.LOGIN_SUCCEED;
    }

    /**
     * return the Principal object,
     * creating it if necessary. This method
     * is invoked at the end of successful
     * authentication session. relies on
     * userTokenID being set by process()
     *
     * <p>
     *
     * @return the Principal object or null if not verified
     */
    @Override
    public java.security.Principal getPrincipal() {
        if (validatedUserID != null && userPrincipal == null) {
            userPrincipal = new YubikeyPrincipal(validatedUserID);
        }
        return userPrincipal;
        
    }

    /**
     * Cleans up state fields.
     */
    @Override
    public void destroyModuleState() {
        validatedUserID = null;
        userPrincipal = null;
    }

    /**
     * TODO-JAVADOC
     */
    @Override
    public void nullifyUsedVars() {
        userName = null ;
        otp = null;
        sharedState = null;
    }
}