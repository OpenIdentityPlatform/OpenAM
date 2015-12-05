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
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import static org.forgerock.util.Reject.checkNotNull;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;

/**
 * Checked exception for errors that occur during federated single sign-on (SSO).
 *
 * @since 13.0.0
 * @see ServerFaultException
 * @see ClientFaultException
 */
public abstract class FederatedSSOException extends Exception {
    private final String messageCode;
    private final String detail;
    private final SAML2IdentityProviderAdapter idpAdapter;

    /**
     * Constructs the FederatedSSOException with the given parameters.
     *
     * @param idpAdapter the identity provider adapter, if resolved - may be null.
     * @param messageCode the message code of the error that occurred.
     * @param detail the detail of the exception.
     */
    public FederatedSSOException(final SAML2IdentityProviderAdapter idpAdapter, final String messageCode,
                                 final String detail) {
        super();
        this.messageCode = checkNotNull(messageCode, "Message code is null");
        this.detail = detail;
        this.idpAdapter = idpAdapter;
    }

    @Override
    public String getMessage() {
        return SAML2Utils.bundle.getString(messageCode) + (detail != null ? " (" + detail  +")" : "");
    }

    /**
     * Returns the message code of this error.
     *
     * @return the message code. Never null.
     */
    public String getMessageCode() {
        return messageCode;
    }

    /**
     * Returns the detail message of this error, if provided.
     *
     * @return the detail message - may be null.
     */
    public String getDetail() {
        return detail;
    }

    /**
     * The IDP adapter. This can be used to invoke hooks during error processing.
     *
     * @return the idp adapter. May be null.
     */
    public SAML2IdentityProviderAdapter getIdpAdapter() {
        return idpAdapter;
    }

    /**
     * The SOAP fault code of the error.
     *
     * @return one of {@link SAML2Constants#SERVER_FAULT} or {@link SAML2Constants#CLIENT_FAULT}. Never null.
     */
    public abstract String getFaultCode();
}
