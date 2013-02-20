/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SwekeyLoginModule.java,v 1.2 2009/03/03 22:48:07 superpat7 Exp $
 *
 */

package com.sun.identity.authentication.modules.swekey;


import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
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
public class SwekeyLoginModule extends AMLoginModule {
    private String swekeyIdAttrName = "employeeNumber";

    private java.security.Principal userPrincipal = null;
    private String userName = null;
    private String id = null;
    private String rt = null;
    private String otp = null;
    private boolean verified = false;

    protected static final Debug debug = Debug.getInstance("SwekeyLoginModule");

    /**
     * initialize this object
     *
     * @param subject
     * @param sharedState
     * @param options
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        if (debug.messageEnabled()) {
            debug.message("SwekeyLoginModule.init()");
        }
    }
    
    /**
     * This method authenticates the subject.
     * We require the user to supply a username so we can check that this is the
     * correct Swekey for that user, otherwise, anyone could pick up someone's
     * Swekey and log in as that person without even knowing who's key it was.
     *
     * @param callbacks the array of callbacks from the module configuration file
     * @param state the current state of the authentication process
     * @throws AuthLoginException if an error occurs
     */
    @Override
    public int process(Callback[] callbacks,int state)
        throws AuthLoginException {
        // this module is married to the module properties file
        // therefore the number of callbacks must match
        if(callbacks.length != 4) {
            throw new AuthLoginException("fatal configuration error, "+
                "wrong number of callbacks");
        }

        if(state == 1) {
            userName = ((NameCallback)callbacks[0]).getName();
            if(userName == null || userName.equals("")) {
                throw new AuthLoginException("Username cannot be empty");
            }

            id = new String(((PasswordCallback)callbacks[1]).getPassword());
            if(id == null || id.equals("")) {
                throw new AuthLoginException("ID cannot be empty");
            }

            rt = new String(((PasswordCallback)callbacks[2]).getPassword());
            if(rt == null || rt.equals("")) {
                throw new AuthLoginException("RT cannot be empty");
            }

            otp = new String(((PasswordCallback)callbacks[3]).getPassword());
            if(otp == null || otp.equals("")) {
                throw new AuthLoginException("OTP cannot be empty");
            }

            // Check that presented Swekey ID matches saved ID for user
            //
            // First, set up the search
            AMIdentityRepository idrepo = getAMIdentityRepository(
                getRequestOrg());

            // We want the Swekey ID attribute
            IdSearchControl ctrl = new IdSearchControl();
            Set attributeNames = new HashSet<String>();
            attributeNames.add(swekeyIdAttrName);
            ctrl.setReturnAttributes(attributeNames);

            // Now do the search
            IdSearchResults results;
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

            // attrSet is the set of values for the Swekey ID attribute
            Set attrSet = (Set)attrMap.get(swekeyIdAttrName);

            // Assume there is only one Swekey ID per user
            String savedId = (String)attrSet.iterator().next();

            // Check that the OTP matches the ID on record for this user
            if ( ! id.equals(savedId)) {
                throw new AuthLoginException("Presented Swekey ID (" + id +
                    ") for user " + userName + "does not match saved ID (" +
                    savedId + ")");
            }

            // Now check that OTP is valid
            verified = SwekeyWebServiceClient.checkOtp(id, rt, otp);

            if(! verified) {
                throw new AuthLoginException("Login failure");
            }
            // If we get here then all is well
        }
        return -1; // -1 indicates success
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
        if(verified && userPrincipal == null) {
            userPrincipal = new SwekeyPrincipal(userName);
        }
        return userPrincipal;
        
    }
}