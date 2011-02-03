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
 * $Id: DSTQueryResponse.java,v 1.2 2008/06/25 05:47:13 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.dst;

import java.text.ParseException;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.liberty.ws.common.Status;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The class <code>DSTQueryResponse</code> represents a <code>DST</code> query
 * response.
 * The following schema fragment specifies the expected content within
 * the <code>DSTQueryResponse</code> object.
 * <pre>
 * &lt;complexType name="QueryResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:liberty:idpp:2003-08}Status"/>
 *         &lt;element name="Data" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction 
 *               base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;any/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="itemIDRef" 
 *                 type="{urn:liberty:idpp:2003-08}IDReferenceType" />
 *                 &lt;attribute name="id"
 *                 type="{http://www.w3.org/2001/XMLSchema}ID" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element ref="{urn:liberty:idpp:2003-08}Extension" 
 *         maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="timeStamp" 
 *       type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *       &lt;attribute name="itemIDRef"
 *       type="{urn:liberty:idpp:2003-08}IDReferenceType" />
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @supported.all.api 
 */
public class DSTQueryResponse {

     private Status dstStatus = null;
     private String itemIDRef = null;
     private String id = null;
     private Date dateStamp = null;
     private List data = new ArrayList();
     private List extensions = new ArrayList();
     private String nameSpaceURI = null;
     private String prefix = null;
  
    /**
     * Default constructor
     */
    public DSTQueryResponse() {}
 
    /**
     * Constructor
     * @param data List of <code>DSTData</code> objects 
     * @param serviceNS service name space
     */
    public DSTQueryResponse(java.util.List data, String serviceNS) {
        if (data != null) {
            this.data = data;
        }
        nameSpaceURI = serviceNS;
    }

    /**
     * Constructor
     *
     * @param element <code>DOM</code> Element 
     * @throws DSTException
     */
    public DSTQueryResponse(org.w3c.dom.Element element) throws DSTException {
        if(element == null) {
           DSTUtils.debug.error("DSTQueryResponse(element):null input");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }
        String elementName = element.getLocalName();
        if(elementName == null || !elementName.equals("QueryResponse")) {
           DSTUtils.debug.error("DSTQueryResponse(element):Invalid " +
           "element name");
           throw new DSTException(DSTUtils.bundle.getString("invalidElement"));
        }
        nameSpaceURI = element.getNamespaceURI();
        if(nameSpaceURI == null) {
           DSTUtils.debug.error("DSTQueryResponse(element): NameSpace is" +
           " not defined");
           throw new DSTException(DSTUtils.bundle.getString("noNameSpace"));
        }
        prefix = element.getPrefix();
        id = element.getAttribute("id");
        itemIDRef = element.getAttribute("itemIDRef");
        String attrib = element.getAttribute("timeStamp");

        if(attrib != null && attrib.length() != 0) {
            try {
                dateStamp = DateUtils.stringToDate(attrib);
            } catch (ParseException ex) {
                DSTUtils.debug.error(
                    "DSTQueryResponse(element): can not parse the date", ex);
            }
        }
        NodeList list = element.getChildNodes();
        if(list == null || list.getLength() == 0) {
           DSTUtils.debug.error("DSTQueryResponse(element): Response does" +
           "not have child elements.");
           throw new DSTException(DSTUtils.bundle.getString("noStatus"));
        }
        for(int i=0; i < list.getLength(); i++) {
           Node node = list.item(i);
           if(node.getNodeType() != Node.ELEMENT_NODE) {
              continue;
           }
           String nodeName = node.getLocalName();

           if(nodeName != null && nodeName.equals("Status")) {
             dstStatus = DSTUtils.parseStatus((Element)node);

           } else if(nodeName != null && nodeName.equals("Data")) {
             data.add(new DSTData((Element)node));

           } else {
             DSTUtils.debug.error("DSTQueryResponse(element): Response does" +
             " have invalid elements.");
             throw new DSTException(
              DSTUtils.bundle.getString("invalidElement"));
           }
        }

        if(dstStatus == null) {
           DSTUtils.debug.error("DSTQueryResponse(element): Response does" +
           "not have Status element.");
           throw new DSTException(DSTUtils.bundle.getString("noStatus"));
        }
    }

