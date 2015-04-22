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

package org.forgerock.openam.sts;

/**
 * Provides an extensible means of identifying a to-be-validated or to-be-provided token type. The TokenType enum will
 * implement this interface, and this interface will be used to identify tokens specified in token transformations.
 * An extensible means of identifying tokens types is necessary to support end-user-provided JsonTokenValidators and
 * JsonTokenProviders.
 */
public interface TokenTypeId {
    String getId();
}
