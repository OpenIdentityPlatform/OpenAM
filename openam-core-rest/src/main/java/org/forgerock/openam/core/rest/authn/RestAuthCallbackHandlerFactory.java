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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.authn;

import com.google.inject.Singleton;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.NameValueOutputCallback;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.spi.X509CertificateCallback;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthChoiceCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthConfirmationCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthHiddenValueCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthHttpCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthLanguageCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthNameCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthNameValueOutputCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthPasswordCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthPollingWaitCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthRedirectCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthTextInputCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthTextOutputCallbackHandler;
import org.forgerock.openam.core.rest.authn.callbackhandlers.RestAuthX509CallbackHandler;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import java.text.MessageFormat;

/**
 * Factory class for getting the appropriate RestAuthCallbackHandlers for the given Callbacks.
 */
@Singleton
public class RestAuthCallbackHandlerFactory {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    /**
     * Singleton approach by using a static inner class.
     */
    private static final class SingletonHolder {
        private static final RestAuthCallbackHandlerFactory INSTANCE = new RestAuthCallbackHandlerFactory();
    }

    /**
     * Private constructor to ensure RestAuthCallbackHandlerFactory remains a Singleton.
     */
    @Inject
    private RestAuthCallbackHandlerFactory() {
    }

    /**
     * Gets the RestAuthCallbackHandlerFactory instance.
     *
     * @return The RestAuthCallbackHandlerFactory singleton instance.
     */
    public static RestAuthCallbackHandlerFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Retrieves the appropriate RestAuthCallbackHandlers for the given Callback class.
     *
     * @param callbackClass The class of the Callback to get the RestAuthCallbackHandler for.
     * @param <T> A class which implements the Callback interface.
     * @return The RestAuthCallbackHandler for the given Callback class.
     */
    public <T extends Callback> RestAuthCallbackHandler getRestAuthCallbackHandler(Class<T> callbackClass) throws RestAuthException {

        if (HiddenValueCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthHiddenValueCallbackHandler();
        } else if (NameCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthNameCallbackHandler();
        } else if (PasswordCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthPasswordCallbackHandler();
        } else if (ChoiceCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthChoiceCallbackHandler();
        } else if (ConfirmationCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthConfirmationCallbackHandler();
        } else if (HttpCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthHttpCallbackHandler();
        } else if (LanguageCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthLanguageCallbackHandler();
        } else if (RedirectCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthRedirectCallbackHandler();
        } else if (TextInputCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthTextInputCallbackHandler();
        } else if (TextOutputCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthTextOutputCallbackHandler();
        } else if (X509CertificateCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthX509CallbackHandler();
        } else if (PollingWaitCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthPollingWaitCallbackHandler();
        } else if (NameValueOutputCallback.class.isAssignableFrom(callbackClass)) {
            return new RestAuthNameValueOutputCallbackHandler();
        }

        DEBUG.error(MessageFormat.format("Unsupported Callback, {0}", callbackClass.getSimpleName()));
        throw new RestAuthException(ResourceException.INTERNAL_ERROR,
                MessageFormat.format("Unsupported Callback, {0}", callbackClass.getSimpleName()));
    }
}
