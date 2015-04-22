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
 * Copyright 2014-2015 ForgeRock AS.
 */

/**
 * Contains the classes related to obtaining the URLs corresponding to the REST authN targets for the various token types.
 * Each sts instance is associated with a realm, and each authenticated token-type is associated with a corresponding
 * authIndexType and authIndexValue, which allows the authentication of each token type to be directed at a distinct
 * OpenAM rest authN target. The classes in this package define this interface, and the implementation which consults
 * the AuthTargetMapping for the rest-sts instance, and returns the url defining the authN target for the specific token
 * type.
 */
package org.forgerock.openam.sts.token.validator.url;