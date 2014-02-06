/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

/**
 * Provides common code for looking up click-through license agreements to display to the user during installation
 * or upgrade of OpenAM. Includes a {@link org.forgerock.openam.license.LicenseLocator} interface for locating a
 * license file within the installation archive/war and basic domain objects for licenses and required license
 * sets.
 *
 * @since 12.0.0
 */
package org.forgerock.openam.license;