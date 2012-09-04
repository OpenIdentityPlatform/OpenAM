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
 * $Id: DefaultHexResourceIDMapper.java,v 1.3 2008/08/06 17:28:09 exu Exp $
 *
 */


package com.sun.identity.liberty.ws.disco.plugins;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.liberty.ws.interfaces.ResourceIDMapper;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.liberty.ws.security.SecurityAssertion;

/**
 * The class <code>DefaultHexResourceIDMapper</code> provide a default
 * implementation of the <code>ResourceIDMapper</code> interface. 
 * <p>
 * The implementation assumes the format of ResourceID is:
 * providerID + "/" + the hex string of userID.
 */
public class DefaultHexResourceIDMapper implements ResourceIDMapper {

    private static Debug debug = Debug.getInstance("libIDWSF");

    /**
     * Default Constructor.
     */
    public DefaultHexResourceIDMapper() {}

    /**
     * Returns the resource ID that is associated with the user in a provider.
     * @param providerID ID of the provider.
     * @param userID ID of the user.
     * @return resource ID. Return null if the resource ID cannot be found.
     */
    public String getResourceID(String providerID, String userID) {
        if (userID == null) {
            debug.error("DefaultHexResourceIDMapper.getResourceID:null userID");
            return null;
        }

        if ((providerID == null) || (providerID.length() == 0)) {
            debug.error("DefaultHexResourceIDMapper.getResourceID:null "
                                + "providerID.");
            return null;
        }
        if (providerID.endsWith("/")) {
            return (providerID + SAMLUtils.byteArrayToHexString(
                                        SAMLUtils.stringToByteArray(userID)));
        } else {
            return (providerID + "/" + SAMLUtils.byteArrayToHexString(
                                        SAMLUtils.stringToByteArray(userID)));
        }
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
        if ((resourceID == null) ||
            (resourceID.equals(DiscoConstants.IMPLIED_RESOURCE)))
        {
            if (debug.messageEnabled()) {
                debug.message("DefaultHexResourceIDMapper.getUserID: used "
                    + "implied resource.");
            }
            if (message == null) {
                debug.error(
                    "DefaultHexResourceIDMapper.getUserID:null message");
                return null;
            } else {
                SecurityAssertion assertion = message.getAssertion();
                if (assertion == null) {
                    debug.error("DefaultHexResourceIDMapper.getUserID:no "
                                + "assertion");
                    return null;
                }
                Subject subject = assertion.getBearerSubject();
                if (subject == null) {
                    debug.error("DefaultHexResourceIDMapper.getUserID:not "
                                + "Bearer Token");
                    return null;
                }
                NameIdentifier ni = subject.getNameIdentifier();
                if (ni == null) {
                    debug.error("DefaultHexResourceIDMapper.getUserID:no "
                                + "NameIdentifier");
                    return null;
                }
                return ni.getName();
            }
        }

        if ((providerID == null) || (providerID.length() == 0)) {
            debug.error("DefaultHexResourceIDMapper.getUserID:null providerID");
            return null;
        }
        if (!resourceID.startsWith(providerID)) {
            debug.error("DefaultHexResourceIDMapper.getUserID:resourceID not "
                + "startsWith providerID:" + providerID);
            return null;
        }

        if (providerID.endsWith("/")) {
            return SAMLUtils.byteArrayToString(SAMLUtils.hexStringToByteArray(
                        resourceID.substring(providerID.length())));
        } else {
            return SAMLUtils.byteArrayToString(SAMLUtils.hexStringToByteArray(
                        resourceID.substring((providerID+"/").length())));
        }
    }
}
