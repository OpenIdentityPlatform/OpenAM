/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package ${packageName};

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import java.security.Principal;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MyModule extends AMLoginModule {

    private static final String MODULE_NAME = "MyModule";
    private static final String BUNDLE_NAME = "amAuthMyModule";
    private static final String AUTHLEVEL = "sunAMAuthMyModuleAuthLevel";
    private static final Debug DEBUG = Debug.getInstance(MODULE_NAME);
    private String authenticatedUser = null;
    private String userName = null;
    private Map currentConfig = null;
    private Map sharedState = null;

    /**
     * Constructor
     */
    public MyModule() {
        DEBUG.message("In MyModule.MyModule()");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        DEBUG.message("In MyModule.init()");
        this.sharedState = sharedState;
        currentConfig = options;

        amCache.getResBundle(BUNDLE_NAME, getLoginLocale());

        String authLevel = CollectionHelper.getMapAttr(options, AUTHLEVEL);
        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                DEBUG.error("Unable to set auth level " + authLevel, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        if (state == ISAuthConstants.LOGIN_START) {
            if (callbacks != null && callbacks.length != 0) {
                NameCallback nc = (NameCallback) callbacks[0];
                userName = nc.getName();
                PasswordCallback pwc = (PasswordCallback) callbacks[1];
                char[] password = pwc.getPassword();
                //store username password both in success and failure case
                storeUsernamePasswd(userName, new String(password));

                if (credentialsValid(userName, password)) {
                    authenticatedUser = userName;
                } else {
                    //In case of password failure ALWAYS throw InvalidPasswordException, so account lockout is aware of
                    //the authentication failure, and increments the failure count.
                    throw new InvalidPasswordException("Incorrect username/password", authenticatedUser);
                }
            }
            return ISAuthConstants.LOGIN_SUCCEED;
        }
        DEBUG.error("Error in MyModule.process()");
        throw new AuthLoginException(BUNDLE_NAME, "authFailed", null);
    }

    private boolean credentialsValid(String userName, char[] password) {
        //magic goes here
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getPrincipal() {
        if (authenticatedUser != null) {
            return new MyPrincipal(authenticatedUser);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyModuleState() {
        authenticatedUser = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void nullifyUsedVars() {
        currentConfig = null;
        sharedState = null;
        userName = null;
    }
}
