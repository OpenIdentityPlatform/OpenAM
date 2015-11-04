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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.session;

import com.iplanet.sso.SSOToken;
import java.util.Set;

/**
 * Interface for session resource listing.
 */
public interface SessionPropertyList {

    /**
     * Returns all listed properties, in no particular order.
     *
     * @param token SSOToken which points to the session whose properties we are interested in.
     * @param realm used to determine the contex tin which the listed properites should be read.
     * @return a set of Strings, each one corresponding to a single listed property which may or may not
     *         be set in the user's profile.
     */
    Set<String> getAllListedProperties(SSOToken token, String realm);

    /**
     * Checks whether a property name/property names is in the list of properties.
     * If multiple property names are passed in, only returns true if ALL properties included.
     *
     * @param token SSOToken which points to the session whose properties we are interested in.
     * @param realm used to determine the contex tin which the listed properites should be read.
     * @param propertyNames a set of property names to check.
     * @return <code>true</code> if ALL values in propertyName are listed, <code>false</code> otherwise.
     */
    boolean isPropertyListed(SSOToken token, String realm, Set<String> propertyNames);

}
