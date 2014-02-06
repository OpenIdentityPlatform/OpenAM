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

package org.forgerock.openam.license;

/**
 * Simple locator for looking up required click-through licenses that the user must accept on install/upgrade.
 *
 * @since 12.0.0
 */
public interface LicenseLocator {
    /**
     * Gets a set of required licenses that must all be accepted by the user before the software can be installed/
     * upgraded. All licenses in the set must be accepted before installation/upgrade can occur.
     *
     * @return the list of all required licenses, in the order in which they should be displayed.
     * @throws MissingLicenseException if a required license cannot be found by the system.
     */
    LicenseSet getRequiredLicenses();
}