    /**
     * Gets status for the query response
     * @return Status
     */
    public com.sun.identity.liberty.ws.common.Status getStatus() {
        return dstStatus;
    }


    /**
     * Sets status for the query response
     * @param status Status object to be set
     */
    public void setStatus(com.sun.identity.liberty.ws.common.Status status) {
        this.dstStatus = status;
    }

    /**
     * Gets time stamp
     * @return Date
     */
    public java.util.Date getTimeStamp() {
        return dateStamp;
    }

    /**
     * Sets time stamp
     * @param date Date to be set
     */
    public void setTimeStamp(java.util.Date date) {
        this.dateStamp = date;
    }

    /**
     * Gets id attribute.
     *
     * @return id attribute.
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets id attribute
     *
     * @param id id attribute to be set 
     */
    public void setId(java.lang.String id) {
        this.id = id;
    }

    /**
     * Gets item reference.
     *
     * @return item reference.
     */
    public java.lang.String getItemIDRef() {
        return itemIDRef;
    }

    /**
     * Sets item reference.
     *
     * @param ref item ID reference to be set.
     */
    public void setItemIDRef(java.lang.String ref) {
        this.itemIDRef = ref;
    }

    /**
     * Gets the value of the Data property.
     * 
     * @return List of <code>DSTData</code> objects
     * 
     */
    public java.util.List getData() {
        return data;
    }

    /**
     * Gets the extension property.
     *
     * @return List of any <code>java.lang.Object</code>
     * 
     */
    public java.util.List getExtension() {
        return extensions;
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
     * @param prefix Name Space prefix.
     */
    public void setNameSpacePrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the name space prefix.
     * @return Name Space prefix.
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
     * @param includeNS if true prepends all elements by their name space 
     *                  prefix
     * @param declareNS if true includes the name space  within the
     *                  generated.
     * @return String A string containing the valid XML for this element
     */
    public java.lang.String toString(boolean includeNS, boolean declareNS) {
   
        if(dstStatus == null) {
           DSTUtils.debug.error("DSTQueryResponse.toString: Status is null");
           return "";
        }

        String tempPrefix = "";
        if(includeNS) {
           if(prefix == null) {
              prefix = DSTConstants.DEFAULT_NS_PREFIX;
           }
           tempPrefix = prefix + ":";
        }
        if(declareNS) {
           if(nameSpaceURI == null) {
              DSTUtils.debug.error("DSTQueryResponse.toString: Name Space is " +
              "not defined");
              return "";
           }
        }
        StringBuffer sb = new StringBuffer(500);
        sb.append("<").append(tempPrefix).append("QueryResponse");
        if(id != null && id.length() != 0) {
           sb.append(" id=\"").append(id).append("\"");
        }
        if(itemIDRef != null && itemIDRef.length() != 0) {
           sb.append(" itemIDRef=\"").append(itemIDRef).append("\"");
        }

        if (dateStamp != null) {
            sb.append(" timeStamp=\"")
                .append(DateUtils.toUTCDateFormat(dateStamp))
                .append("\"");
        }
        if(declareNS) {
           sb.append(" xmlns:").append(prefix).append("=\"")
             .append(nameSpaceURI).append("\"")
             .append(" xmlns=\"").append(nameSpaceURI).append("\"");
        }

        sb.append(">").append(DSTConstants.NL).append(dstStatus.toString());
        Iterator iter = data.iterator();
        while(iter.hasNext()) {
           DSTData dstData = (DSTData)iter.next(); 
           sb.append(dstData.toString());
        }
        sb.append("</").append(tempPrefix).append("QueryResponse").append(">");
        return sb.toString();
    }


}
