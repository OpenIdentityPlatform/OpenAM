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
 * $Id: ModifyResponse.java,v 1.2 2008/06/25 05:47:10 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.w3c.dom.*;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.liberty.ws.common.Status;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;

/**
 * The class <code>ModifyResponse</code> represents a discovery response for
 * modify request.
 * <p>The following schema fragment specifies the expected content within the
 * <code>ModifyResponse</code> object.
 * <p>
 * <pre>
 * &lt;complexType name="ModifyResponseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:liberty:disco:2003-08}Status"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="newEntryIDs">
 *         &lt;simpleType>
 *           &lt;list itemType="{urn:liberty:disco:2003-08}IDReferenceType" />
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class ModifyResponse {

    private String id = null;
    private List newEntryIDs = null;
    private Status status = null;
    private Element extension = null;

    /**
     * constructor.
     * @param status Status of the modify response
     */
    public ModifyResponse (com.sun.identity.liberty.ws.common.Status status) {
        this.status = status;
    }

    /**
     * Constructor.
     * @param root <code>ModifyResponse</code> DOM element.
     * @exception DiscoveryException if error occurs.
     */
    public ModifyResponse(Element root) throws DiscoveryException {
        if (root == null) {
            DiscoUtils.debug.message("ModifyResponse(Element): null input.");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        String nodeName;
        String nameSpaceURI;
        if (((nodeName = root.getLocalName()) == null) ||
            (!nodeName.equals("ModifyResponse")) ||
            ((nameSpaceURI = root.getNamespaceURI()) == null) ||
            (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
        {
            DiscoUtils.debug.message("ModifyResponse(Element): wrong input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("wrongInput"));
        }

        id = root.getAttribute("id");

        String ids = root.getAttribute("newEntryIDs");
        if ((ids != null) && (ids.length() != 0)) {
            StringTokenizer st = new StringTokenizer(ids);
            if (st.countTokens() > 0) {
                if (newEntryIDs == null) {
                    newEntryIDs = new ArrayList();
                }
                while (st.hasMoreTokens()) {
                    newEntryIDs.add(st.nextToken());
                }
            }
        }

        NodeList contentnl = root.getChildNodes();
        Node child;
        for (int i = 0, length = contentnl.getLength(); i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                nameSpaceURI = ((Element) child).getNamespaceURI();
                if ((nodeName.equals("Status")) &&
                    (nameSpaceURI != null) &&
                    (nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
                {
                    if (status != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("ModifyResponse(Element): "
                                + "included more than one Status.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    status = DiscoUtils.parseStatus((Element) child);
                } else if ((nodeName.equals("Extension")) &&
                    (nameSpaceURI != null) &&
                    (nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
                {
                    if (extension != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("ModifyResponse(Element): "
                                + "included more than one Extension.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    extension = (Element) child;
                    
                } else {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("ModifyResponse(Element): "
                            + "invalid node" + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
            }
        }

        if (status == null) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("ModifyResponse(Element): missing "
                    + "Status.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingStatus"));
        }


    }

    /**
     * Gets modify response status.
     * @return Status
     * @see #setStatus(com.sun.identity.liberty.ws.common.Status)
     */
    public com.sun.identity.liberty.ws.common.Status getStatus() {
        return status;
    } 

    /**
     * Sets modify response status.
     * @param value Status
     * @see #getStatus()
     */
    public void setStatus(com.sun.identity.liberty.ws.common.Status value) {
        status = value;
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
     * Gets the <code>newEntryIDs</code> attribute.
     *
     * @return the <code>newEntryIDs</code> attribute.
     * @see #setNewEntryIDs(List)
     */
    public List getNewEntryIDs() {
        return newEntryIDs; 
    }

    /**
     * Sets the <code>newEntryIDs</code> attribute.
     * @param ids values of the <code>newEntryIDs</code> attribute.
     * @see #getNewEntryIDs()
     */
    public void setNewEntryIDs(List ids) {
        newEntryIDs = ids;
    }

    /**
     * Gets modify response Extension Element.
     * @return Extension Element
     * @see #setExtension(Element)
     */
    public Element getExtension() {
        return extension;
    } 

    /**
     * Sets modify response extension.
     * @param extension Element
     * @see #getExtension()
     */
    public void setExtension(Element extension) {
        this.extension = extension;
    }

    /**
     * Gets formatted string of modify response.
     *
     * @return formatted string of modify response.
     */ 

    public java.lang.String toString() {
        StringBuffer sb = new StringBuffer(400);
        sb.append("<ModifyResponse xmlns=\"").append(DiscoConstants.DISCO_NS).
            append("\"");
        if ((id != null) && id.length() != 0) {
            sb.append(" id=\"").append(id).append("\"");
        }
        if (newEntryIDs != null) {
            sb.append(" newEntryIDs=\"");
            Iterator iter = newEntryIDs.iterator();
            if (iter.hasNext()) {
                sb.append((String) iter.next());
            }
            while (iter.hasNext()) {
                sb.append(" ").append((String) iter.next());
            }
            sb.append("\"");
        }
        sb.append(">");
        if (status != null) {
            sb.append(status.toString());
        }
        if (extension != null) {
            sb.append(XMLUtils.print(extension));
        }
        sb.append("</ModifyResponse>");
        return sb.toString();
    }
}
