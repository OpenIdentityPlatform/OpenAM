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
 * $Id: ResourceID.java,v 1.2 2008/06/25 05:47:11 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;

import org.w3c.dom.Element;

import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The class <code>ResourceID</code> represents a discovery service resource ID.
 * The following schema fragment specifies the expected content within the
 * <code>ResourceID</code> object.
 * <pre>
 * &lt;xs:element name="ResourceID" type="ResourceIDType"/>
 * &lt;complexType name="ResourceIDType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>anyURI">
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class ResourceID {

    private String resourceID = null;
    private String id = null;

    /**
     * Constructor.
     * @param resourceID resource ID string
     */
    public ResourceID(java.lang.String resourceID) {
        this.resourceID = resourceID;
    }

    /**
     * Constructor.
     * @param elem <code>ResourceID</code> in DOM Element
     * @exception DiscoveryException if error occurs
     */
    public ResourceID(Element elem) throws DiscoveryException {
        if (elem == null) {
            DiscoUtils.debug.message("ResourceID(Element): null input.");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        String tag = null;
        String nameSpaceURI = null;
        if (((tag = elem.getLocalName()) == null) ||
            (!tag.equals("ResourceID")) ||
            ((nameSpaceURI = elem.getNamespaceURI()) == null) ||
            (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
        {
            DiscoUtils.debug.message("ResourceID(Element): wrong input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("wrongInput"));
        }

        id = elem.getAttribute("id");
        resourceID = XMLUtils.getElementValue(elem);
        if ((resourceID == null) || (resourceID.length() == 0)) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("ResourceID(Element): missing "
                    + "ResourceID element value.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingResourceIDValue"));
        }
    }

    /**
     * Gets id attribute.
     *
     * @return id attribute.
     * @see #setId(String)
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets id attribute.
     *
     * @param id id attribute.
     * @see #getId()
     */
    public void setId(String id) {
        this.id = id;
    }


    /**
     * Gets resource id.
     * @return resource id.
     * @see #setResourceID(String)
     */
    public String getResourceID() {
        return resourceID;
    }

    /**
     * Sets resource id.
     * @param resourceID resource id to be set 
     * @see #getResourceID()
     */
    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }


    /**
     * Returns string format of object <code>ResourceID</code>.
     *
     * @return formatted string.
     */ 
    public String toString() {
        StringBuffer sb = new StringBuffer(300);
        sb.append("<ResourceID xmlns=\"").append(DiscoConstants.DISCO_NS).
                append("\"");
        if ((id != null) && id.length() != 0) {
            sb.append(" id=\"").append(id).append("\"");
        }
        sb.append(">");
        if (resourceID != null) {
            sb.append(resourceID);
        }
        sb.append("</ResourceID>");
        return sb.toString();
    }
}
