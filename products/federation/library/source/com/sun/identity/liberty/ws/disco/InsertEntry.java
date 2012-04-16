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
 * $Id: InsertEntry.java,v 1.2 2008/06/25 05:47:10 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.w3c.dom.*;

import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;

/**
 * The class <code>InsertEntry</code> represents a Insert Entry for Discovery
 * Modify request.
 * <p>The following schema fragment specifies the expected content within the
 * <code>InsertEntry</code> object.
 * <p>
 * <pre>
 * &lt;xs:element name="InsertEntry" type="InsertEntryType">
 * &lt;complexType name="InsertEntryType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:liberty:disco:2003-08}ResourceOffering"/>
 *         &lt;any/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class InsertEntry {

    private ResourceOffering offering = null;
    private List any = null;

    /**
     * Constructor.
     * @param resourceOffering discovery Resource offering to be inserted. 
     * @param any List of Directive object, this is to allow the requester to
     *     include directives about the resource offering being inserted.
     */
    public InsertEntry(
        com.sun.identity.liberty.ws.disco.ResourceOffering resourceOffering,
        java.util.List any)
    {
        offering = resourceOffering;
        this.any = any;
    }

    /**
     * Constructor.
     * @param elem <code>InsertEntry</code> DOM element
     * @exception DiscoveryException if error occurs
     */
    public InsertEntry(Element elem) throws DiscoveryException {
        if (elem == null) {
            DiscoUtils.debug.message("InsertEntry(Element): null input.");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        String nodeName;
        String nameSpaceURI;
        if (((nodeName = elem.getLocalName()) == null) ||
            (!nodeName.equals("InsertEntry")) ||
            ((nameSpaceURI = elem.getNamespaceURI()) == null) ||
            (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
        {
            DiscoUtils.debug.message("InsertEntry(Element): wrong input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("wrongInput"));
        }

        NodeList contentnl = elem.getChildNodes();
        Node child;
        for (int i = 0, length = contentnl.getLength(); i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                nameSpaceURI = ((Element) child).getNamespaceURI();
                if ((nodeName.equals("ResourceOffering")) &&
                    (nameSpaceURI != null) &&
                    (nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
                {
                    if (offering != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("InsertEntry(Element): "
                                + "included more than one ResourceOffering.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    offering = new ResourceOffering((Element) child);
                } else {
                    Directive directive = new Directive((Element) child);
                    if (any == null) {
                        any = new ArrayList();
                    }
                    any.add(directive);
                }
            }
        }

        if (offering == null) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("InsertEntry(Element): missing "
                    + "ResourceOffering.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingResourceOffering"));
        }
    }

    /**
     * Gets the resource offering to be inserted. 
     *
     * @return the resource offering to be inserted.
     * @see #setResourceOffering(ResourceOffering)
     */
    public ResourceOffering getResourceOffering() {
        return offering;
    }

    /**
     * Sets the resource offering to be inserted. 
     *
     * @param value the resource offering to be inserted.
     * @see #getResourceOffering()
     */
    public void setResourceOffering(ResourceOffering value) {
        offering = value;
    }

    /**
     * Gets the value of the Any property.
     *
     * @return List of <code>com.sun.identity.liberty.ws.disco.Directive</code>
     *                objects.
     * @see #setAny(List)
     */
    public List getAny() {
        return any;
    }

    /**
     * Sets the value of the Any property.
     *
     * @param any List of
     *  <code>com.sun.identity.liberty.ws.disco.Directive</code> objects.
     * @see #getAny()
     */
    public void setAny(List any) {
        this.any = any;
    }

    /**
     * Gets string format.
     *
     * @return formatted String.
     */ 
    public java.lang.String toString() {
        StringBuffer sb = new StringBuffer(1000);
        sb.append("<InsertEntry xmlns=\"").append(DiscoConstants.DISCO_NS).
            append("\">");
        if (offering != null) {
            sb.append(offering);
        }
        if (any != null) {
            Iterator iter = any.iterator();
            while (iter.hasNext()) {
                sb.append(iter.next().toString());
            }
        }
        sb.append("</InsertEntry>");
        return sb.toString();
    }
}
