/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SMDescriptionData.java,v 1.2 2008/06/25 05:49:46 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.liberty.ws.disco.jaxb.DescriptionType;
import java.io.Serializable;
import java.util.List;
import javax.xml.namespace.QName;

/* - NEED NOT LOG - */

/**
 * <code>SMDescriptionData</code> defines a set of methods that
 * are required by discovery service description viewbean.
 */
public class SMDescriptionData implements Serializable {
    public String descriptionID = null;
    public String wsdlUri = null;
    public String nameSpace = null;
    public String localPart = null;
    public String endPointUrl = null;
    public String soapAction = null;
    public boolean isBriefSoapHttp = true;
    public List securityMechId = null;

    public SMDescriptionData() {
    }

    public SMDescriptionData(SMDescriptionData clone) {
	this.descriptionID = clone.descriptionID;
	this.wsdlUri = clone.wsdlUri;
	this.nameSpace = clone.nameSpace;
	this.localPart = clone.localPart;
	this.endPointUrl = clone.endPointUrl;
	this.soapAction = clone.soapAction;
	this.isBriefSoapHttp = clone.isBriefSoapHttp;
	this.securityMechId = clone.securityMechId;
    }

    /**
     * Returns first elements of security mechanism ID.
     *
     * @return first elements of security mechanism ID.
     */
    public String getFirstSecurityMechId() {
	return ((securityMechId != null) && !securityMechId.isEmpty())
	    ? (String)securityMechId.get(0) : null;
    }


    /**
     * Stores the description entry data.
     *
     * @param desc <code>DescriptionType</code> object.
     */
    public void setDescriptionEntry(DescriptionType desc) {
	descriptionID = desc.getId();
	securityMechId = desc.getSecurityMechID();
	String wsdlURI = desc.getWsdlURI();

	if ((wsdlURI != null) && (wsdlURI.length() > 0)) {
	    isBriefSoapHttp = false;
	    wsdlUri = desc.getWsdlURI();
	    QName q = desc.getServiceNameRef();
	    nameSpace = q.getNamespaceURI();
	    localPart = q.getLocalPart();
	} else {
	    isBriefSoapHttp = true;
	    endPointUrl = desc.getEndpoint();
	    soapAction = desc.getSoapAction();
	}
    }
}
