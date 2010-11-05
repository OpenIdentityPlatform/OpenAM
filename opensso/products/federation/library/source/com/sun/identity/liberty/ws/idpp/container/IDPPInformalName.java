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
 * $Id: IDPPInformalName.java,v 1.2 2008/06/25 05:47:16 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.idpp.container;

import com.sun.identity.shared.datastruct.CollectionHelper;
import javax.xml.bind.JAXBException;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.w3c.dom.Document;
import com.sun.identity.liberty.ws.idpp.common.*;
import com.sun.identity.liberty.ws.idpp.jaxb.*;
import com.sun.identity.liberty.ws.idpp.plugin.*;


/**
 * This class <code>IDPPInformalName</code> is an implementation of 
 * <code>IDPPContainer</code>.
 */ 

public class IDPPInformalName extends IDPPBaseContainer {

     // Constructor
     public IDPPInformalName() { 
        IDPPUtils.debug.message("IDPPInformalName:constructor:init.");
     }

     /**
      * Gets the Informal Name JAXB Object 
      * @param userMap user map
      * @return InformalNameElement JAXB Object.
      * @exception IDPPException.
      */
     public Object getContainerObject(Map userMap) throws IDPPException {
        IDPPUtils.debug.message("IDPPInformalName:getInformalName:Init");
        try {
            PPType ppType = IDPPUtils.getIDPPFactory().createPPElement();
            InformalNameElement in = 
                 IDPPUtils.getIDPPFactory().createInformalNameElement();

            String informalName = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.INFORMAL_NAME_ELEMENT).toLowerCase());
            in.setValue(informalName);

            ppType.setInformalName(in);
            return ppType;
        } catch (JAXBException je) {
            IDPPUtils.debug.error(
            "IDPPInformalName:getContainerObject: JAXB failure", je); 
            throw new IDPPException(
            IDPPUtils.bundle.getString("jaxbFailure"));
        }
     }

     /**
      * Gets required common name container attributes.
      * @return Set set of required container attributes 
      */
     public Set getContainerAttributes() {
        IDPPUtils.debug.message("IDPPInformalName:getContainerAttributes:Init");
        Set set = new HashSet();
        set.add(IDPPConstants.INFORMAL_NAME_ELEMENT);
        return getMapperAttributeSet(set);
     }

     /**
      * Gets the container attributes for a given select expression.
      * @param select Select expression.
      * @return Set set of required user attributes.
      */
     public Set getContainerAttributesForSelect(String select) {
         return getContainerAttributes();
     }

     /**
      * Processes modify container values and returns key value/pair to be
      * modified.
      * @param select Select Excepression.
      * @param data list of new data objects.
      * @return Attribute key value pair for the given select.
      * @throws IDPPException.
      */
     public Map getDataMapForSelect(String select, List data) 
     throws IDPPException {
        IDPPUtils.debug.message("IDPPInformalName:getDataMapForSelect:Init");
        Object dataElement = null;
        if(data != null && !data.isEmpty()) {
           dataElement = data.get(0);
        }
        if((dataElement == null) || 
           (dataElement instanceof InformalNameElement)) {
           Map map = new HashMap();
           map = getAttributeMap(
           IDPPConstants.INFORMAL_NAME_ELEMENT, dataElement, map);
           if(IDPPUtils.debug.messageEnabled()) {
              IDPPUtils.debug.message("IDPPInformalName:getDataMapForSelect:" +
              "Attr map to be modified." + map);
           }
           return map;
        } else {
           throw new IDPPException(
           IDPPUtils.bundle.getString("invalid Element"));
        }
     }

     /**
      * Checks if there are any binary attributes.
      */
     public boolean hasBinaryAttributes() {
         return false;
     }

}
