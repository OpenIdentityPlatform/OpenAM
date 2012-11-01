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
 * $Id: SMDiscoveryServiceData.java,v 1.3 2008/06/25 05:49:46 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.jaxb.AuthorizeRequesterElement;
import com.sun.identity.liberty.ws.disco.jaxb.AuthenticateRequesterElement;
import com.sun.identity.liberty.ws.disco.jaxb.AuthenticateSessionContextElement;
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
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.xml.sax.InputSource;

/* - NEED NOT LOG - */

/**
 * <code>SMDiscoveryServiceData</code> defines a set of methods that
 * are required by discovery service.
 */
public class SMDiscoveryServiceData implements Serializable
{
    public static Debug debug = Debug.getInstance("amConsole");

    public static final int GLOBAL_ENTRY = 0;
    public static final int DYNAMIC_ENTRY = 1;
    public static final int USER_RESOURCE_OFFERING_ENTRY = 2;

    private List discoData = new ArrayList();
    private Map attr = null;
    private Map dynAttr = null;
    private SMDiscoEntryData smDisco = null;
    private int type = GLOBAL_ENTRY;
    private String priority = null;
    private boolean resourceOfferingModified = false;

    /**
     * Returns the handler to the <code>SMDiscoEntryData</code> instance stored
     * in this object.
     *
     * @return handler to the <code>SMDiscoEntryData</code> instance.
     */
    public SMDiscoEntryData getSMDiscoEntryDataHandler() {
        return smDisco;
    }

    /**
     * Sets the resource offering entry object to the value provided.
     *
     * @param data <code>SMDiscoEntryData</code>
     */
    public void setSMDiscoEntryDataHandler(SMDiscoEntryData data) {
        smDisco = data;
    }

    /**
     * Returns true if there is an entry for resource offering.
     *
     * @return true if there is an entry for resource offering.
     */
    public boolean hasResources() {
        return (discoData != null) && !discoData.isEmpty();
    }

    /**
     * Adds the resource offering entry to the list object.
     *
     * @param data <code>SMDiscoEntryData</code>
     */
    public void addResourceData(SMDiscoEntryData data) {
	try {
	    data.setDiscoStr(isUserResourceOffering());
	    discoData.add(data);
	} catch (AMConsoleException e) {
	    debug.error("SMDiscoveryServiceData.addResourceData", e);
	}
    }

    /**
     * Replaces the resource offering entry to the list object.
     *
     * @param idx Index of element in resource offering list.
     * @param data <code>SMDiscoEntryData</code>
     */
    public void replaceResourceData(int idx, SMDiscoEntryData data)
	throws AMConsoleException {
	data.setDiscoStr(isUserResourceOffering());
	SMDiscoEntryData old = (SMDiscoEntryData)discoData.set(idx, data);
    }

    /**
     * Returns the list of resource offering.
     *
     * @return the list of resource offering.
     */
    public List getResourceData() {
        return discoData;
    }
 
    /**
     * Stores the global attributes for discovery services in this object.
     *
     * @param map global attributes and values.
     */
    public void setAttr(Map map) {
        attr = map;
    }

    /**
     * Stores the dynamic attributes for discovery services in this object.
     *
     * @param map dynamic attributes and values.
     */
    public void setDynamicAttr(Map map) {
        dynAttr = map;
    }

    /**
     * Returns the global attributes for discovery service stored in 
     * this object.
     *
     * @return the map of global attributes.
     */
    public Map getAttr() {
        return attr;
    }

    /**
     * Returns the dynamic attributes for discovery service stored in 
     * this object.
     *
     * @return the map of dynamic attributes.
     */
    public Map getDynamicAttr() {
        return dynAttr;
    }

    /**
     * Returns true if the flag for global entry is set.
     *
     * @return true if the flag for global entry is set.
     */
    public boolean isUserResourceOffering() {
        return type == USER_RESOURCE_OFFERING_ENTRY;
    }

    /**
     * Returns true if the flag for global entry is set.
     *
     * @return true if the flag for global entry is set.
     */
    public int getEntryType() {
        return type;
    }

