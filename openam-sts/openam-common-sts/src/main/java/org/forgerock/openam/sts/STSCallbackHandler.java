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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts;

import org.apache.ws.security.WSPasswordCallback;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.config.user.KeystoreConfig;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

import org.slf4j.Logger;

public class STSCallbackHandler implements CallbackHandler {
    private final KeystoreConfig keystoreConfig;

    private final Logger logger;

    public STSCallbackHandler(KeystoreConfig keystoreConfig, Logger logger) {
        this.keystoreConfig = keystoreConfig;
        this.logger = logger;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof WSPasswordCallback) {
                WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
                if (pc.getUsage() == WSPasswordCallback.DECRYPT) {
                    if ((keystoreConfig.getEncryptionKeyAlias() != null) ? keystoreConfig.getEncryptionKeyAlias().equals(pc.getIdentifier()) : false) {
                        pc.setPassword(new String(keystoreConfig.getEncryptionKeyPassword(), AMSTSConstants.UTF_8_CHARSET_ID));
                    }
                } else if (pc.getUsage() == WSPasswordCallback.SIGNATURE) {
                        if ((keystoreConfig.getSignatureKeyAlias() != null) ? keystoreConfig.getSignatureKeyAlias().equals(pc.getIdentifier()) : false) {
                            pc.setPassword(new String(keystoreConfig.getSignatureKeyPassword(), AMSTSConstants.UTF_8_CHARSET_ID));
                        }
                }  else {
                    /*
                    TODO: getting a usage of 9 - which is for a secret key - unexpected - happens only in the renew token operation, and
                    only when we don't call setVerifyProofOfPossession(false) in the SAMLTokenRenewer. But the identifier we get is
                    some arcane string - e.g. _E56EE83BEB8E9F63C9136683677368353 - I need to figure out what is going on here.

                    debug.error("Going to throw UnsupportedCallbackException in STSCallbackHandler. " +
                            "Dealing with WSPasswordCallback, but unexpected usage of: " + pc.getUsage());
                    throw new UnsupportedCallbackException(callbacks[i]);
                    */
                    logger.error("Unexpected state in STSCallbackHandler. " +
                            "Dealing with WSPasswordCallback, but unexpected usage of: " + pc.getUsage() + " and an id of " + pc.getIdentifier());

                }
            } else {
                logger.error("Going to throw UnsupportedCallbackException in STSCallbackHandler. " +
                        "Dealing a Callback of unexpected type " +
                        (callbacks[i] != null ? callbacks[i].getClass().getCanonicalName() : null));
                throw new UnsupportedCallbackException(callbacks[i]);
            }
        }
    }
}
