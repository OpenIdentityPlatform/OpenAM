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
 * $Id: GetComplete.java,v 1.2 2008/06/25 05:46:47 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message.common;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class contains methods for the <code>GetComplete</code>
 * Element. This element specifies a URI which resolves to
 * the complete IDPList.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated

public class GetComplete extends EntityName {
    
    /**
     * Default Constructor.
     */
    public GetComplete() {
    }
    
    /**
     * Constructor create <code>GetComplete</code> object.
     *
     * @param uri the value of the <code>URI</code>.
     */
    public GetComplete(String uri) {
        super(uri);
    }
    
    /**
     * Constructor create <code>GetComplete</code> object.
     *
     * @param uri the value of the <code>URI</code>.
     * @param otherElements list of other elements.
     */
    public GetComplete(String uri, List otherElements) {
        super(uri, otherElements);
    }
    
    /**
     * Constructor creates <code>GetComplete</code> object from
     * the Document Element.
     *
     * @param root the Document Element object.
     * @throws FSMsgException if error creating this object.
     */
    public GetComplete(Element root) throws FSMsgException {
        String tag = null;
        if (root == null) {
            FSUtils.debug.message("GetComplete(Element): null input.");
            throw new FSMsgException("nullInput",null);
        }
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals("GetComplete"))) {
            FSUtils.debug.message("GetComplete(Element): wrong input");
            throw new FSMsgException("wrongInput",null);
        }
        
        int length = 0;
        // get the contents of the request
        NodeList contentnl = root.getChildNodes();
        Node child;
        String nodeName;
        length = contentnl.getLength();
        for (int i = 0; i < length; i++) {
            child = contentnl.item(i);
            nodeName = child.getLocalName();
            if ((nodeName != null) &&  nodeName.equals("URI")) {
                // make sure the providerId is not assigned already
                if (uri != null) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("GetComplete(Element): should"
                                + "contain only one URI.");
                    }
                    throw new FSMsgException("wrongInput",null);
                }
                uri = XMLUtils.getElementValue((Element) child);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("GetComplete(Element): invalid"
                            + " node" + nodeName);
                }
                throw new FSMsgException("wrongInput",null);
            }
        }
    }

/**
 * Returns <code>GetComplete</code> object. This
 * object is created by parsing the <code>XML</code> string.
 *
 * @param xml <code>XML</code> String
 * @return the <code>GetComplete</code> object.
 * @throws FSMsgException if there is an error creating this object.
 */
public static GetComplete parseXML(String xml) throws FSMsgException {
    Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
    if (doc == null) {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("GetComplete.parseXML:Error "
                    + "while parsing input xml string");
        }
        throw new FSMsgException("parseError",null);
    }
    Element root = doc.getDocumentElement();
    return new GetComplete(root);
}

/**
 * Returns the string representation of this object.
 *
 * @return An XML String representing this object.
 *
 */
public String toXMLString() throws FSMsgException {
    return toXMLString(true, true);
}

    /**
     * Returns a String representation of the &lt;samlp:Response&gt; element.
     *
     * @param includeNS : Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *        within the Element.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
    throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }
    
    /**
     * Returns a String representation of the &lt;samlp:Response&gt; element.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the xml
     *        declaration header.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */

    public String toXMLString(boolean includeNS,boolean declareNS,
            boolean includeHeader) throws FSMsgException {
        StringBuffer xml = new StringBuffer(300);
        if (includeHeader) {
            xml.append("<?xml version=\"1.0\" encoding=\"").
                    append(IFSConstants.DEFAULT_ENCODING).append("\" ?>");
        }
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            uri = IFSConstants.LIB_NAMESPACE_STRING;
        }
        
        xml.append("<").append(prefix).append("GetComplete").append(uri).
                append(">\n");
        
        xml.append("<").append(prefix).append("URI").append(uri).append(">").
                append(this.uri).
                append("</").append(prefix).append("URI").append(">");
        
        xml.append("</").append(prefix).append("GetComplete>");
        
        return xml.toString();
    }
}
