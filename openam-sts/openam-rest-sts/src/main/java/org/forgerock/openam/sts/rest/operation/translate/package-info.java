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
 * Contains classes related to the creation and implementation of token transformation operations, factory classes
 * that produce the RestTokenTransformValidator and RestTokenProvider implementations which realize a particular token transform,
 * and marshalling classes that marshal the token transform invocation state into the parameter state needed by the
 * RestTokenTransformValidator and RestTokenProvider implementations.
 *
 * Token transformation and token translation can be considered synonyms - different names were chosen to distinguish
 * the top-level operation(TokenTransformOperation), and the set of specific TokenTransform instances, each of which
 * validates a specific input token type and generates a specific output token type.
 */
package org.forgerock.openam.sts.rest.operation.translate;