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

package org.forgerock.openam.sts.config.user;

/**
 * An enum describing the manner in which the public key corresponding to the private signing key is
 * identified, as specified here: https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-41#section-4
 * Not that if the jku type is specified, the kid claim must also be specified. An instance of this enum will be passed to the
 * will be specified in the OpenIdConnectTokenConfig, and referenced in the token service to insure that the appropriate
 * key identifier state can be created. Note that not all id types will be supported initially - the x5u and jku seem
 * especially problematic in an STS context without actually validating that the public key referenced by the url
 * actually corresponds to the private key. Support for additional schemes can be provided as indicated by customer
 * demand. Initially, only NONE and JWK will be supported.
 */
public enum OpenIdConnectTokenPublicKeyReferenceType {
    NONE, JKU, JWK, X5U, X5C, X5T, X5TS256
}
