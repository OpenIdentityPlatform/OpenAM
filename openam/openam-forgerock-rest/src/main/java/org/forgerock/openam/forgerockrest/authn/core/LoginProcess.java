/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn.core;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.sm.DNMapper;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;

/**
 * This class represents the login process, maintaining the current state of the login process and providing
 * operations to continue to the next stage of the login process to the completion of the login process.
 *
 * This class is stateless and uses the AuthContextLocal to provide any state required. Therefore a LoginProcess
 * object can be created on a per request basis, as long as the underlying AuthContextLocal is provided.
 */
public class LoginProcess {

    private final LoginAuthenticator loginAuthenticator;
    private final LoginConfiguration loginConfiguration;
    private final AuthContextLocalWrapper authContext;

    /**
     * Constructs an instance of the LoginProcess.
     *
     * @param loginAuthenticator An instance of the LoginAuthenticator.
     * @param loginConfiguration The LoginConfiguration object used to start the login process.
     * @param authContext The underlying AuthContextLocal object wrapped as a AuthContextLocalWrapper.
     */
    public LoginProcess(LoginAuthenticator loginAuthenticator, LoginConfiguration loginConfiguration,
            AuthContextLocalWrapper authContext) {
        this.loginAuthenticator = loginAuthenticator;
        this.loginConfiguration = loginConfiguration;
        this.authContext = authContext;
    }

    /**
     * Returns the LoginStage representing whether the login process still requires callbacks to be submitted
     * or has finished with a status define in AuthContext.Status.
     *
     * @return The login stage.
     */
    public LoginStage getLoginStage() {

        boolean isLevelOrCompositeIndexType = AuthIndexType.LEVEL.equals(loginConfiguration.getIndexType()) ||
                AuthIndexType.COMPOSITE.equals(loginConfiguration.getIndexType());

        if ((isLevelOrCompositeIndexType && authContext.getRequirements() == null) || !isLevelOrCompositeIndexType) {
            authContext.hasMoreRequirements();
        }

        if (AuthContext.Status.IN_PROGRESS.equals(authContext.getStatus())) {
            return LoginStage.REQUIREMENTS_WAITING;
        } else {
            return LoginStage.COMPLETE;
        }
    }

    /**
     * Returns the array of Callbacks for the current step in the login process.
     *
     * @return The array of Callbacks.
     */
    public Callback[] getCallbacks() {
        return authContext.getRequirements();
    }

    /**
     * Returns the PagePropertiesCallback from the current array of Callbacks.
     *
     * If no PagePropertiesCallback exists then null is returned.
     *
     * @return The PagePropertiesCallback or null.
     */
    public PagePropertiesCallback getPagePropertiesCallback() {

        PagePropertiesCallback pagePropertiesCallback = null;

        for (Callback callback : authContext.getRequirements(true)) {
            if (callback instanceof PagePropertiesCallback) {
                pagePropertiesCallback = (PagePropertiesCallback) callback;
                break;
            }
        }

        return pagePropertiesCallback;
    }

    /**
     * Moves the login process to the next step, which means either submitting the callbacks or if the AuthIndexType
     * is LEVEL or COMPOSITE then instead of submitting the callbacks, the callbacks are assumed to be the
     * authentication module choice login step and the resulting choice will be used to continue the login process.
     *
     * @param callbacks The array of Callbacks to process.
     * @return The LoginProcess to use to continue the login process.
     * @throws com.sun.identity.authentication.spi.AuthLoginException If there is a problem processing the login.
     */
    public LoginProcess next(Callback[] callbacks) throws AuthLoginException {

        AuthIndexType indexType = authContext.getIndexType();
        if (indexType == null) {
            indexType = loginConfiguration.getIndexType();
        }

        if (AuthIndexType.LEVEL.equals(indexType) || AuthIndexType.COMPOSITE.equals(indexType)) {

            String choice = null;

            for (Callback responseCallback : callbacks) {
                if (responseCallback instanceof ChoiceCallback) {
                    int selectedIndex = ((ChoiceCallback) responseCallback).getSelectedIndexes()[0];
                    choice = ((ChoiceCallback) responseCallback).getChoices()[selectedIndex];
                    break;
                }
            }

            String indexValue = AMAuthUtils.getDataFromRealmQualifiedData(choice);
            String qualifiedRealm = AMAuthUtils.getRealmFromRealmQualifiedData(choice);
            if ((qualifiedRealm != null) && (qualifiedRealm.length() != 0)) {
                String orgDN = DNMapper.orgNameToDN(qualifiedRealm);
                authContext.getAuthContext().setOrgDN(orgDN);
            }

            int type = AuthUtils.getCompositeAdviceType(authContext.getAuthContext());

            if (type == AuthUtils.MODULE) {
                indexType = AuthIndexType.MODULE;
            } else if (type == AuthUtils.SERVICE) {
                indexType = AuthIndexType.SERVICE;
            } else if (type == AuthUtils.REALM) {
                indexType = AuthIndexType.SERVICE;
                String orgDN = DNMapper.orgNameToDN(choice);
                indexValue = AuthUtils.getOrgConfiguredAuthenticationChain(orgDN);
                authContext.getAuthContext().setOrgDN(orgDN);
            } else {
                indexType = AuthIndexType.MODULE;
            }

            loginConfiguration.indexType(indexType);
            loginConfiguration.indexValue(indexValue);

            return loginAuthenticator.startLoginProcess(this);

        } else {
            authContext.submitRequirements(callbacks);
            return this;
        }
    }

    /**
     * Returns true if the outcome of the login process is SUCCESS.
     *
     * @return If the login process was successful.
     */
    public boolean isSuccessful() {
        return AuthContext.Status.SUCCESS.equals(authContext.getStatus());
    }

    /**
     * Returns the underlying AuthContextLocal.
     *
     * @return The AuthContextLocal wrapped as a AuthContextLocalWrapper.
     */
    public AuthContextLocalWrapper getAuthContext() {
        return authContext;
    }

    /**
     * Returns the LoginConfiguration object used to configure the login process.
     *
     * @return The LoginConfiguration object.
     */
    public LoginConfiguration getLoginConfiguration() {
        return loginConfiguration;
    }
}
