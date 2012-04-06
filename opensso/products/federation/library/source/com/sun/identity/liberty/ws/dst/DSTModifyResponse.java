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
 * $Id: DSTModifyResponse.java,v 1.2 2008/06/25 05:47:13 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.dst;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.liberty.ws.common.Status;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>DSTModifyResponse</code> class represents a <code>DST</code>
 * response for <code>DST</code> modify request.
 * 
 * The following schema fragment specifies the expected content within the
 * <code>DSTModifyResponse</code> object.
 * <pre>
 * &lt;complexType name="ResponseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:liberty:idpp:2003-08}Status"/>
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
public class DSTModifyResponse {

    private Status status;
    private Date timeStamp;
    private String id;
    private String itemIDRef; 
    private List extension = new ArrayList();
    private String nameSpaceURI = null;
    private String prefix = null;

    /**
     * Default constructor
     */
    public DSTModifyResponse () {}

    /**
     * Constructor
     * @param element <code>DOM</code> Element.
     * @throws DSTException
     */
    public DSTModifyResponse(org.w3c.dom.Element element) throws DSTException{
        if(element == null) {
           DSTUtils.debug.error("DSTModifyResponse(element):null input");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }
        String elementName = element.getLocalName();
        if(elementName == null || !elementName.equals("ModifyResponse")) {
           DSTUtils.debug.error("DSTModifyResponse(element):Invalid " +
           "element name");
           throw new DSTException(DSTUtils.bundle.getString("invalidElement"));
        }
        nameSpaceURI = element.getNamespaceURI();
        if(nameSpaceURI == null) {
           DSTUtils.debug.error("DSTModifyResponse(element): NameSpace" +
           " is not defined");
           throw new DSTException(DSTUtils.bundle.getString("noNameSpace"));
        }
        prefix = element.getPrefix();
        id = element.getAttribute("id");
        itemIDRef = element.getAttribute("itemIDRef");
        String attrib = element.getAttribute("timeStamp");

        if (attrib != null && attrib.length() != 0) {
            try {
                timeStamp = DateUtils.stringToDate(attrib);
            } catch(ParseException ex) {
                DSTUtils.debug.error(
                    "DSTModifyResponse(element): can not parse the date", ex); 
            }
        }

        NodeList list = element.getChildNodes();
        if(list == null || list.getLength() == 0) {
           DSTUtils.debug.error("DSTModifyResponse(element): Response does" +
           "not have status element.");
           throw new DSTException(DSTUtils.bundle.getString("noStatus"));
        }

        for(int i=0; i < list.getLength(); i++) {
           Node node = list.item(i);
           if(node.getNodeType() != Node.ELEMENT_NODE) {
              continue;
           }
           String nodeName = node.getLocalName();
           if(nodeName != null && nodeName.equals("Status")) {
              status = DSTUtils.parseStatus((Element)node);
           } else {
              DSTUtils.debug.error("DSTModifyResponse(element): Response does" +
              "have invalid element.");
              throw new DSTException(
              DSTUtils.bundle.getString("invalidElement"));
           }
        }
      
        if(status == null) {
           DSTUtils.debug.error("DSTModifyResponse(element): Response does" +
           "not have status element.");
           throw new DSTException(DSTUtils.bundle.getString("noStatus"));
        }
    }

    /**
     * Gets response status 
     * @return Status
     */
    public com.sun.identity.liberty.ws.common.Status getStatus() {
        return status;
    }

    /**
     * Sets response status 
     * @param status response status to be set 
     */
    public void setStatus(com.sun.identity.liberty.ws.common.Status status) {
        this.status = status;
    }

    /**
     * Gets time stamp 
     * @return Date for the time stamp
     */
    public java.util.Date getTimeStamp() {
        return timeStamp;
    }

    /**
     * Sets time stamp 
     * @param date time stamp date to be set
     */
    public void setTimeStamp(java.util.Date date) {
        this.timeStamp = date;
    }

    /**
     * Gets id attribute 
     * @return String id attribute value
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets id attribute
     * @param id value of id attribute to be set
     */
    public void setId(java.lang.String id) {
        this.id = id;
    }

    /**
     * Gets item id reference 
     * @return item id reference 
     */
    public java.lang.String getItemIDRef() {
        return itemIDRef;
    }

    /**
     * Sets item id reference 
     * @param value item id reference to be set 
     */
    public void setItemIDRef(java.lang.String value) {
        this.itemIDRef = value;
    }

    /**
     * Gets the extension property.
     * @return List of Object 
     * 
     */
    java.util.List getExtension() {
        return extension;
    }

    /**
     * Gets the name space.
     *
     * @return name space.
     */
    public java.lang.String getNameSpaceURI() {
        return nameSpaceURI;
    }

    /**
     * Sets the name space.
     * @param nameSpace Name space URI String
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
     * @return Name space prefix.
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
     *        prefix.
     * @param declareNS if true includes the name space within the generated.
     * @return A string containing the valid XML for this element
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
              DSTUtils.debug.error("DSTModifyResponse.toString: NameSpace is" +
              " not defined");
              return "";
           }
        }
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(tempPrefix).append("ModifyResponse");
        if(id != null && id.length() != 0) {
           sb.append(" id=\"").append(id).append("\"");
        }
        if(itemIDRef != null && itemIDRef.length() != 0) {
           sb.append(" itemIDRef=\"").append(itemIDRef).append("\"");
        }

        if(timeStamp != null) {
            sb.append(" timeStamp=\"")
                .append(DateUtils.toUTCDateFormat(timeStamp))
                .append("\"");
        }

        if(declareNS) {
           sb.append(" xmlns:").append(prefix).append("=\"")
             .append(nameSpaceURI).append("\"")
             .append(" xmlns=\"").append(nameSpaceURI).append("\"");
        }
        sb.append(">").append(status.toString())
          .append("</").append(tempPrefix).append("ModifyResponse").append(">");

        return sb.toString();
    }

}
