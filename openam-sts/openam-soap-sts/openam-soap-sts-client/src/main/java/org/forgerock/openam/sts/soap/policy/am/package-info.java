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

/**
 * The classes in this package provide the client-side support for inserting OpenAMSessionToken assertions in the
 * RequestSecurityToken so that soap-sts instances, protected by SecurityPolicy bindings specifying OpenAMSessionToken
 * SupportingTokens, can be successfully invoked.
 */
package org.forgerock.openam.sts.soap.policy.am;