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
 * $Id: IDPPResourceIDMapper.java,v 1.2 2008/06/25 05:47:17 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.idpp.plugin;

import com.sun.identity.liberty.ws.interfaces.ResourceIDMapper;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.idpp.common.IDPPUtils;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;

/**
 * The class <code>IDPPResourceIDMapper</code> is an implementation of
 * <code>ResourceIDMapper</code> which is used to map a IDPP
 * user with a user that has been registered with discovery.
 */

public class IDPPResourceIDMapper implements ResourceIDMapper {
    
    /**
     * Gets the resourceID for a user in IDPP service provider
     * @param providerID Service provider ID
     * @param userID user ID 
     * @return String resource ID of a user, null if there is an error
     */
    public String getResourceID(String providerID, String userID) {
        if (userID == null) {
            IDPPUtils.debug.error("IDPPResourceIDMapper.getResourceID:" +
            "null userID");
            return null;
        }
        if ((providerID == null) || (providerID.length() == 0)) {
            IDPPUtils.debug.error("IDPPResourceIDMapper.getResourceID:" +
            "null providerID.");
            return null;
        }

        byte byteResult[] = SAMLUtils.stringToByteArray(userID);
        String result = null;
        try {
            result = Base64.encode(byteResult).trim();
        } catch (Exception e) {
            IDPPUtils.debug.error("IDPPResourceIDMapper.getResourceID:" , e);
            return null;
        }

        String urlEncoded = null;
        if (providerID.endsWith("/")) {
            urlEncoded = providerID + URLEncDec.encode(result);
        } else {
            urlEncoded = providerID + "/" + URLEncDec.encode(result);
        }
        return urlEncoded;

    }

    /**
     * Gets the user ID by decrypting resource id
     * @param providerID Service Provider ID
     * @param resourceID Resource ID of a user
     * @return String userID by decrypting the resource ID,
     *                null, if there's any failure
     */ 
    public String getUserID(String providerID, String resourceID) {
        return getUserID(providerID, resourceID, null);
    }

    /**
     * Gets the user ID by decrypting resource id
     * @param providerID Service Provider ID
     * @param resourceID Resource ID of a user
     * @param message Message of soapbinding 
     * @return String userID by decrypting the resource ID,
     *                null, if there's any failure
     */ 
    public String getUserID(String providerID, String resourceID,
                                Message message) {
        String result = null;
        if ((resourceID == null) || (providerID == null) || 
            (providerID.length() == 0)) {
            IDPPUtils.debug.error("IDPPResourceIDMapper.getUserID:" +
            "resourceID or providerID is null");
            return null;
        }

        if (!resourceID.startsWith(providerID)) {
            IDPPUtils.debug.error("IDPPResourceIDMapper.getUserID:resourceID" +
            " does not startsWith providerID:" + providerID);
            return null;
        }

        String urlDecoded = null;
        if (providerID.endsWith("/")) {
            urlDecoded = URLEncDec.decode(resourceID.substring(
                                        providerID.length()));
        } else {
            urlDecoded = URLEncDec.decode(resourceID.substring(
                                        (providerID+"/").length()));
        }

        try {
            result = SAMLUtils.byteArrayToString(Base64.decode(urlDecoded));
        } catch (Exception e) {
            IDPPUtils.debug.error("IDPPResourceIDMapper.getUserID:",e);
            return null;
        }
        return result;

    }
}
