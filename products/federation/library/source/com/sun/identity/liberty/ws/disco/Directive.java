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
 * $Id: Directive.java,v 1.2 2008/06/25 05:47:10 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;

/**
 * The class <code>Directive</code> represents a discovery service
 * <code>DirectiveType</code> element. Current implementation supports the
 * following four directive types: <code>AUTHENTICATE_REQUESTER</code>,
 * <code>AUTHORIZE_REQUESTER</code>, <code>AUTHENTICATE_SESSION_CONTEXT</code>,
 * and <code>ENCRYPT_RESOURCEID</code>.
 * <p>
 * The following schema fragment specifies the expected content within the
 * <code>DirectiveType</code> object.
 * <pre>
 * &lt;complexType name="DirectiveType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="descriptionIDRefs" type="{http://www.w3.org/2001/XMLSchema}IDREFS" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class Directive {

    /**
     * <code>DirectiveType AuthenticateRequester</code>.
     */
    public static final String AUTHENTICATE_REQUESTER =
                                                "AuthenticateRequester";

    /**
     * <code>DirectiveType AuthorizeRequester</code>.
     */
    public static final String AUTHORIZE_REQUESTER = "AuthorizeRequester";


    /**
     * <code>DirectiveType AuthenticateSessionContext</code>.
     */
    public static final String AUTHENTICATE_SESSION_CONTEXT = 
                                                "AuthenticateSessionContext";

    /**
     * <code>DirectiveType EncryptResourceID</code>.
     */
    public static final String ENCRYPT_RESOURCEID = "EncryptResourceID";


    /**
     * <code>DirectiveType GenerateBearerToken</code>.
     */
    public static final String GENERATE_BEARER_TOKEN = "GenerateBearerToken";

    private String type = null;
    private List descIDRefs = null;

    /**
     * Constructs a directive instance for a type of directive.
     * @param directiveType Type of the directive.
     */
    public Directive(String directiveType) {
        type = directiveType;
    }

    /**
     * Constructs a directive instance from DOM element.
     * @param elem <code>DirectiveType</code> DOM element.
     * @exception DiscoveryException if error occurs.
     */
    public Directive(Element elem) throws DiscoveryException {
        String tag = null;
        if (elem == null) {
            DiscoUtils.debug.message("Directive(Element): null input.");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        if ((tag = elem.getLocalName()) == null) {
            DiscoUtils.debug.message("Directive(Element): wrong input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("wrongInput"));
        }
        if (tag.equals(AUTHENTICATE_REQUESTER)) {
            type = AUTHENTICATE_REQUESTER;
            setDescIDRefs(elem);
        } else if (tag.equals(AUTHORIZE_REQUESTER)) {
            type = AUTHORIZE_REQUESTER;
            setDescIDRefs(elem);
        } else if (tag.equals(AUTHENTICATE_SESSION_CONTEXT)) {
            type = AUTHENTICATE_SESSION_CONTEXT;
            setDescIDRefs(elem);
        } else if (tag.equals(ENCRYPT_RESOURCEID)) {
            type = ENCRYPT_RESOURCEID;
            setDescIDRefs(elem);
        } else if (tag.equals(GENERATE_BEARER_TOKEN)) {
            type = GENERATE_BEARER_TOKEN;
            setDescIDRefs(elem);
        } else {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("Directive(Element): not supported:"
                    + tag);
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("directiveNotSupported"));
        }
    }

    private void setDescIDRefs(Element elem) {
        String ids = elem.getAttribute("descriptionIDRefs");
        if ((ids != null) && (ids.length() != 0)) {
            StringTokenizer st = new StringTokenizer(ids);
            if (st.countTokens() > 0) {
                if (descIDRefs == null) {
                    descIDRefs = new ArrayList();
                }
                while (st.hasMoreTokens()) {
                    descIDRefs.add(st.nextToken());
                }
            }
        }
    }

    /**
     * Returns a list of description ID references. 
     * @return a list of description ID references.
     * @see #setDescriptionIDRef(List)
     */
    public List getDescriptionIDRef() {
        return descIDRefs;
    }

    /**
     * Sets a list of description ID references.
     * @param idrefs a list of description ID references to be set.
     * @see #getDescriptionIDRef()
     */
    public void setDescriptionIDRef(List idrefs) {
        descIDRefs = idrefs;
    }


    /**
     * Returns type of directive.
     * @return type of directive.
     * @see #setDirectiveType(String)
     */
    public String getDirectiveType() {
        return type;
    }

    /**
     * Sets type of the directive.
     * @param directiveType type of the directive to be set.
     * @see #getDirectiveType()
     */
    public void setDirectiveType(String directiveType) {
        type = directiveType;
    }


    /**
     * Returns the directive object in string format.
     * @return the directive object in string format.
     */ 
    public String toString() {
        String ns = null;
        if (type.equals(GENERATE_BEARER_TOKEN)) {
            ns = DiscoConstants.DISCO11_NS;
        } else {
            ns = DiscoConstants.DISCO_NS;
        }

        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(type).append(" xmlns=\"").
            append(ns).append("\"");
        if (descIDRefs != null) {
            sb.append(" descriptionIDRefs=\"");
            Iterator iter = descIDRefs.iterator();
            if (iter.hasNext()) {
                sb.append((String) iter.next());
            }
            while (iter.hasNext()) {
                sb.append(" ").append((String) iter.next());
            }
            sb.append("\"");
        }
        sb.append(">");
        sb.append("</").append(type).append(">");
        return sb.toString();
    }
}