    /**
     * Sets the global entry flag to the value provided. True value indicate 
     * that it is a global entry.
     *
     * @param value value to indicate global entry or not.
     */
    public void setEntryType(int value) {
        type = value;
    }

    /**
     * Sets the priority to a new value in this object.
     *
     * @param value priority value.
     */
    public void setPriority(String value) {
        priority = value;
    }

    /**
     * Returns priority stored in this object.
     *
     * @return priority stored in this object.
     */
    public String getPriority() {
        return priority;
    }

    /**
     * Returns true if a resource offering entries has modified.
     *
     * @return true if a resource offering entries has modified.
     */
    public boolean isResourceOfferingModified() {
        return resourceOfferingModified;
    }

    /**
     * Sets the resource offering entry flag to a new value in this object.
     *
     * @param value resource offering entry flag value.
     */
    public void setResourceOfferingEntryFlag(boolean value) {
        resourceOfferingModified = value;
    }

    /**
     * Returns a set resource offering entries.
     *
     * @return resource offering entries in <code>entry</code>.
     */
    public Set getDiscoveryEntries() {
        OrderedSet discoEntrySet = new OrderedSet();

        if ((discoData != null) && !discoData.isEmpty()) {
            for (Iterator iter = discoData.iterator(); iter.hasNext(); ) {
                SMDiscoEntryData data = (SMDiscoEntryData)iter.next();
                discoEntrySet.add(data.discoStr);
            }
        }

        return discoEntrySet;
    }

    /**
     * Deletes the resource offering entries.
     *
     * @param array Array of entries to be deleted.
     */
    public void deleteDiscoEntries(Integer[] array) {
	for (int i = (array.length -1); i >= 0; --i) {
	    discoData.remove(array[i].intValue());
	}
    }

    /**
     * Returns resource offering entry stored in the Directory Server.
     *
     * @param set Set of entry data.
     * @return resource offering entry stored in the Directory Server.
     */
    public static SMDiscoveryServiceData getEntries(Set set)
	throws AMConsoleException {
        return getEntries(set, GLOBAL_ENTRY);
    }

    /**
     * Returns resource offering entry stored in the Directory Server.
     *
     * @param set Set of entry data.
     * @param type Type of offering entry, global, dynamic or user.
     * @return resource offering entry stored in the Directory Server.
     */
    public static SMDiscoveryServiceData getEntries(Set set, int type)
	throws AMConsoleException {
        SMDiscoveryServiceData smEntry = new SMDiscoveryServiceData();
        smEntry.setEntryType(type);

        if (set != null && !set.isEmpty()) {
            try {
                JAXBContext jc =
                    JAXBContext.newInstance(Utils.getJAXBPackages());
                Unmarshaller u = jc.createUnmarshaller();

		for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                    String str = (String)iter.next();
                    SMDiscoEntryData smDisco = new SMDiscoEntryData();
                    smDisco.discoStr = str;
                    DiscoEntryElement entry = (DiscoEntryElement)u.unmarshal(
                            XMLUtils.createSAXSource(new InputSource(new StringReader(str))));
                    setDiscoEntryData(entry, smDisco);
                    smEntry.addResourceData(smDisco);
                }
            } catch (JAXBException je) {
                debug.error("SMDiscoveryServiceData.getEntries", je);
            }
        }

