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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.xacml.v3;

/**
 * Diff status types used to describe the change in state of a single resource.
 *
 * @since 13.5.0
 */
public enum DiffStatus {

    ADD('A'), UPDATE('U');

    private final char code;

    DiffStatus(char code) {
        this.code = code;
    }

    /**
     * Single character description of diff status.
     *
     * @return Character code description of diff status.
     */
    public char getCode() {
        return code;
    }

}
