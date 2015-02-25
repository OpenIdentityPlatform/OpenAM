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

package com.sun.identity.idm;

/**
 * A listener that will be notified each time a {@link AMIdentityRepository} is created.
 *
 * @since 13.0.0
 */
public interface IdRepoCreationListener {

    /**
     * Called when a {@code AMIdentityRepository} is created.
     *
     * @param idRepo The {@code AMIdentityRepository} instance that has just been created.
     * @param realm The realm the {@code AMIdentityRepository} was created in.
     */
    void notify(AMIdentityRepository idRepo, String realm);
}
