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
 * This package contains classes related to creating the json payloads corresponding to REST-STS and TokenGenerationService
 * invocations. These classes allows these invocations to be constituted programmatically, and also allows the REST-STS
 * and TokenGenerationService to unmarshall json invocations into java objects.
 *
 * The builder pattern is followed throughout to support a fluent idiom, and to allow for the flexible addition of additional
 * state to the various token types.
 *
 * Some of the classes in this package, e.g. the ProofTokenState, are shared among the REST-STS and the TokenGenerationService.
 * I want to limit the dependencies between the sts constituent components, so the classes used to invoke the various services,
 * if shared between services, should be located in this package.
 */
package org.forgerock.openam.sts.service.invocation;