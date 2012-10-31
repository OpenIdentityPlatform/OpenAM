/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: TaskModel.java,v 1.8 2009/07/28 17:46:24 babysunil Exp $
 *
 */

package com.sun.identity.console.task.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import java.util.Map;
import java.util.Set;

public interface TaskModel
    extends AMModel
{
    /**
     * Returns realm names.
     *
     * @return realm names.
     * @throws AMConsoleException if realm cannot be retrieved.
     */
    Set getRealms()
        throws AMConsoleException;
    
    /**
     * Returns a set of signing keys.
     *
     * @return a set of signing keys.
     */
    Set getSigningKeys()
        throws AMConsoleException;
    
    /**
     * Returns a set of circle of trusts.
     * 
     * @param realm Realm.
     * @return a set of circle of trusts.
     * @throws AMConsoleException if unable to retrieve circle of trusts.
     */
    Set getCircleOfTrusts(String realm)
        throws AMConsoleException;
    
    /**
     * Returns a set of entities in a circle of trust.
     * 
     * @param realm Realm.
     * @param cotName Name of circle of trust.
     * @return a set of entities in a circle of trust.
     * @throws AMConsoleException if unable to retrieve entities.
     */
    Set getEntities(String realm, String cotName) 
        throws AMConsoleException;
    
     /**
     * Returns a set of hosted IDP in a circle of trust.
     * 
     * @param realm Realm.
     * @param cotName Name of circle of trust.
     * @return a set of hosted IDP in a circle of trust.
     * @throws AMConsoleException if IDP cannot be returned.
     */
    Set getHostedIDP(String realm, String cotName)
        throws AMConsoleException;
    
    /**
     * Returns a set of remote IDP in a circle of trust.
     * 
     * @param realm Realm.
     * @param cotName Name of circle of trust.
     * @return a set of remote IDP in a circle of trust.
     * @throws AMConsoleException if IDP cannot be returned.
     */
    Set getRemoteIDP(String realm, String cotName)
        throws AMConsoleException;
    
    /**
     * Returns a set of hosted SP in a circle of trust.
     * 
     * @param realm Realm.
     * @param cotName Name of circle of trust.
     * @return a set of hosted SP in a circle of trust.
     * @throws AMConsoleException if IDP cannot be returned.
     */
    Set getHostedSP(String realm, String cotName)
        throws AMConsoleException;
    
    /**
     * Returns a set of remote SP in a circle of trust.
     * 
     * @param realm Realm.
     * @param cotName Name of circle of trust.
     * @return a set of remote SP in a circle of trust.
     * @throws AMConsoleException if IDP cannot be returned.
     */
    Set getRemoteSP(String realm, String cotName)
        throws AMConsoleException;
    
    /**
     * Returns a map of realm to a map of circle of trust name to a set of
     * Hosted Identity Providers.
     * 
     * @return a map of realm to a map of circle of trust name to a set of
     *         Hosted Identity Providers.
     * @throws AMConsoleException if this map cannot be constructed.
     */
    Map getRealmCotWithHostedIDPs() 
        throws AMConsoleException;
    
    Map getConfigureGoogleAppsURLs(String realm, String entityId)
        throws AMConsoleException;
    
    Map getConfigureSalesForceAppsURLs(String realm, String entityId,
            String attrMap) throws AMConsoleException;

    /**
     * Saves the Salesforce login url as the Assertion Consumer Service Location
     * @param realm Realm
     * @param entityId Entity Name
     * @param acsUrl assertion consumer service location
     * @throws AMConsoleException if value cannot be saved.
     */
    void setAcsUrl(String realm, String entityId, String acsUrl)
            throws AMConsoleException;
    
}
