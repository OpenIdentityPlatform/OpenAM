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
package com.sun.identity.authentication.callbacks;

import org.forgerock.util.Reject;

/**
 * Adds a hidden value callback so that the login form can return values that the are not visually rendered on the page.
 *
 * @since 12.0.0
 */
public class HiddenValueCallback implements javax.security.auth.callback.Callback, java.io.Serializable {

    private String value;
    private final String id;
    private final String defaultValue = "";

    /**
     * Create a new HiddenValueCallback with the id as specified.
     *
     * @param id The id for the HiddenValueCallback when it is rendered as an HTML element.
     */
    public HiddenValueCallback(String id) {
        Reject.ifNull(id, "A HiddenValueCallback must be given an id.");
        this.id = id;
    }

    /**
     * Create a new HiddenValueCallback with the id and initial value as specified.
     *
     * @param id The id for the HiddenValueCallback when it is rendered as an HTML element.
     * @param value The initial value for the HiddenValueCallback when it is rendered as an HTML element.
     */
    public HiddenValueCallback(String id, String value) {
        Reject.ifNull(id, "A HiddenValueCallback must be given an id.");
        this.id = id;
        this.value = value;
    }

    /**
     * Get the value set on the HiddenValueCallback when it is sent in a REST request.
     *
     * @return The value set on the callback.
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the value on the HiddenValueCallback.
     *
     * @param value The value to set on the callback.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the id for the HiddenValueCallback to use when it is rendered as an HTML element.
     *
     * @return The id for the callback.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the initial default value set on the HiddenValueCallback.
     *
     * @return The initial default value.
     */
    public java.lang.String getDefaultValue() {
        return defaultValue;
    }
}
