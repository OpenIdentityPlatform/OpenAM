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
 * $Id: DSTQueryItem.java,v 1.2 2008/06/25 05:47:13 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.dst;

import java.text.ParseException;
import java.util.Date;
import java.util.StringTokenizer;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

/**
 * The class <code>DSTQueryItem</code> is the wrapper for one query item
 * for Data service.
 * The following schema fragment specifies the expected content within the
 * <code>DSTQueryItem</code> object.
 * <pre>
 * &lt;element name="QueryItem" maxOccurs="unbounded">
 *   &lt;complexType>
 *     &lt;complexContent>
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *         &lt;sequence>
 *           &lt;element name="Select"
 *           type="{urn:liberty:id-sis-pp:2003-08}SelectType"/>
 *         &lt;/sequence>
 *         &lt;attribute name="itemID"
 *         type="{urn:liberty:id-sis-pp:2003-08}IDType" />
 *         &lt;attribute name="changedSince"
 *         type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *         &lt;attribute name="includeCommonAttributes" 
 *         type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *         &lt;attribute name="id" 
 *         type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;/restriction>
 *     &lt;/complexContent>
 *   &lt;/complexType>
 * &lt;/element>
 * </pre>
 *
 * @supported.all.api
 */
public class DSTQueryItem {

    private String select; 
    private boolean includeCommonAttribute;
    private Date changedSince;
    private String itemID = null;
    private String id = null;
    private String nameSpaceURI = null;
    private String prefix = null;

    /**
     * Constructor
     * @param select specifies the data the query  should return 
     * @param serviceNS service Name space
     */
    public DSTQueryItem (String select, String serviceNS) { 
        this.select = select;
        this.nameSpaceURI = serviceNS;
    }
   
    /**
     * Constructor
     * @param select specifies the data the query  should return 
     * @param includeCommonAttribute if true, query response will 
     *        contains common attributes (attribute id and modification 
     *        time)
     * @param changedSince Only match entries changed after the specified
     *        date
     * @param serviceNS service Name space
     */
    public DSTQueryItem (String select, 
                         boolean includeCommonAttribute,
                         Date changedSince,
                         String serviceNS) {
        this.select = select;
        this.includeCommonAttribute = includeCommonAttribute;
        this.changedSince = changedSince; 
        this.nameSpaceURI = serviceNS;
    }

    /**
     * Constructor
     * @param element <code>DOM</code> Element 
     * @throws DSTException
     */
    public DSTQueryItem(org.w3c.dom.Element element) throws DSTException{
        if(element == null) {
           DSTUtils.debug.error("DSTQueryItem(element):null input");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }
        String elementName = element.getLocalName();
        if(elementName == null || !elementName.equals("QueryItem")) {
           DSTUtils.debug.error("DSTQueryItem(element):Invalid elementName");
           throw new DSTException(DSTUtils.bundle.getString("invalidElement"));
        }
        nameSpaceURI = element.getNamespaceURI();
        if(nameSpaceURI == null) {
           DSTUtils.debug.error("DSTQueryItem(element): Namespace is null");
           throw new DSTException(DSTUtils.bundle.getString("noNameSpace"));
        }
        prefix = element.getPrefix();
        id = element.getAttribute("id");
        String attr = element.getAttribute("includeCommonAttributes");
        if(attr != null) {
           includeCommonAttribute = Boolean.valueOf(attr).booleanValue();
        }
        attr = element.getAttribute("changedSince");

        if (attr != null && attr.length() != 0) {
            try {
                changedSince = DateUtils.stringToDate(attr);
            } catch(ParseException ex) {
                DSTUtils.debug.error(
                    "DSTQueryItem(element): date can not be parsed.", ex);
            }
        }

        NodeList list = element.getElementsByTagNameNS(
                        nameSpaceURI, "Select");

        if((list.getLength() != 1)) {
           DSTUtils.debug.error("DSTQueryItem(element): Select is null" +
           " or more than one select found.");
           throw new DSTException(
           DSTUtils.bundle.getString("invalidSelect"));
        }
        select = XMLUtils.getElementValue((Element)list.item(0));
        if(select == null) {
           DSTUtils.debug.error("DSTQueryItem(element): Select is null" );
           throw new DSTException(
           DSTUtils.bundle.getString("invalidSelect"));
        }
    }


    /**
     * Returns data selection string 
     * @return String
     */
    public String getSelect() {
        return select;
    }

    /**
     * Gets <code>itemID</code> attribute
     * @return String
     */
    public String getItemID() {
        return itemID;
    }

    /**
     * Sets <code>itemID</code> attribute
     * @param itemID item ID to be set
     */
     public void setItemID(String itemID) {
         this.itemID = itemID;
     }

    /**
     * Gets id attribute.
     *
     * @return id attribute.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets id attribute
     * @param id id attribute to be set
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Checks include common attribute for the <code>DST</code> query item.
     * @return boolean
     */
    public boolean isIncludeCommonAttributes() { 
        return includeCommonAttribute;
    }

   /**
    * Gets changed since attribute
    * @return Date
    */
    public Date getChangedSince() {
        return changedSince;
    }

    /**
     * Gets the name space.
     * @return Name space.
     */
    public java.lang.String getNameSpaceURI() {
        return nameSpaceURI;
    }

    /**
     * Sets the name space.
     *
     * @param nameSpace Name space URI.
     */
    public void setNameSpaceURI(String nameSpace) {
        this.nameSpaceURI = nameSpace;
    }

    /**
     * Sets the name space prefix.
     * @param prefix Name space prefix.
     */
    public void setNameSpacePrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the name space prefix.
     * @return String Name space prefix.
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
     * @param includeNS if true prepends all elements by their Name space prefix
     * @param declareNS if true includes the Name space within the
     *                  generated.
     * @return String A string containing the valid XML for this element
     */
    public java.lang.String toString(boolean includeNS, boolean declareNS) {

        if(select == null) {
           DSTUtils.debug.error("DSTQueryItem.toString: Select cannot be null");
           return "";
        }
        String tempPrefix = "";
        if(includeNS) { 
           if(prefix == null) {
              prefix = DSTConstants.DEFAULT_NS_PREFIX;
           }
           tempPrefix = prefix + ":";   
        }
        if (declareNS && nameSpaceURI == null) {
            DSTUtils.debug.error("DSTQueryItem.toString: Name Space is " +
              "not defined");
            return "";
        }
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(tempPrefix).append("QueryItem");
        if(id != null && id.length() != 0) {
           sb.append(" id=\"").append(id).append("\"");
        }
        sb.append(" includeCommonAttributes=\"");
        if(includeCommonAttribute) {
           sb.append("true").append("\"");
        } else {
           sb.append("false").append("\"");
        }
        if(itemID != null && itemID.length() != 0) {
           sb.append(" itemID=\"").append(itemID).append("\"");
        }

        if (changedSince != null) {
            sb.append(" changedSince=\"")
                .append(DateUtils.toUTCDateFormat(changedSince))
                .append("\"");
        }

        if(declareNS) {
           sb.append(" xmlns:").append(prefix).append("=\"")
             .append(nameSpaceURI).append("\"")
             .append(" xmlns=\"").append(nameSpaceURI).append("\"");
        }
        sb.append(">").append("<").append(tempPrefix).append("Select")
          .append(">").append(appendPrefix(select, prefix)).append("</")
          .append(tempPrefix).append("Select").append(">")
          .append("</").append(tempPrefix).append("QueryItem").append(">");

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
