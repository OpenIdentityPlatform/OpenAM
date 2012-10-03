/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 */
package org.forgerock.restlet.ext.oauth2.provider;

import org.restlet.Context;
import org.restlet.Restlet;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public interface OAuth2Provider {

    /**
     * Returns the context.
     * 
     * @return The context.
     */
    public Context getContext();

    /**
     * Attach a new realm to the OAuth2Provider
     * 
     * @param realm
     * @param next
     * @return
     */
    public boolean attachRealm(String realm, Restlet next);

    /**
     * Detach the specified realm from the OAuth2Provider
     * 
     * @param realm
     * @return
     */
    public Restlet detachRealm(String realm);

    /**
     * Attach the default realm to OAuth2Provider
     * 
     * @param next
     * @return
     */
    public boolean attachDefaultRealm(Restlet next);

    /**
     * Detach the default realm from OAuth2Provider
     * 
     * @return
     */
    public Restlet detachDefaultRealm();
}
