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
 * $Id: SMDiscoEntryData.java,v 1.2 2008/06/25 05:49:46 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.liberty.ws.disco.jaxb.AuthenticateRequesterElement;
import com.sun.identity.liberty.ws.disco.jaxb.AuthenticateSessionContextElement;
import com.sun.identity.liberty.ws.disco.jaxb.AuthorizeRequesterElement;
import com.sun.identity.liberty.ws.disco.jaxb.DescriptionType;
import com.sun.identity.liberty.ws.disco.jaxb.DirectiveType;
import com.sun.identity.liberty.ws.disco.jaxb.EncryptResourceIDElement;
import com.sun.identity.liberty.ws.disco.jaxb.OptionsType;
import com.sun.identity.liberty.ws.disco.jaxb.ResourceIDType;
import com.sun.identity.liberty.ws.disco.jaxb.ResourceOfferingType;
import com.sun.identity.liberty.ws.disco.jaxb.ServiceInstanceType;
import com.sun.identity.liberty.ws.disco.jaxb11.GenerateBearerTokenElement;
import com.sun.identity.liberty.ws.disco.jaxb11.SendSingleLogOutElement;
import com.sun.identity.liberty.ws.disco.plugins.jaxb.DiscoEntryElement;
import com.sun.identity.liberty.ws.soapbinding.Utils;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

/* - NEED NOT LOG - */

public class SMDiscoEntryData implements Serializable {
    private static com.sun.identity.liberty.ws.disco.jaxb.ObjectFactory
	discoFac = DiscoUtils.getDiscoFactory();
    private static com.sun.identity.liberty.ws.disco.jaxb11.ObjectFactory
	disco11Fac = DiscoUtils.getDisco11Factory();
    private static com.sun.identity.liberty.ws.disco.plugins.jaxb.ObjectFactory
	entryFac = DiscoUtils.getDiscoEntryFactory();

    public List descData = new ArrayList();
    public String entryId = "";
    public String resourceIdValue = "";
    public String resourceIdAttribute = "";
    public String serviceType = "";
    public String providerId = "";
    public String abstractValue = "";
    public List options = new ArrayList();
    public boolean newEntry = true;
    public boolean noOption = false;
    public String discoStr = "";
    public Map directives = null;
    public SMDescriptionData smDesc = null;

    public List getPossibleDirectivesToMechIDs() {
	List possibleChoices = new ArrayList(descData.size());
	for (Iterator iter = descData.iterator(); iter.hasNext(); ) {
	    SMDescriptionData smDesc = (SMDescriptionData)iter.next();
	    possibleChoices.add(smDesc.getFirstSecurityMechId());
	}
	return possibleChoices;
    }

    public Set getAllAssignedMechIDs() {
	Set allMechIDs = new HashSet();
	for (Iterator iter = descData.iterator(); iter.hasNext(); ) {
	    SMDescriptionData smDesc = (SMDescriptionData)iter.next();
	    allMechIDs.addAll(smDesc.securityMechId);
	}
	return allMechIDs;
    }

    public void setDiscoStr(boolean isUserView)
	throws AMConsoleException
    {
        try {
            ResourceOfferingType res = discoFac.createResourceOfferingType();
            ResourceIDType rid = discoFac.createResourceIDType();
            // if user resource offering then set resource id type.
            if (isUserView) {
                if ((resourceIdAttribute != null) &&
		    resourceIdAttribute.length() > 0
		) {
                    rid.setId(resourceIdAttribute);
                }
                rid.setValue(resourceIdValue);
                res.setEntryID(entryId);
            } else {
		/*
                 * jaxb api requires that we set resource id value to empty
                 * string if there is no value so that it will create empty tag
                 * for Resource ID.
                 */
		rid.setValue("");
	    }

            ServiceInstanceType svc = createServiceInstanceEntry();
            List descriptionTypeList = (List)svc.getDescription();
            res.setServiceInstance(svc);
            res.setResourceID(rid);

            if (abstractValue != null && abstractValue.length() > 0) {
                res.setAbstract(abstractValue);
            }
            if (!noOption) {
                res.setOptions(createOptionsEntry());
            }

            DiscoEntryElement de = entryFac.createDiscoEntryElement();
            de.setResourceOffering(res);
            createDirectivesEntry(de, descriptionTypeList);

            String str = convertDiscoEntryToXmlStr(de);
            if (str == null || str.length() == 0) {
                throw new AMConsoleException("discoEntryFailed.message");
            } else {
                discoStr = str;
            }
        } catch(JAXBException e) {
            Throwable t = e.getLinkedException();
            String str = (t != null) ? t.getMessage() : e.toString();
            throw new AMConsoleException(str);
        }
    }

    private String convertDiscoEntryToXmlStr(DiscoEntryElement de)
        throws JAXBException
    {
        JAXBContext jc = JAXBContext.newInstance(Utils.getJAXBPackages());
        Marshaller m = jc.createMarshaller();
        StringWriter sw = new StringWriter();
        m.marshal(de, sw);
        return sw.getBuffer().toString();
    }

    /**
     * Returns <code>ServiceInstanceType</code> object.
     *
     * @return <code>ServiceInstanceType</code> object.
     */
    private ServiceInstanceType createServiceInstanceEntry()
        throws JAXBException
    {
        ServiceInstanceType svc = discoFac.createServiceInstanceType();
        svc.setProviderID(providerId);
        svc.setServiceType(serviceType);
        List descriptionTypeList = (List)svc.getDescription();

	for (Iterator iter = descData.iterator(); iter.hasNext(); ) {
	    DescriptionType dType = createDescriptionEntry(
		(SMDescriptionData)iter.next());
	    descriptionTypeList.add(dType);
        }

        return svc;
    }

