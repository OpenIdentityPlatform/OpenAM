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

package org.forgerock.openam.sts.rest.token.provider;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.sts.TokenTypeId;

import java.security.Principal;

/**
 * Parameter state passed to JsonTokenProvider instances. Generic type corresponds to the token state necessary to
 * produce the token produced by the RestTokenProvider. The token creation state necessary to create a SAML2 assertion includes
 * the SubjectConfirmation and the ProofTokenState (for HolderOfKey assertions). The token creation state necessary to
 * create a OIDC token includes a nonce and the authentication time. This state is too heterogeneous to subsume in anything
 * other than a marker interface. Note also that this type is reflected in the RestTokenProvider interface, and it
 * should be as generic as possible, as to support user-defined RestTokenProvider implementations.
 */
public interface RestTokenProviderParameters<T> {
    /**
     *
     * @return the token state necessary to produce the - e.g. the SubjectConfirmation
     * or proof token state for a SAML2 assertion.
     */
    T getTokenCreationState();

    /**
     *
     * @return the type of the input token. Necessary to generate the authentication context class ref for
     * a SAML2 assertion - in general, produced tokens may have to have a representation of how the subject encapsulated
     * in the generated token was authenticated. Published sts instances allow for the specification of a Saml2JsonTokenAuthnContextMapper
     * implementation which will generate this SAML2 authentication context class ref, a plug-in interface which takes
     * the TokenTypeId as input. Published rest-sts instances which produce OpenIdConnect tokens have similar mapping
     * implementations which produce the amr and acr claims.
     */
    TokenTypeId getInputTokenType();

    /**
     *
     * @return the json representation of the input token. Necessary to generate the authentication context class ref for
     * a SAML2 assertion - in general, produced tokens may have to have a representation of how the subject encapsulated
     * in the generated token was authenticated. Published sts instances allow for the specification of a Saml2JsonTokenAuthnContextMapper
     * implementation which will generate this SAML2 authentication context class ref, a plug-in interface which takes the json representation
     * of the token as input. Published rest-sts instances which produce OpenIdConnect tokens have similar mapping
     * implementations which produce the amr and acr claims.
     */
    JsonValue getInputToken();
}
