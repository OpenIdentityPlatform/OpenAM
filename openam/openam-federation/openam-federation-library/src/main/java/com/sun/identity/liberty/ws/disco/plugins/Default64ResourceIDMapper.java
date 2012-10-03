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
 * $Id: Default64ResourceIDMapper.java,v 1.3 2008/08/06 17:28:08 exu Exp $
 *
 */


package com.sun.identity.liberty.ws.disco.plugins;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.liberty.ws.interfaces.ResourceIDMapper;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;

/**
 * The class <code>Default64ResourceIDMapper</code> provides a default
 * implementation of the <code>ResourceIDMapper</code> interface. 
 * <p>
 * The implementation assumes the format of ResourceID is:
 * providerID + "/" + the Base64 encoded userID.
 */
public class Default64ResourceIDMapper implements ResourceIDMapper {

    private static Debug debug = Debug.getInstance("libIDWSF");
    /**
     * Default Constructor.
     */
    public Default64ResourceIDMapper() {}

    /**
     * Returns the resource ID that is associated with the user in a provider.
     * @param providerID ID of the provider.
     * @param userID ID of the user.
     * @return resource ID. Return null if the resource ID cannot be found.
     */
    public String getResourceID(String providerID, String userID) {
        if (userID == null) {
            debug.error("Default64ResourceIDMapper.getResourceID:null userID");
            return null;
        }
        if ((providerID == null) || (providerID.length() == 0)) {
            debug.error("Default64ResourceIDMapper.getResourceID:null "
                + "providerID.");    
            return null;
        }

        byte byteResult[] = SAMLUtils.stringToByteArray(userID);
        String result = null;
        try {
            result = Base64.encode(byteResult).trim();
        } catch (Exception e) {
            debug.error("Default64ResourceIDMapper.getResourceID:",e);
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
     * Returns the ID of the user who has the resource ID in a provider.
     * @param providerID ID of the provider.
     * @param resourceID ID of the resource.
     * @return user ID. Return null if the user is not found.
     */
    public String getUserID(String providerID, String resourceID) {
        return getUserID(providerID, resourceID, null);
    }

    /**
     * Returns the ID of the user who has the resource ID in a provider.
     * @param providerID ID of the provider.
     * @param resourceID ID of the resource.
     * @param message Request message.
     * @return user ID. Return null if the user is not found.
     */
    public String getUserID(String providerID,
                        String resourceID,
                        Message message)
    {
        String result = null;
        if ((resourceID == null) ||
            (resourceID.equals(DiscoConstants.IMPLIED_RESOURCE)))
        {
            if (debug.messageEnabled()) {
                debug.message("Default64ResourceIDMapper.getUserID: used "
                    + "implied resource.");
            }
            if (message == null) {
                debug.error("Default64ResourceIDMapper.getUserID:null message");
                return null;
            } else {
                SecurityAssertion assertion = message.getAssertion();
                if (assertion == null) {
                    debug.error("Default64ResourceIDMapper.getUserID:null "
                        + "assertion");
                    return null;
                }
                Subject subject = assertion.getBearerSubject();
                if (subject == null) {
                    debug.error("Default64ResourceIDMapper.getUserID:not "
                        + "Bearer Token");
                    return null;
                }
                NameIdentifier ni = subject.getNameIdentifier();
                if (ni == null) {
                    debug.error("Default64ResourceIDMapper.getUserID:no "
                        + "NameIdentifier");
                    return null;
                }
                return ni.getName();
            }
        }

        if ((providerID == null) || (providerID.length() == 0)) {
            debug.error("Default64ResourceIDMapper.getUserID:null providerID.");
            return null;
        }
        if (!resourceID.startsWith(providerID)) {
            debug.error("Default64ResourceIDMapper.getUserID:resourceID not "
                + "startsWith providerID:" + providerID);
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
            debug.error("Default64ResourceIDMapper.getUserID:",e);
            return null;
        }
        return result;
    }
}
