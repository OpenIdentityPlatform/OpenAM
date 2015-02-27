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
 * Copyright 2014-2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token;

/**
 * This interface defines functionality to catenate url elements, insuring that a '/' exists in between. Note that this
 * interface does not define a generic means to catenate any sequence of strings, ensuring that the result is still
 * in url format. The scope of these concerns are more constrained: sts instances can be published with various elements
 * which must be catenated to create a url. It is impossible to enforce that all of the catenated elements are separated
 * by only a single '/', so this is the primary concern of this interface. The UrlConstituentCatenator will also not
 * interpose a '/' between url constituents once a query parameter has been encountered.
 */
public interface UrlConstituentCatenator {
    /**
     * Catenate an array of url constituents into a single string, insuring that only a single '/' separates each of the
     * constituents. A constituent may be the empty string, or null. If a '?' exists in any of the constituents, the
     * '/' character will no longer be interposed between constituents.
     * @param constituents The array of url constituents
     * @return the string composed of all of the constituents.
     */
    String catenateUrlConstituents(String... constituents);
}
