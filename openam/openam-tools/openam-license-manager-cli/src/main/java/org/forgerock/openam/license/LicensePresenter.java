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
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2014 ForgeRock AS.
*/

package org.forgerock.openam.license;

/**
 * Interface between OpenAM modules and the license system.
 *
 * Abstracts away the interaction between the user and the presentation/acceptance of
 * licenses.
 */
public interface LicensePresenter {

    /**
     * Displays license(s) to the user.
     *
     * Displays a set of licenses to the user, prompting to ensure that
     * all licenses are accepted.
     *
     * @param preAccept Whether or not to auto-accept the license(s) displayed
     */
    public void presentLicenses(boolean preAccept) throws LicenseRejectedException;

    /**
     * Returns a notice to display to the user, indicating
     * that all licenses must be accepted for the user to be able to continue.
     */
    public String getNotice();

}