        return smEntry;
    }

    private static void setDiscoEntryData(
        DiscoEntryElement entry,
        SMDiscoEntryData smDisco)
    {
        ResourceOfferingType resOff = entry.getResourceOffering();
        ResourceIDType resourceIdType = resOff.getResourceID();
        ServiceInstanceType serviceInstance = resOff.getServiceInstance();
        String providerID = serviceInstance.getProviderID();
        String serviceType = serviceInstance.getServiceType();

        smDisco.entryId = resOff.getEntryID();
        smDisco.resourceIdAttribute = resourceIdType.getId();
        smDisco.resourceIdValue = resourceIdType.getValue();
        smDisco.serviceType = serviceType;
        smDisco.providerId = providerID;
        smDisco.abstractValue = resOff.getAbstract();
        OptionsType optType = resOff.getOptions();

        if (optType != null) {
            smDisco.options = optType.getOption();
            smDisco.noOption = false;
        } else {
            smDisco.noOption = true;
        }

        List list = serviceInstance.getDescription();

        if (list != null && !list.isEmpty()) {
            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                SMDescriptionData smDesc = new SMDescriptionData();
                DescriptionType desc = (DescriptionType)iter.next();
                smDesc.setDescriptionEntry(desc);
                smDisco.descData.add(smDesc);
            }
        } else {
            debug.error("SMDiscoveryServiceData.setDiscoEntryData: " +
                "No description exists in the disco entry");
        }

        smDisco.directives = getDirectiveEntry(entry);
    }

    /**
     * Returns the value of abstract type.
     *
     * @param discoStr resource offering entry string.
     * @return the value of abstract type in the resource offering entry.
     */
    public String getAbstractValue(String discoStr) {
        String abstractValue = null;

        if ((discoStr != null) && (discoStr.length() > 0)) {
            if ((discoData != null) && !discoData.isEmpty()) {
                int size = discoData.size();

		for (Iterator iter = discoData.iterator(); 
		    iter.hasNext() && (abstractValue == null);
		) {
                    SMDiscoEntryData smDisco = (SMDiscoEntryData)iter.next();
                    if (discoStr.equals(smDisco.discoStr)) {
                        abstractValue = smDisco.abstractValue;
                    }
                }
            }
        }

        return (abstractValue == null) ? "" : abstractValue;
    }

    /**
     * Returns map of directive entries.
     *
     * @return map of directive entries.
     */
    public static  Map getDirectiveEntry(DiscoEntryElement entry) {
        Map map = Collections.EMPTY_MAP;
        List directiveList = entry.getAny();

        if ((directiveList != null) && !directiveList.isEmpty()) {
            map = new HashMap(directiveList.size() *2);

            for (Iterator iter = directiveList.iterator(); iter.hasNext(); ) {
                Object obj = iter.next();;

                if (obj instanceof AuthenticateRequesterElement) {
                     AuthenticateRequesterElement dType =
                         (AuthenticateRequesterElement)obj;
                     setDirectiveData(dType, map,
                         DiscoConstants.AUTHN_DIRECTIVE);
                 } else if (obj instanceof EncryptResourceIDElement) {
                     EncryptResourceIDElement dType =
                         (EncryptResourceIDElement)obj;
                     setDirectiveData(dType, map,
                         DiscoConstants.ENCRYPT_DIRECTIVE);
                 } else if (obj instanceof AuthenticateSessionContextElement) {
                     AuthenticateSessionContextElement dType =
                         (AuthenticateSessionContextElement)obj;
                     setDirectiveData(dType, map,
                         DiscoConstants.SESSION_DIRECTIVE);
                 } else if (obj instanceof AuthorizeRequesterElement) {
                     AuthorizeRequesterElement dType =
                         (AuthorizeRequesterElement)obj;
                     setDirectiveData(dType, map,
                         DiscoConstants.AUTHZ_DIRECTIVE);
                 } else if (obj instanceof GenerateBearerTokenElement) {
                     GenerateBearerTokenElement dType =
                         (GenerateBearerTokenElement)obj;
                     setDirectiveData(dType, map,
                         DiscoConstants.BEARER_DIRECTIVE);
                 } else if (obj instanceof SendSingleLogOutElement) {
                     SendSingleLogOutElement dType =
                         (SendSingleLogOutElement)obj;
                     setDirectiveData(dType, map,
                         DiscoConstants.LOGOUT_DIRECTIVE);
                 } else {
                     debug.error("unsupported directive type");
                }
            }
        }

        return map;
    }

    private static void setDirectiveData(
        DirectiveType dType,
        Map map,
        String directiveName)
    {
        List idRefsList = dType.getDescriptionIDRefs();
        List idRefsSelected = Collections.EMPTY_LIST;

        if ((idRefsList != null) && !idRefsList.isEmpty()) {
            idRefsSelected = new ArrayList(idRefsList.size());

            for (Iterator iter = idRefsList.iterator(); iter.hasNext(); ) {
                DescriptionType descType = (DescriptionType)iter.next();
                List list = descType.getSecurityMechID();
                idRefsSelected.add(list.get(0));
            }
        }

        map.put(directiveName, idRefsSelected);
    }
}
