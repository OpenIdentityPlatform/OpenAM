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
package org.forgerock.openam.utils;

import javax.xml.transform.TransformerFactory;

/**
 * Abstract factory for obtaining {@link TransformerFactory} instances. Implementations may cache or pool the instances
 * to avoid expensive initialisation overhead.
 *
 * @since 12.0.0
 */
public interface TransformerFactoryProvider {

    /**
     * Returns a TransformerFactory from the underlying mechanism. Multiple calls to this method on the same provider
     * from the same thread <em>may</em> return the same instance, but are not required to.
     *
     * @return A basic TransformerFactory object.
     */
    TransformerFactory getTransformerFactory();
}
