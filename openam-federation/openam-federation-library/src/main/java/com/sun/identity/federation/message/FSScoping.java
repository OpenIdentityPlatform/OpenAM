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
 * $Id: FSScoping.java,v 1.2 2008/06/25 05:46:45 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.message.common.IDPEntries;
import com.sun.identity.federation.message.common.IDPEntry;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This class <code>FSScoping</code> creates scoping element for the
 * authentication request.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSScoping {
    
    private int proxyCount = -1;
    private FSIDPList idpList = null;
    
    /**
     * Default constructor
     */
    public FSScoping() {}
    
    /**
     * Constructor creates <code>FSScoping</code> object.
     *
     * @param idpList the <code>FSIDPList</code> object.
     * @param proxyCount the number of proxies
     */
    public FSScoping(FSIDPList idpList, int proxyCount) {
        this.idpList = idpList;
        this.proxyCount = proxyCount;
    }
    
    /**
     * Constructor creates <code>FSScoping</code> object from
     * the Document Element.
     *
     * @param root the Document Element .
     * @throws FSMsgException if there is a failure creating this object.
     */
    public FSScoping(Element root) throws FSMsgException {
        if(root == null) {
            FSUtils.debug.error("FSScoping(Element): null input");
            throw new FSMsgException("nullInput", null);
        }
        String tagName = root.getLocalName();
        if(tagName == null || !tagName.equals("Scoping")) {
            FSUtils.debug.error("FSScoping(Element): wrong input");
            throw new FSMsgException("wrongInput", null);
        }
        NodeList childNodes = root.getChildNodes();
        int length = childNodes.getLength();
        for (int i=0; i < length; i++) {
            Node child = childNodes.item(i);
            String nodeName = child.getLocalName();
            if(nodeName == null) {
                continue;
            }
            if(nodeName.equals("ProxyCount")) {
                String count = XMLUtils.getElementValue((Element)child);
                try {
                    proxyCount = Integer.parseInt(count);
                } catch (NumberFormatException ne) {
                    FSUtils.debug.error("FSScoping(Element): invalid proxy" +
                            "Count", ne);
                    throw new FSMsgException("wrongInput", null);
                }
            } else if(nodeName.equals("IDPList")) {
                idpList = new FSIDPList((Element)child);
            }
        }
    }
    
    /**
     * Sets the proxy count.
     *
     * @param count number of proxies
     */
    public void setProxyCount(int count) {
        proxyCount = count;
    }
    
    /**
     * Returns the proxy count.
     *
     * @return number of proxies.
     */
    public int getProxyCount() {
        return proxyCount;
    }
    
    /**
     * Sets preferred ordered List of IDPs that is known to SP for proxying.
     *
     * @param idpList the <code>FSIDPList</code> object.
     */
    public void setIDPList(FSIDPList idpList) {
        this.idpList = idpList;
    }
    
    /**
     * Returns the preferred IDPs list in an authentication request.
     *
     * @return the <code>FSIDPList</code> object.
     */
    public FSIDPList getIDPList() {
        return idpList;
    }
    
    /**
     * Returns a <code>XML</code> string representation of this object.
     *
     * @return XML String representing this object.
     * @throws FSMsgException if there is an error creating
     *         the XML string or if the required elements to create
     *         the string do not conform to the schema.
     */
    
    public String toXMLString() throws FSMsgException {
        return toXMLString(true, true);
    }
    
    /**
     * Creates a String representation of this object.
     *
     * @param includeNS : Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *        within the Element.
     * @return string containing the valid XML for this element.
     * @throws FSMsgException if there is an error.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
    throws FSMsgException {
        StringBuffer xml = new StringBuffer(300);
        String prefix = "";
        String uri = "";
        if(includeNS) {
            prefix = IFSConstants.LIB_PREFIX;
        }
        if(declareNS) {
            uri = IFSConstants.LIB_12_NAMESPACE_STRING;
        }
        xml.append("<").append(prefix).append("Scoping")
        .append(uri).append(">\n");
        if(proxyCount >= 0) {
            xml.append("<").append(prefix).append("ProxyCount").append(">")
            .append(proxyCount).append("</").append(prefix)
            .append("ProxyCount").append(">\n");
        }
        if(idpList != null) {
            xml.append(idpList.toXMLString(true, false));
        }
        xml.append("</").append(prefix).append("Scoping").append(">\n");
        return xml.toString();
    }
    
    /**
     * Returns an URL Encoded String.
     *
     * @return a url encoded query string.
     * @throws FSMsgException if there is an error.
     */
    public String toURLEncodedQueryString() throws FSMsgException {
        
        if(proxyCount == -1) {
            FSUtils.debug.error("FSScoping.toURLEncodedQueryString: " +
                    "proxyCount is not defined.");
            throw new FSMsgException("proxyCountNotDefined",null);
        }
        
        StringBuffer sb = new StringBuffer(100);
        sb.append("ProxyCount=").append(proxyCount).append("&");
        if (idpList != null) {
            IDPEntries entries = idpList.getIDPEntries();
            if(entries != null) {
                List idps = entries.getIDPEntryList();
                if(idps != null && idps.size() != 0) {
                    Iterator iter = idps.iterator();
                    StringBuffer strProviders = new StringBuffer(100);
                    String space = "";
                    while(iter.hasNext()) {
                        IDPEntry entry = (IDPEntry)iter.next();
                        String providerID = entry.getProviderID();
                        strProviders.append(space).append(providerID);
                        space = " ";
                    }
                    sb.append("IDPEntries=").append(
                            URLEncDec.encode(strProviders.toString()));
                }
            }
        }
        sb.append(IFSConstants.AMPERSAND);
        return sb.toString();
        
    }
    
    /**
     * Returns <code>FSScoping</code> object. The
     * object is creating by parsing the <code>HttpServletRequest</code>
     * object.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @throws FSMsgException if there is an error creating this object.
     */
    public static FSScoping parseURLEncodedRequest(HttpServletRequest request) {
        
        if (request == null) {
            return null;
        }
        String count = request.getParameter("ProxyCount");
        if(count == null) {
            return null;
        }
        int proxyCount = -1;
        try {
            proxyCount = Integer.parseInt(count);
        } catch (NumberFormatException ne) {
            FSUtils.debug.error("FSScoping.parseURLEncodedRequest:" +
                    "proxyCount can not be parsed.");
            return null;
        }
        
        FSScoping scoping = new FSScoping();
        scoping.setProxyCount(proxyCount);
        
        String[] idps = request.getParameterValues("IDPEntries");
        if (idps == null || idps.length == 0) {
            return scoping;
        }
        
        List list = new ArrayList();
        for (int i=0; i < idps.length; i++) {
            String providerID = idps[i];
            IDPEntry entry = new IDPEntry(providerID, null, null);
            list.add(entry);
        }
        IDPEntries entries = new IDPEntries(list);
        FSIDPList idpsList = new FSIDPList(entries, null);
        scoping.setIDPList(idpsList);
        
        return scoping;
    }
}
