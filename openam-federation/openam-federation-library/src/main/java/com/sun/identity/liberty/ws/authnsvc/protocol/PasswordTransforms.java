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
 * $Id: PasswordTransforms.java,v 1.2 2008/06/25 05:47:08 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS.
 */

package com.sun.identity.liberty.ws.authnsvc.protocol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.liberty.ws.authnsvc.AuthnSvcConstants;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcException;

/**
 * The <code>PasswordTransforms</code> class represents 'PasswordTransforms'
 * element defined in Authentication Service schema.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class PasswordTransforms {

    private List transforms = null;

    /**
     * This is the default constructor.
     */
    public PasswordTransforms(List transforms)
    {
        this.transforms = transforms;
    }

    /**
     * This constructor takes a <code>org.w3c.dom.Element</code>.
     * @param element a PasswordTransforms element
     * @exception AuthnSvcException if an error occurs while parsing the
     *                              PasswordTransforms element
     */
    PasswordTransforms(Element element) throws AuthnSvcException
    {
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();

        for(int i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element)child;
                String localName = childElement.getLocalName();
                String namespaceURI = childElement.getNamespaceURI();

                if (AuthnSvcConstants.NS_AUTHN_SVC.equals(namespaceURI) &&
                    AuthnSvcConstants.TAG_TRANSFORM.equals(localName)) {

                    Transform tf = Transform.getTransform(childElement);
                    if (transforms == null) {
                        transforms = new ArrayList();
                    }
                    transforms.add(tf);
                } else {
                    throw new AuthnSvcException("invalidChildPT");
                }
            }
        }
    }

    /**
     * Returns a list of child 'Transforms' Elements
     * @return a list of child 'Transforms' Elements
     * @see #setTransforms(List)
     */
    public List getTransforms()
    {
        return transforms;
    }

    /**
     * Sets a list of child 'Transforms' Elements.
     * @param transforms a list of child 'Transforms' Element
     * @see #getTransforms()
     */
    public void setTransforms(List transforms)
    {
        this.transforms = transforms;
    }

    /**
     * Converts this to <code>org.w3c.dom.Element</code> and add to
     * parent SASLResponse Element.
     * @param respE parent SASLResponse Element
     * @exception AuthnSvcException if there is no child
     */
    void addToParent(Element respE) throws AuthnSvcException
    {
        if (transforms == null || transforms.isEmpty()) {
            throw new AuthnSvcException("noChildPT");
        }
        Document doc = respE.getOwnerDocument();
        Element ptE = doc.createElementNS(
                                AuthnSvcConstants.NS_AUTHN_SVC,
                                AuthnSvcConstants.PTAG_PASSWORD_TRANSFORMS);
        respE.appendChild(ptE);

        for(Iterator iter = transforms.iterator(); iter.hasNext(); ) {
            Transform tf = (Transform)iter.next();
            tf.addToParent(ptE);
        }
    }
}
