/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MAPModel.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */


package com.sun.identity.console.service.model;

import java.util.Set;

/* - NEED NOT LOG - */

public interface MAPModel {
    /**
     * Default profile name.
     */
    String DEFAULT_PROFILE_NAME = "HDML";
                                                                                
    /**
     * Attribute name of parent type
     */
    String ATTRIBUTE_NAME_PARENT_TYPE = "parentId";
                                                                                
    /**
     * Attribute name of client type name
     */
    String ATTRIBUTE_NAME_CLIENT_TYPE = "clientType";
                                                                                
    /**
     * Attribute name of user agent
     */
    String ATTRIBUTE_NAME_USER_AGENT = "userAgent";

    /**
     * Additional properties  classification name.
     */
    String ADDITIONAL_PROPERTIES_CLASSIFICAIION = "additionalPropertiesNames";

    /**
     * Returns styles of a profile.
     *
     * @param name Name of profile.
     * @return styles of a profile.
     */
    Set getStyleNames(String name);

    /**
     * Returns true if the given client is customizable.
     *
     * @param clientType Client Type.
     * @return true if the given client is customizable.
     */
    boolean isCustomizable(String clientType);

    /**
     * Returns true if the given client is customizable.
     *
     * @param clientType Client Type.
     * @return true if the given client is customizable.
     */
    boolean hasDefaultSetting(String clientType);

    /**
     * Returns true if the client can be deleted.
     *
     * @param clientType Client Type.
     * @return true if the client can be deleted.
     */
    boolean canBeDeleted(String clientType);

    /**
     * Returns device user agent of a client.
     *
     * @param clientType Client Type.
     * @return device user agent of a client.
     */
    String getDeviceUserAgent(String clientType);

    /**
     * Returns prefix for client type.
     *
     * @return prefix for client type.
     */
    String getClientTypePrefix();

    /**
     * Returns prefix for device user agent.
     *
     * @return prefix for device user agent.
     */
    String getDeviceNamePrefix();
}
