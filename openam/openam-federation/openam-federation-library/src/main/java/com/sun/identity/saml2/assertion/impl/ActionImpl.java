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
 * $Id: ActionImpl.java,v 1.2 2008/06/25 05:47:42 qcheng Exp $
 *
 */

package com.sun.identity.saml2.assertion.impl;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.assertion.Action;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;

/**
 * This class is an implementation of interface <code>Action</code>.
 * The <code>Action</code> element specifies an action on the specified
 * resource for which permission is sought. Its type is <code>ActionType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="ActionType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="Namespace" use="required"
 *       type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 *
 */
public class ActionImpl implements Action {

    private String action = null;
    private String namespace = null;
    private boolean mutable = true;

    // used by constructors
    private void parseElement(Element element)
        throws SAML2Exception
    {
        // make sure that the input xml block is not null
        if (element == null) {
            SAML2SDKUtils.debug.message("ActionImpl.parseElement:"+
                " Input is null.");
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an Action.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("Action"))) {
            SAML2SDKUtils.debug.message("ActionImpl.parseElement: not Action.");
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the attribute of <Action> element
        NamedNodeMap atts = ((Node)element).getAttributes();
        if (atts != null) {
            Node att = atts.getNamedItem("Namespace");
            if (att != null) {
                namespace = ((Attr)att).getValue().trim();
            }
        }
        if (namespace == null || namespace.length() == 0) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ActionImpl.parseElement: "+
                    "Namespace is empty or missing.");
            }
            throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("missingAttribute"));
        }

        //handle the children elements of <Action>
        NodeList  nodes = element.getChildNodes();
        int nodeCount = nodes.getLength();
        if (nodeCount > 0) {
            for (int i = 0; i < nodeCount; i++) {
                Node currentNode = nodes.item(i);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (SAML2SDKUtils.debug.messageEnabled()) {
                        SAML2SDKUtils.debug.message("ActionImpl.parseElement: "
                            + "Illegal value of the element.");
                    }
                    throw new SAML2Exception(
                              SAML2SDKUtils.bundle.getString("wrongInput"));
                }
            }
        }
        action = XMLUtils.getElementValue(element);
        // check if the action is null.
        if (action == null || action.trim().length() == 0) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ActionImpl.parseElement: "+
                    "Action value is null or empty.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("missingElementValue"));
        }

        mutable = false;
    }

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public ActionImpl() {
    }

    /**
     * Class constructor with <code>Action</code> in <code>Element</code>
     * format.
     */
    public ActionImpl(org.w3c.dom.Element element)
        throws SAML2Exception
    {
        parseElement(element);
    }

    /**
     * Class constructor with <code>Action</code> in xml string format.
     */
    public ActionImpl(String xmlString)
        throws SAML2Exception
    {
        Document doc = XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
    }

    /**
     * Makes the object immutable.
     */
    public void makeImmutable() {
        mutable = false;
    }

    /**
     * Returns the mutability of the object.
     *
     * @return true if the object is mutable; false otherwise.
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Returns the value of the value property.
     *
     * @return A String label for the action.
     * @see #setValue(String)
     */
    public String getValue() {
        return action;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value A String lable for the action to be set.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue()
     */
    public void setValue(String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        action = value;
    }

    /**
     * Returns the value of the <code>Namespace</code> property.
     *
     * @return A String representing <code>Namespace</code> attribute.
     * @see #setNamespace(String)
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the value of the <code>Namespace</code> property.
     *
     * @param value A String representing <code>Namespace</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getNamespace()
     */
    public void setNamespace(java.lang.String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        namespace = value;
    }

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *         By default name space name is prepended to the element name.
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString()
        throws SAML2Exception
    {
        return this.toXMLString(true, false);
    }

    /**
     * Returns a String representation of the element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *                prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *                within the Element.
     * @return A string containing the valid XML for this element
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
        throws SAML2Exception
    {
        // validate the data before output the string
        if (action == null || action.trim().length() == 0) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ActionImpl.toXMLString: "+
                    "Action value is null or empty.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("emptyElementValue"));
        }

        if (namespace == null || namespace.trim().length() == 0) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ActionImpl.toXMLString: " +
                     "Namespace is empty or missing");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("missingAttribute"));
        }

        StringBuffer result = new StringBuffer(1000);
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAML2Constants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAML2Constants.ASSERTION_DECLARE_STR;
        }

        result.append("<").append(prefix).append("Action").
                append(uri).append(" Namespace=\"").append(namespace).
                append("\">");
        result.append(action);
        result.append("</").append(prefix).append("Action>");
        return ((String)result.toString());
    }

}
