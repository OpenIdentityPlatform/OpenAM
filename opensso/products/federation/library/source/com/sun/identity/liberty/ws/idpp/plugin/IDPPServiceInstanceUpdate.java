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
 * $Id: IDPPServiceInstanceUpdate.java,v 1.2 2008/06/25 05:47:17 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.idpp.plugin;

import com.sun.identity.liberty.ws.soapbinding.ServiceInstanceUpdateHeader;
import com.sun.identity.liberty.ws.interfaces.ServiceInstanceUpdate;
import com.sun.identity.liberty.ws.idpp.IDPPServiceManager;
import com.sun.identity.liberty.ws.idpp.common.IDPPUtils;

import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * This class <code>IDPPServiceInstanceUpdate</code> is an implementation of
 * <code>ServiceInstanceUpdate</code> for the Personal Profile Service.
 * The default implementation supports only end point moved and issues a
 * soap fault. The decision to include a service instance update header
 * is through a global flag. 
 */
public class IDPPServiceInstanceUpdate implements ServiceInstanceUpdate {

    /**
     * Gets the service instance update header.
     *
     * @return <code>ServiceInstanceUpdateHeader</code> ServiceInstanceUpdate
     *         Header for the Personal Profile Service.
     *         "null" if the end point is not configured.
     */
    public ServiceInstanceUpdateHeader getServiceInstanceUpdateHeader() {
       
         ServiceInstanceUpdateHeader siuHeader = 
                    new ServiceInstanceUpdateHeader();
         String alternateEndPoint = 
              IDPPServiceManager.getInstance().getAlternateEndPoint();

         if(alternateEndPoint == null) {
            IDPPUtils.debug.error("IDPPServiceInstanceUpdate.getService" +
            "InstanceUpdateHeader: Alternate Endpoint is null");
            return null;
         }
         siuHeader.setEndpoint(alternateEndPoint);

         Set alternateSechMechs = 
             IDPPServiceManager.getInstance().getAlternateSecurityMechs();

         if(alternateSechMechs != null && !alternateSechMechs.isEmpty()) {

            List list = new ArrayList();
            Iterator iter = alternateSechMechs.iterator();

            while(iter.hasNext()) {
               list.add((String)iter.next());
            }

            siuHeader.setSecurityMechIDs(list);
         }

         return siuHeader;
    }

    /**
     * Check to see if soap fault needs to be issue while processing the
     * request.
     *
     * @return true
     */
    public boolean isSOAPFaultNeeded() {
        return true;
    }
}
