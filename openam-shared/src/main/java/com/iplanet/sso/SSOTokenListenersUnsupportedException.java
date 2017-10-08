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
package com.iplanet.sso;

/**
 * This <code>SSOTokenCannotBeObservedException</code> is thrown when calling
 * {@link SSOToken#addSSOTokenListener(SSOTokenListener)} on an <code>SSOToken</code>
 * type that does not generate lifecycle events.
 *
 * @supported.all.api
 */
public class SSOTokenListenersUnsupportedException extends SSOException {

    /**
     * Constructs an <code>SSOTokenListenersUnsupportedException</code> with a detail message.
     *
     * @param msg The message provided by the object that is throwing the exception.
     */
    public SSOTokenListenersUnsupportedException(final String msg) {
        super(msg);
    }

}
