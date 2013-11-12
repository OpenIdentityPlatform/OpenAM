/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ResourceIDMapper.java,v 1.2 2008/06/25 05:47:18 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.interfaces;

import com.sun.identity.liberty.ws.soapbinding.Message;

/**
 * The class <code>ResourceIDMapper</code> is an interface that is 
 * used to map between an <code>userID</code> and the <code>ResourceID</code>
 * associated with it.
 * <p>
 * A different implementation of the interface may be developed by different
 * service provider. The implementation class(s) should be given to the provider
 * that hosts discovery service. The mapping between the <code>providerID</code>
 * and the implementation class can be configured through the
 * <code>Class for ResourceID Mapper Plugin</code> field in Discovery service.
 * @supported.all.api 
 */
public interface ResourceIDMapper {

    /**
     * Returns the resource ID that is associated with the user in a provider.
     * @param providerID ID of the provider.
     * @param userID ID of the user.
     * @return resource ID. Return null if the resource ID cannot be found.
     */
    public String getResourceID(String providerID, String userID);

    /**
     * Returns the ID of the user who has the resource ID in a provider.
     * @param providerID ID of the provider.
     * @param resourceID ID of the resource.
     * @return user ID. Return null if the user is not found.
     */
    public String getUserID(String providerID, String resourceID);

    /**
     * Returns the ID of the user who has the resource ID in a provider.
     * @param providerID ID of the provider.
     * @param resourceID ID of the resource.
     * @param message Request message.
     * @return user ID. Return null if the user is not found.
     */
    public String getUserID(String providerID,
                        String resourceID,
                        Message message);
}