    private OptionsType createOptionsEntry()
        throws JAXBException
    {
        OptionsType optionsType = discoFac.createOptionsType();
        if ((options != null) && !options.isEmpty()) {
            optionsType.getOption().addAll(options);
        }
        return optionsType;
    }

    private void createDirectivesEntry(
	DiscoEntryElement de,
        List descriptionTypeList
    ) throws JAXBException, AMConsoleException {
        if ((directives != null) && !directives.isEmpty()) {
            Set set = directives.keySet();

	    for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                String dName = (String)iter.next();
                List idRefs = (List)directives.get(dName);

                if (dName.equals(DiscoConstants.AUTHN_DIRECTIVE)) {
                    AuthenticateRequesterElement authenticateRequester =
                        discoFac.createAuthenticateRequesterElement();
                    createDirectiveEntry(de, authenticateRequester, idRefs,
                        descriptionTypeList);
                } else if (dName.equals(DiscoConstants.ENCRYPT_DIRECTIVE)) {
                    EncryptResourceIDElement encryptResourceId =
                        discoFac.createEncryptResourceIDElement();
                    createDirectiveEntry(de, encryptResourceId, idRefs,
                        descriptionTypeList);
                } else if (dName.equals(DiscoConstants.SESSION_DIRECTIVE)) {
                    AuthenticateSessionContextElement authSessionCntx =
                        discoFac.createAuthenticateSessionContextElement();
                    createDirectiveEntry(de, authSessionCntx, idRefs,
                        descriptionTypeList);
                } else if (dName.equals(DiscoConstants.AUTHZ_DIRECTIVE)) {
                    AuthorizeRequesterElement authorizeRequester =
                        discoFac.createAuthorizeRequesterElement();
                    createDirectiveEntry(de, authorizeRequester, idRefs,
                        descriptionTypeList);
                } else if (dName.equals(DiscoConstants.BEARER_DIRECTIVE)) {
                    GenerateBearerTokenElement bearer =
                        disco11Fac.createGenerateBearerTokenElement();
                    createDirectiveEntry(de, bearer, idRefs,
                        descriptionTypeList);
                } else if (dName.equals(DiscoConstants.LOGOUT_DIRECTIVE)) {
                    SendSingleLogOutElement logout =
                        disco11Fac.createSendSingleLogOutElement();
                    createDirectiveEntry(de, logout, idRefs,
                        descriptionTypeList);
                }
	    }
	}
    }

    private void createDirectiveEntry(
        DiscoEntryElement de,
        DirectiveType dType,
        List idRefs,
        List descriptionTypeList
    ) throws AMConsoleException {
        if (idRefs != null && !idRefs.isEmpty()) {
	    for (Iterator iter = idRefs.iterator(); iter.hasNext(); ) {
                String idRef = (String)iter.next();
                DescriptionType desc = getDescriptionType(
		    idRef, descriptionTypeList);

                if (desc == null) {
                    throw new AMConsoleException("invalidDescIdRefs.message");
                }

                dType.getDescriptionIDRefs().add(desc);
            }
        }

        de.getAny().add(dType);
    }

    private DescriptionType getDescriptionType(String id, List list) {
        DescriptionType descType = null;
        boolean found = false;

	for (Iterator iter = list.iterator();
	    iter.hasNext() && (descType == null);
	) {
            DescriptionType desc = (DescriptionType)iter.next();

            if (desc != null) {
                List descTypeList = desc.getSecurityMechID();

                if (descTypeList.contains(id)) {
		    descType = desc;
                }
            }
        }

        return descType;
    }

    private DescriptionType createDescriptionEntry(SMDescriptionData smDesc)
        throws JAXBException
    {
        DescriptionType description = discoFac.createDescriptionType();
        description.setId(smDesc.descriptionID);
        List ids = smDesc.securityMechId;

        if ((ids != null) && !ids.isEmpty()) {
            description.getSecurityMechID().addAll(ids);
        }

        if (smDesc.isBriefSoapHttp) {
            String soapAction = smDesc.soapAction;
            if ((soapAction != null) && soapAction.trim().length() > 0) {
                description.setSoapAction(soapAction);
            }
            description.setEndpoint(smDesc.endPointUrl);
        } else {
            QName q = new QName(smDesc.nameSpace, smDesc.localPart);
            description.setServiceNameRef(q);
            description.setWsdlURI(smDesc.wsdlUri);
        }

        return description;
    }


    public String toString() {
	StringBuffer buff = new StringBuffer(1000);
	buff.append("descData=")
	    .append(descData.toString())
	    .append("\n")
	    .append("entryId=")
	    .append(entryId)
	    .append("\n")
	    .append("resourceIdValue=")
	    .append(resourceIdValue)
	    .append("\n")
	    .append("resourceIdAttribute=")
	    .append(resourceIdAttribute)
	    .append("\n")
	    .append("serviceType=")
	    .append(serviceType)
	    .append("\n")
	    .append("providerId=")
	    .append(providerId)
	    .append("\n")
	    .append("abstractValue=")
	    .append(abstractValue)
	    .append("\n")
	    .append("options=")
	    .append(options.toString())
	    .append("\n")
	    .append("newEntry=")
	    .append(newEntry)
	    .append("\n")
	    .append("noOption=")
	    .append(noOption)
	    .append("\n")
	    .append("discoStr=")
	    .append(discoStr)
	    .append("\n")
	    .append("directives=");

	if (directives != null) {
	    buff.append(directives.toString());
	} else {
	    buff.append("null");
	}

	buff.append("\n")
	    .append("smDesc=");

	if (smDesc != null) {
	    buff.append(smDesc.toString());
	} else {
	    buff.append("null");
	}
	buff.append("\n");
	return buff.toString();
    }
}
