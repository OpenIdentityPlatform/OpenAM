/*
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
 */

package org.forgerock.identity.authentication.modules.common;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;

public abstract class AbstractEnhancedLoginModule extends AMLoginModule {
	/**
	 * 
	 */
	protected Map<Object, String> options;

	/**
	 * Original shared state
	 */
	protected Map<Object, Object> sharedState;
	
	/**
	 * Shallow copy of sharedState. Original managed shared state gets cleaned after successful login. 
	 */
	protected Map<Object, String> sharedStateCopy;

	/**
	 * Logger
	 */
	private final static Debug debug = Debug.getInstance(AbstractEnhancedLoginModule.class.getName());

	/**
	 * 
	 */
	protected ResourceBundle bundle;
	
	
	/**
	 * Gets current user username from login modules shared state
	 * @return
	 */
	protected String getUsername() {
		try {			
			return (String) sharedStateCopy.get(getUserKey());
		} catch (RuntimeException e) {
			debug.error("Unable to get userName: ", e);
			throw e;
		}
	}

	@Override
	public Principal getPrincipal() {
		return new CommonAuthenticationPrincipal(getUsername());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void init(Subject subject, Map sharedState, Map options) {
		debug.message("Initialization");
		this.options = options;
		
		this.sharedState = sharedState;
		this.sharedStateCopy = new HashMap<Object, String>(sharedState);
		
		bundle = amCache.getResBundle(getBundleName(), getLoginLocale());
	}

	@Override
	public int process(Callback[] arg0, int arg1) throws LoginException {
		return ISAuthConstants.LOGIN_SUCCEED;
	}
	
    public void destroyModuleState() {
        nullifyUsedVars();
    }

    public void nullifyUsedVars() {
        bundle = null;
        //share current shared state with next modules in chain
        this.sharedState.putAll(sharedStateCopy);
    }
    
    /**
     * Return configuration bundle name
     * @return
     */
    protected abstract String getBundleName();
    
    @SuppressWarnings({ "unchecked", "deprecation" })
	protected AMIdentity getIdentity(String uName) {
        AMIdentity theID = null;
        AMIdentityRepository amIdRepo = getAMIdentityRepository(getRequestOrg());

        IdSearchControl idsc = new IdSearchControl();
        idsc.setRecursive(true);
        idsc.setAllReturnAttributes(true);
        Set<AMIdentity> results = Collections.EMPTY_SET;
        
        try {
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
                    amIdRepo.searchIdentities(IdType.USER, uName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results == null || results.size() != 1) {
                throw new IdRepoException("getIdentity : More than one user found");
            }

            theID = results.iterator().next();
        } catch (IdRepoException e) {
            debug.error("Error searching Identities with username : " + uName, e);
        } catch (SSOException e) {
            debug.error("Module exception : ", e);
        }
        
        return theID;
    }
}
