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

package org.forgerock.openam.uma;

/**
 * An UMA exception for when Clients need to provide more information to gain
 * authorization to a resource.
 *
 * @since 13.0.0
 */
public class NeedInfoException extends UmaException {

    /**
     * Constructs a new NeedInfoException.
     */
    public NeedInfoException() {
        super(403, UmaConstants.NEED_INFO_ERROR_CODE,
                "Additional information is required to determine whether the client is authorized to access "
                        + "the requested resource.");

    }
}
