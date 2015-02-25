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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

/**
 * Contains the classes related to token validation, as defined by the org.apache.cxf.sts.token.validator.TokenValidator
 * interface. The general model is for org.apache.cxf.sts.token.validator.TokenValidator implementations to delegate
 * the actual TokenValidation to classes in the wss package via the AuthenticationHandler<T>, where T is the particular
 * Token type. Things get a bit confusing, as the CXF-STS defines its own TokenValidator interface, which serves to
 * integrate token validation into the CXF-STS context, but these implementations often delegate to the wss-defined
 * validator interface, org.apache.ws.security.validate.Validator. Furthermore, integrating token validation into
 * SecurityPolicy binding enforcement requires the implementation of the org.apache.ws.security.validate.Validator
 * interface.
 */
package org.forgerock.openam.sts.token.validator;