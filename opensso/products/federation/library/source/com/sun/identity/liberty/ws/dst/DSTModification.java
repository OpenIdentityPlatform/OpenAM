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
 * $Id: DSTModification.java,v 1.2 2008/06/25 05:47:13 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.dst;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.DateUtils;

/**
 * The <code>DSTModification</code> class represents a <code>DST</code>
 * modification operation. 
 * 
 * <p>The following schema fragment specifies the expected content within
 * the <code>DSTModification</code> object.
 * <p>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Select"
 *         type="{urn:liberty:idpp:2003-08}SelectType"/>
 *         &lt;element name="NewData" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}
 *               anyType">
 *                 &lt;sequence>
 *                   &lt;any/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="overrideAllowed"
 *       type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class DSTModification {
    private String id;
    private String select;
    private Date notChangedSince;
    private boolean overrideAllowed = false;
    private List newData = new ArrayList();
    private String nameSpaceURI = null;
    private String prefix = null;

    /**
     * Default constructor
     */
    public DSTModification() {}

 
    /**
     * Constructor
     * @param select identifies the data service to be modified
     * @param notChangedSince  match only entries that are not changed 
     *        after the specified date.
     * @param serviceNS service namespace
     */
    public DSTModification(String select, 
                           Date notChangedSince,
                           String serviceNS) {
        this.select = select;
        this.notChangedSince = notChangedSince;
        nameSpaceURI = serviceNS;
    }

    /**
     * Constructor
     *
     * @param element <code>DOM</code> Element.
     * @throws DSTException
     */
    public DSTModification(org.w3c.dom.Element element) throws DSTException{
        if(element == null) {
           DSTUtils.debug.error("DSTModification(element):null input");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }
        String elementName = element.getLocalName();
        if(elementName == null || !elementName.equals("Modification")) {
           DSTUtils.debug.error("DSTModification(element):Invalid elementName");
           throw new DSTException(DSTUtils.bundle.getString("invalidElement"));
        }
        nameSpaceURI = element.getNamespaceURI();
        prefix = element.getPrefix();
        if(nameSpaceURI == null) {
           DSTUtils.debug.error("DSTModification(element): Namespace is null");
           throw new DSTException(DSTUtils.bundle.getString("noNameSpace"));
        }
        id = element.getAttribute("id");
        String attr = element.getAttribute("overrideAllowed");
        if(attr != null) {
           overrideAllowed = Boolean.valueOf(attr).booleanValue();
        }
        attr = element.getAttribute("notChangedSince");

        if(attr != null && attr.length() != 0) {
            try {
                notChangedSince = DateUtils.stringToDate(attr);
            } catch(ParseException ex) {
                DSTUtils.debug.error(
                    "DSTModification(element): date can not be parsed.", ex); 
            }
        }

        NodeList list = element.getElementsByTagNameNS(
                        nameSpaceURI, "Select");

        if((list.getLength() != 1)) {
           DSTUtils.debug.error("DSTModification(element): Select is null" +
           " or more than one select found.");
           throw new DSTException(
           DSTUtils.bundle.getString("invalidSelect"));
        }
        select = XMLUtils.getElementValue((Element)list.item(0));
        if(select == null) {
           DSTUtils.debug.error("DSTModification(element): Select is null" );
           throw new DSTException(
           DSTUtils.bundle.getString("invalidSelect"));
        }
        NodeList newDataElements = element.getElementsByTagNameNS(
                 nameSpaceURI, "NewData"); 
        if(newDataElements.getLength() != 1) {
           DSTUtils.debug.error("DSTModification(element): Modification can"+
           "not have more than one new data elements.");
           throw new DSTException(
           DSTUtils.bundle.getString("invalidNewData"));
        }
        Node newDataElement = newDataElements.item(0);
        NodeList dataElements = newDataElement.getChildNodes();
        int size = dataElements.getLength();
        for(int i=0; i < size; i++) {
           Node node = dataElements.item(0);
           if(node.getNodeType() == Node.ELEMENT_NODE) {
              newData.add((Element)node); 
           }
        }
    }

    /**
     * Gets id attribute 
     * @return 
     * {@link java.lang.String}
     */
    public java.lang.String getId() {
        return id;
    } 

    /**
     * Sets id attribute
     * @param id id attribute value to be set.
     */
    public void  setId(java.lang.String id) {
        this.id = id;
    }


    /**
     * Gets new data value 
     * @return  
     * {@link java.util.List}
     */
    public java.util.List getNewDataValue() {
        return newData;
    }

    /**
     * Sets new data value 
     * @param value list of Data XML DOM Elements 
     * 
     */
    public void setNewDataValue(java.util.List value) {
        if(value != null && !value.isEmpty()) {
           newData.addAll(value);
        }
    }

    /**
     * Checks if override is allowed
     * @return if true, means override is allowed, false otherwise
     */
    public boolean isOverrideAllowed() {
        return overrideAllowed;
    }

    /**
     * Sets if override is allowed
     * @param value if true, means override is allowed, false otherwise
     */
    public void setOverrideAllowed(boolean value) {
        this.overrideAllowed = value;
    }

    /**
     * Gets select element 
     * @return the select element as string
     */
    public java.lang.String getSelect() {
        return select;
    }

    /**
     * Sets select element 
     * @param value select value to be set  
     */
    public void setSelect(java.lang.String value) {
        this.select = value;
    }

    /**
     * Gets the <code>NotChangedSince</code> attribute.
     * @return Date for the <code>NotChangedSince</code> attribute 
     */
    public Date getNotChangedSince() {
        return notChangedSince;
    }

    /**
     * Sets <code>NotChangedSince</code> attribute.
     * @param value value of the <code>NotChangedSince</code> attribute to be
     *        set.
     */
    public void setNotChangedSince(java.util.Date value) {
        this.notChangedSince = value;
    }

    /**
     * Gets the name space.
     * @return String NameSpace String
     */
    public java.lang.String getNameSpaceURI() {
        return nameSpaceURI;
    }

    /**
     * Sets the name space.
     * @param nameSpace NameSpace URI
     */
    public void setNameSpaceURI(String nameSpace) {
        this.nameSpaceURI = nameSpace;
    }

    /**
     * Sets the name space prefix.
     * @param prefix NameSpace prefix.
     */
    public void setNameSpacePrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the name space prefix.
     * @return String NameSpace prefix.
     */
    public java.lang.String getNameSpacePrefix() {
        return prefix;
    }

    /**
     * Creates a String representation of this object.
     * By default name space name is prepended to the element name
     * @return String A string containing the valid XML for this element
     */
    public java.lang.String toString() {
        return toString(true, false);
    }

    /**
     * Creates a String representation of this object.
     * @param includeNS if true prepends all elements by their Namespace prefix
     * @param declareNS if true includes the namespace within the
     *                  generated.
     * @return String A string containing the valid XML for this element
     */
    public java.lang.String toString(boolean includeNS, boolean declareNS) {

        String tempPrefix = "";
        if(includeNS) {
           if(prefix == null) {
              prefix = DSTConstants.DEFAULT_NS_PREFIX;
           }
           tempPrefix = prefix + ":";
        }
        if(declareNS) {
           if(nameSpaceURI == null) {
              DSTUtils.debug.error("DSTModification.toString: Name Space is " +
              "not defined");
              return "";
           }
        }
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(tempPrefix).append("Modification");
        if(id != null && id.length() != 0) {
           sb.append(" id=\"").append(id).append("\"");
        }
        sb.append(" overrideAllowed=\"");
        if(overrideAllowed) {
           sb.append("true").append("\"");
        } else {
           sb.append("false").append("\"");
        }

        if(notChangedSince != null) {
            sb.append(" notChangedSince=\"")
                .append(DateUtils.toUTCDateFormat(notChangedSince))
                .append("\"");
        }

        if(declareNS) {
           sb.append(" xmlns:").append(prefix).append("=\"")
             .append(nameSpaceURI).append("\"")
             .append(" xmlns=\"").append(nameSpaceURI).append("\"");
        }
        sb.append(">")
          .append("<").append(tempPrefix).append("Select").append(">")
          .append(appendPrefix(select, prefix)).append("</")
          .append(tempPrefix).append("Select").append(">")
          .append("<").append(tempPrefix).append("NewData").append(">");
        Iterator iter = newData.iterator();
        while(iter.hasNext()) {
           Node node = (Node)iter.next();
           sb.append(XMLUtils.print(node));
        }
        sb.append("</").append(tempPrefix).append("NewData").append(">")
          .append("</").append(tempPrefix).append("Modification").append(">");

        return sb.toString();
    }

    private String appendPrefix(String select, String prefix) {
        if(select.indexOf(":") != -1) {
           // prefix is already defined.
           return select;
        }
        StringBuffer sb = new StringBuffer(100);
        StringTokenizer st = new StringTokenizer(select, "/");
        while(st.hasMoreTokens()) {
           String temp = (String)st.nextToken();
           temp = "/" + prefix + ":" + temp;
           sb.append(temp);
        }
        return sb.toString();
    }

}
