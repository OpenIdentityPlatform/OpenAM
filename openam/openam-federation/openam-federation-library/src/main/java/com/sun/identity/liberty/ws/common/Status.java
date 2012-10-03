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
 * $Id: Status.java,v 1.2 2008/06/25 05:47:09 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.common;

import javax.xml.namespace.QName;

/**
 * This class represents a common status object.
 * The following schema fragment specifies the expected content within the  
 * <code>Status</code> object.
 * <pre>
 * &lt;complexType name="Status">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="Status" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="code" use="required" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="comment" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ref" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @supported.all.api
 */
public class Status {

    private Status status;  
    private String ref;
    private QName code;
    private String comment;
    private String ns = null;
    private String nsPrefix = null;

    /**
     * Default constructor.
     */
    public Status() {}

    /**
     * Constructor.
     *
     * @param ns Name space for the Status object   
     * @param nsPrefix prefix used for the name space, for example,
     *        <code>disco</code>.
     */
    public Status(String ns, String nsPrefix) {
        this.ns = ns;
        this.nsPrefix = nsPrefix;
    }

    /**
     * Gets sub status.
     * @return Status
     * @see #setSubStatus(Status)
     */
    public Status getSubStatus() {
        return status;
    }

    /**
     * Sets sub status. The sub status is used by a service to convey 
     * second-level status information in addition to the status code. 
     * @param status Status to be set
     * @see #getSubStatus()
     */
    public void setSubStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets reference attribute.
     *
     * @return reference attribute. 
     * @see #setRef(String)
     */
    public String getRef() {
        return ref;
    }

    /**
     * Sets reference attribute.
     * @param value reference to be set 
     * @see #getRef()
     */
    public void setRef(String value) {
        this.ref = value;
    }

    /**
     * Gets status code.
     *
     * @return status code.
     * @see #setCode(QName)
     */
    public QName getCode() {
        return code;
    }

    /**
     * Sets status code.
     * @param value status code to be set
     * @see #getCode()
     */
    public void  setCode(QName value) {
        this.code = value;
    }

    /**
     * Gets comment for the status.
     *
     * @return comment for the status.
     * @see #setComment(String)
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets comment 
     * @param comment String 
     * @see #getComment()
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns string format of the status.
     * @return String
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(500);
        sb.append("<");
        if ((nsPrefix != null) && nsPrefix.length() != 0) {
            sb.append(nsPrefix).append(":Status xmlns:").append(nsPrefix).
                append("=\"");
        } else {
            sb.append("Status xmlns=\"");
        }
        // ns must be present
        sb.append(ns).append("\"");

        if (code != null) {
            String localPart = code.getLocalPart();
            if ((localPart != null) && localPart.length() != 0) {
                sb.append(" code=\"");
                String codeNS = code.getNamespaceURI();
                if ((codeNS == null) || codeNS.length() == 0) {
                    sb.append(localPart).append("\"");
                } else {
                    if ((ns != null) && ns.equals(codeNS)) {
                        if ((nsPrefix != null) && nsPrefix.length() != 0) {
                            sb.append(nsPrefix).append(":");
                        }
                         sb.append(localPart).append("\"");
                    } else {
                        String codePrefix = code.getPrefix();
                        if ((codePrefix != null) && codePrefix.length() != 0) {
                            sb.append(codePrefix).append(":").
                                append(localPart).append("\" xmlns:").
                                append(codePrefix).append("=\"").
                                append(codeNS).append("\"");
                        } else {
                            if ((nsPrefix == null) || nsPrefix.length() == 0) {
                                sb.append("ns1:").append(localPart).
                                    append("\" xmlns:").append("ns1=\"").
                                    append(codeNS).append("\"");
                            } else {
                                sb.append(localPart).append("\" xmlns=\"").
                                    append(codeNS).append("\"");
                            }
                        }
                    }
                }
            }
        }

        if ((ref != null) && ref.length() != 0) {
            sb.append(" ref=\"").append(ref).append("\"");
        }

        if ((comment != null) && comment.length() != 0) {
            sb.append(" comment=\"").append(comment).append("\"");
        }
        sb.append(">");
        if (status != null) {
            status.toString();
        }
        if ((nsPrefix != null) && nsPrefix.length() != 0) {
            sb.append("</").append(nsPrefix).append(":Status>");
        } else {
            sb.append("</Status>");
        }
        return sb.toString();
    }
}
