/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.session.util;

/**
 * Use to store an application token so that is can be evaluating later in the 
 * thread
 *
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class AppTokenHandler {
    /* The LocalThread for the currentcontext*/
    private static ThreadLocal appTokenHolder = new ThreadLocal();

    /**
     * Sets the ThreadLocal to the supplied application token
     *
     * @param appToken The application token
     */
    public static void set(Object appToken) {
        appTokenHolder.set(appToken);
    }

    /**
     * Returns the stored application token and then clears the ThreadLocal
     *
     * @return The application token
     */
    public static Object getAndClear() {
        Object temp = (Object) appTokenHolder.get();
        appTokenHolder.remove();

        return temp;
    }
}
