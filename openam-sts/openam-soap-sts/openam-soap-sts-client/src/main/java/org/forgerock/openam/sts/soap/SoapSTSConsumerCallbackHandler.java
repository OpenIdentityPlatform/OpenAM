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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap;

import org.apache.ws.security.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * The CallbackHandler which supports all of the possible Callback types required by the SoapSTSConsumer. This CallbackHandler
 * is invoked by the CXF STS client encapsulated within the SoapSTSConsumer to provide password state required from
 * within the CXF STS client runtime.
 * The cases are as follow:
 * 1. OpenAMSessionTokenCallback instances, in order to set the OpenAM session id necessary to consume sts instances
 * protected by an OpenAMSessionToken Assertion. Consumed by the OpenAMSessionTokenClientAssertionBuilder.
 * 2. org.apache.ws.security.WSPasswordCallback instances, with DECRYPT and SIGNATURE usages, to set the KeyStore
 * password corresponding to the KeyStore alias necessary to decrypt and sign messages. Necessary for the asymmetric
 * binding, when messages from client to server must be signed by the client's private key, and where messages
 * from server to client are encrypted with the client's public key.
 * 3. org.apache.ws.security.WSPasswordCallback instances, with USERNAME_TOKEN usage, to provide the password set in
 * the UsernameTokens created to consume soap-sts instances protected by SecurityPolicy bindings specifying
 * UsernameToken SupportingTokens. Will provide the password corresponding to the usernameTokenUsername parameter above.
 *
 * Most soap-sts consumers will only use a subset of this functionality, but this CallbackHandler is provided as an
 * example of how to satisfy the various Callback scenarios.
 */
public class SoapSTSConsumerCallbackHandler implements CallbackHandler {
    public static class SoapSTSConsumerCallbackHandlerBuilder {
        private String decryptionKeyAlias;
        private String decryptionKeyPassword;
        private String signatureKeyAlias;
        private String signatureKeyKeyPassword;
        private String usernameTokenUsername;
        private String usernameTokenPassword;
        private String openAMSupportingTokenSessionId;


        public SoapSTSConsumerCallbackHandlerBuilder decryptionKeyAlias(String decryptionKeyAlias) {
            this.decryptionKeyAlias = decryptionKeyAlias;
            return this;
        }

        public SoapSTSConsumerCallbackHandlerBuilder decryptionKeyPassword(String decryptionKeyPassword) {
            this.decryptionKeyPassword = decryptionKeyPassword;
            return this;
        }

        public SoapSTSConsumerCallbackHandlerBuilder signatureKeyAlias(String signatureKeyAlias) {
            this.signatureKeyAlias = signatureKeyAlias;
            return this;
        }

        public SoapSTSConsumerCallbackHandlerBuilder signatureKeyKeyPassword(String signatureKeyKeyPassword) {
            this.signatureKeyKeyPassword = signatureKeyKeyPassword;
            return this;
        }

        public SoapSTSConsumerCallbackHandlerBuilder usernameTokenSupportingTokenUsername(String usernameTokenUsername) {
            this.usernameTokenUsername = usernameTokenUsername;
            return this;
        }

        public SoapSTSConsumerCallbackHandlerBuilder usernameTokenSupportingTokenPassword(String usernameTokenPassword) {
            this.usernameTokenPassword = usernameTokenPassword;
            return this;
        }

        public SoapSTSConsumerCallbackHandlerBuilder openAMSupportingTokenSessionId(String openAMSessionId) {
            this.openAMSupportingTokenSessionId = openAMSessionId;
            return this;
        }

        public SoapSTSConsumerCallbackHandler build() {
            return new SoapSTSConsumerCallbackHandler(this);
        }
    }

    private final String decryptionKeyAlias;
    private final String decryptionKeyPassword;
    private final String signatureKeyAlias;
    private final String signatureKeyKeyPassword;
    private final String usernameTokenSupportingTokenUsername;
    private final String usernameTokenSupportingTokenPassword;
    private final String openAMSupportingTokenSessionId;

    private SoapSTSConsumerCallbackHandler(SoapSTSConsumerCallbackHandlerBuilder builder) {
        this.decryptionKeyAlias = builder.decryptionKeyAlias;
        this.decryptionKeyPassword = builder.decryptionKeyPassword;
        this.signatureKeyAlias = builder.signatureKeyAlias;
        this.signatureKeyKeyPassword = builder.signatureKeyKeyPassword;
        this.usernameTokenSupportingTokenUsername = builder.usernameTokenUsername;
        this.usernameTokenSupportingTokenPassword = builder.usernameTokenPassword;
        this.openAMSupportingTokenSessionId = builder.openAMSupportingTokenSessionId;
        //no rejects for null values - not all callback types will be supported.
    }

    public static SoapSTSConsumerCallbackHandlerBuilder builder() {
        return new SoapSTSConsumerCallbackHandlerBuilder();
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback) {
                WSPasswordCallback pc = (WSPasswordCallback) callback;
                /*
                In a asymmetric binding, messages from server to client will be encrypted with the client's public key,
                and thus decryption requires the client's private key entry
                 */
                if (pc.getUsage() == WSPasswordCallback.DECRYPT) {
                    if (decryptionKeyAlias.equals(pc.getIdentifier())) {
                        pc.setPassword(decryptionKeyPassword);
                    } else {
                        throw new UnsupportedCallbackException(callback,
                                "Callback class has unexpected decryptionKeyAlias: " + pc.getIdentifier());
                    }
                }
                /*
                In a asymmetric binding, messages from client to server must be signed by the client's private key.
                 */
                else if (pc.getUsage() == WSPasswordCallback.SIGNATURE) {
                    if (signatureKeyAlias.equals(pc.getIdentifier())) {
                        pc.setPassword(signatureKeyKeyPassword);
                    } else {
                        throw new UnsupportedCallbackException(callback,
                                "Callback class has unexpected signatureKeyAlias: " + pc.getIdentifier());
                    }
                } else if (pc.getUsage() == WSPasswordCallback.USERNAME_TOKEN) {
                    if (usernameTokenSupportingTokenUsername.equals(pc.getIdentifier())) {
                        pc.setPassword(usernameTokenSupportingTokenPassword);
                    } else {
                        throw new UnsupportedCallbackException(callback,
                                "Callback class has unexpected UsernameToken username: " + pc.getIdentifier());
                    }
                }
            } else if (callback instanceof OpenAMSessionTokenCallback) {
                ((OpenAMSessionTokenCallback)callback).setSessionId(openAMSupportingTokenSessionId);
            } else {
                throw new UnsupportedCallbackException(callback, "Unsupported Callback class: "
                        + callback.getClass().getCanonicalName());
            }
        }
    }
}
