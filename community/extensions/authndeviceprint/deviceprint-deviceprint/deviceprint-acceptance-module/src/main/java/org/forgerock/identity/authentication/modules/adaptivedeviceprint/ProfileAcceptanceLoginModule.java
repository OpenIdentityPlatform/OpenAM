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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint;

import static org.forgerock.identity.authentication.modules.adaptivedeviceprint.ProfileAcceptanceConstants.PERSISTENT_COOKIE_VALUE_SESSION_ATTRIBUTE_NAME;
import static org.forgerock.identity.authentication.modules.adaptivedeviceprint.ProfileAcceptanceConstants.RESOURCE_BUNDLE_NAME;
import static org.forgerock.identity.authentication.modules.adaptivedeviceprint.ProfileAcceptanceConstants.STATE_ASK;
import static org.forgerock.identity.authentication.modules.adaptivedeviceprint.ProfileAcceptanceConstants.STATE_BEGIN;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.EmptyUserProfileNameException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.NotUniqueUserProfileException;
import org.forgerock.identity.authentication.modules.common.AbstractEnhancedLoginModule;
import org.forgerock.identity.authentication.modules.common.AdaptiveException;
import org.forgerock.identity.authentication.modules.common.config.AbstractObjectAttributesTransformer;
import org.forgerock.identity.authentication.modules.common.config.ConfigurationObjectTransformer;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

public class ProfileAcceptanceLoginModule extends AbstractEnhancedLoginModule {
	
	private static final Debug debug = Debug.getInstance(ProfileAcceptanceLoginModule.class.getName());

	
	
	private String headerText = "Do you want to store current profile?";
	
	private ProfileAcceptanceConfig config;
	private ProfileAcceptanceLogicIface profileAcceptanceLogic = new ProfileAcceptanceLogic();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void init(Subject subject, Map sharedState, Map options) {
		super.init(subject, sharedState, options);
		
		AbstractObjectAttributesTransformer configurationReader = new ConfigurationObjectTransformer(options);
		
		config = configurationReader.createObjectUsingAttributes(ProfileAcceptanceConfig.class);
		
		profileAcceptanceLogic.setAMIdentity(getIdentity(getUsername()));
	}

	@Override
	public int process(Callback[] callbacks, int state) throws LoginException {
		debug.message("ProfilAcceptanceModule, state: " + state);
		
		profileAcceptanceLogic.init(sharedState);
		
		try {
			if( profileAcceptanceLogic.proccedInit() ) {
				setNewCookieValueAsSessionAttribute();
				return ISAuthConstants.LOGIN_SUCCEED;
			}
		} catch (AdaptiveException e) {
			debug.error("AdaptiveException: " + e.getMessage());
		} 
		
		if (state == STATE_BEGIN) {
			replaceCallbacks(headerText, config.getDefaultStoringAnswer());

			return STATE_ASK;
		} else if( state == STATE_ASK ) {
			NameCallback name = (NameCallback) callbacks[0];
			ChoiceCallback stored = (ChoiceCallback) callbacks[1];

			boolean storeProfile = stored.getSelectedIndexes()[0] == 0;
			String profileName = getSuggestedProfileName(name);
			
			try {
				profileAcceptanceLogic.proccedStoring(profileName, storeProfile);
			} catch (NotUniqueUserProfileException e) {
				replaceCallbacks("User profile name has to be unique", storeProfile);
				return STATE_ASK;
			} catch (EmptyUserProfileNameException e) {
				replaceCallbacks("User profile cannot be blank", storeProfile);
				return STATE_ASK;
			}
			setNewCookieValueAsSessionAttribute();
			return ISAuthConstants.LOGIN_SUCCEED;
		} else {
			throw new AuthLoginException("invalid state");
		}
	}

	private String getSuggestedProfileName(NameCallback name) {
		String profileName = name.getName();
		if(profileName == null || profileName.equals("")) {
			profileName = profileAcceptanceLogic.getDefaultName();
		}
		return profileName;
	}
	
	private void setNewCookieValueAsSessionAttribute() {
		String persistentCookie = profileAcceptanceLogic.getNewPersistentCookieValue();
		if(persistentCookie != null) {
			getHttpServletRequest().getSession().setAttribute(PERSISTENT_COOKIE_VALUE_SESSION_ATTRIBUTE_NAME, persistentCookie);
		}
	}

	private void replaceCallbacks(String header, boolean stored) throws LoginException {
		substituteHeader(STATE_ASK, header);
		
		replaceCallback(STATE_ASK, 0, new NameCallback("Profile name", profileAcceptanceLogic.getDefaultName()));
		replaceCallback(STATE_ASK, 1, new ChoiceCallback("Store profile?", new String[] { "Yes", "No" }, stored ? 0 : 1, false));
	}
		
	@Override
	protected String getBundleName() {
		return RESOURCE_BUNDLE_NAME;
	}

}
