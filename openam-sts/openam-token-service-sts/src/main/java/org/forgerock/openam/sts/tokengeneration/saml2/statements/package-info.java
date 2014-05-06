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
 * This package contains classes defining the interfaces consumed to obtain the various statements contituting SAML2
 * assertions. End-users can implement a specific interface, and specify this class in the SAML2Config associated with
 * a published STS instance, and their class will be invoked to produce the specific statement type for inclusion in the
 * SAML2 assertion.
 */
package org.forgerock.openam.sts.tokengeneration.saml2.statements;