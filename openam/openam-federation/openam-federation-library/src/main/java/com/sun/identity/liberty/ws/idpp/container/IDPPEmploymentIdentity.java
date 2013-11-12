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
 * $Id: IDPPEmploymentIdentity.java,v 1.2 2008/06/25 05:47:16 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.idpp.container;

import com.sun.identity.shared.datastruct.CollectionHelper;
import javax.xml.bind.JAXBException;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.w3c.dom.Document;
import com.sun.identity.liberty.ws.idpp.common.*;
import com.sun.identity.liberty.ws.idpp.jaxb.*;
import com.sun.identity.liberty.ws.idpp.plugin.*;
import com.sun.identity.liberty.ws.idpp.IDPPServiceManager;


/**
 * This class <code>IDPPEmploymentIdentity</code> is an implementation of 
 * <code>IDPPContainer</code>.
 */ 

public class IDPPEmploymentIdentity extends IDPPBaseContainer {

     // Constructor
     public IDPPEmploymentIdentity() { 
         IDPPUtils.debug.message("IDPPEmploymentIdentity:constructor:init.");
     }

     /**
      * Gets the employment identity jaxb object 
      * @param userMap user map
      * @return EmploymentIdentityElement JAXB Object.
      * @exception IDPPException.
      */
     public Object getContainerObject(Map userMap) throws IDPPException {
         IDPPUtils.debug.message("IDPPEmploymentIdentity:getContainerObj:Init");
         try {
             PPType ppType = IDPPUtils.getIDPPFactory().createPPElement();
             EmploymentIdentityElement ei = 
                 IDPPUtils.getIDPPFactory().createEmploymentIdentityElement();
             String jobTitle = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.JOB_TITLE_ELEMENT).toLowerCase());
             if(jobTitle != null) {
                DSTString dstString = getDSTString(jobTitle);
                ei.setJobTitle(dstString);
             }
 
             String org = CollectionHelper.getMapAttr(userMap, 
                getAttributeMapper().getDSAttribute(
                    IDPPConstants.O_ELEMENT).toLowerCase());
             if(org != null) {
                DSTString dstString = getDSTString(org);
                ei.setO(dstString);
             }

             Set altOs = (Set)userMap.get(
             getAttributeMapper().getDSAttribute(
             IDPPConstants.ALT_O_ELEMENT).toLowerCase());
             Iterator iter = altOs.iterator();
             while(iter.hasNext()) {
                DSTString dstString = getDSTString((String)iter.next());
                ei.getAltO().add(dstString);
             }
             ppType.setEmploymentIdentity(ei);
             return ppType;
         } catch (JAXBException je) {
             IDPPUtils.debug.error(
              "IDPPContainers:getContainerObject: JAXB failure", je); 
              throw new IDPPException(
              IDPPUtils.bundle.getString("jaxbFailure"));
         }
     }

     /**
      * Gets required employment identity container attributes.
      * @return Set set of required container attributes 
      */
     public Set getContainerAttributes() {
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPEmploymentIdentity:getContainer" +
           "Attributes:Init");
        }
        Set set = new HashSet();
        set.add(IDPPConstants.JOB_TITLE_ELEMENT);
        set.add(IDPPConstants.O_ELEMENT);
        set.add(IDPPConstants.ALT_O_ELEMENT);
        return getMapperAttributeSet(set);
     }

     /**
      * Gets the container attributes for a given select expression.
      * @param select  Select expression.
      * @return Set set of required user attributes.
      */
     public Set getContainerAttributesForSelect(String select) {
        String expContext = IDPPUtils.getExpressionContext(select);
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPEmploymentIdentity:getContainer" +
           "AttribSelect:exp context = " + expContext);
        }
        Set set = new HashSet();
        if(expContext != null) {
           set.add(expContext);
        }
        return getMapperAttributeSet(set);
     }

     /**
      * Process modify container values and returns key value/pair to be
      * modified.
      * @param select Select expression.
      * @param data list of new data objects.
      * @return Attribute key value pair for the given select.
      * @exception IDPPException.
      */
     public Map getDataMapForSelect(String select, List data) 
     throws IDPPException {
        IDPPUtils.debug.message("IDPPEmploymentIdentity:getDataMapForSelect:");
        Map map = new HashMap();
        String expContext = IDPPUtils.getExpressionContext(select);
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPEmploymentIdentity:getDataMapForSelect:"
           + "exp context = " + expContext);
        }
        int attrType = IDPPUtils.getIDPPElementType(expContext);
        Object dataElement = null;
        if(data != null && !data.isEmpty()) {
           dataElement = data.get(0);
        }
        switch(attrType) {
               case IDPPConstants.JOB_TITLE_ELEMENT_INT:
                   if((dataElement == null) || 
                      (dataElement instanceof JobTitleElement)) {
                      map = getAttributeMap(expContext, dataElement, map);
                      break;
                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
               case IDPPConstants.O_ELEMENT_INT:
                   if((dataElement == null) || 
                      (dataElement instanceof OElement)) {
                      map = getAttributeMap(expContext, dataElement, map);
                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
                   break;
               case IDPPConstants.ALT_O_ELEMENT_INT:
                   map = getAltOMap(data, map);
                   break;
               case IDPPConstants.EMPLOYMENT_IDENTITY_ELEMENT_INT:
                   map = getEmploymentIdentityMap(dataElement, map);
                   break;
               default:
                   IDPPUtils.debug.error("IDPPEmploymentIdentity:getDataMap" +
                   "ForSelect. Unsupported element");
                   break;
          }
          if(IDPPUtils.debug.messageEnabled()) {
             IDPPUtils.debug.message("IDPPEmploymentIdentity:getDataMapFor" + 
             "Select:Attr map to be modified." + map);
          }
          return map;
     }

     /**
      * Gets EmploymentIdentity attributes in a hashmap.
      * @param obj EmploymentIdentityType JAXB object.
      * @param map map that sets attribute/value pairs.
      * @return required employment identity hashmap.
      */
     private Map getEmploymentIdentityMap(Object obj, Map map) 
     throws IDPPException {
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPEmploymentIdentity:getEmployment" +
           "IdentityMap:Init");
        }
        DSTString jobTitle = null, org = null;
        List altO = null;
        if(obj != null) {
           if(obj instanceof EmploymentIdentityType) {
              EmploymentIdentityType eiType = (EmploymentIdentityType)obj;
              jobTitle = eiType.getJobTitle();
              org = eiType.getO();
              altO = eiType.getAltO();
           } else {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }
        }
        getAltOMap(altO, map);
        getAttributeMap(IDPPConstants.JOB_TITLE_ELEMENT, jobTitle, map);
        getAttributeMap(IDPPConstants.O_ELEMENT, org, map);
        return map;
     }

     /**
      * Get the Alt Org attributes map
      * @param dataObject list of data objects
      * @param map map to be filled in
      * @return Map map to be returned
      * @exception IDPPException
      */
     private Map getAltOMap(List dataObject, Map map) throws IDPPException {
        IDPPUtils.debug.message("IDPPEmploymentIdentity:getAltOMap:Init");
        if(dataObject == null) {
           map = getAttributeMap(IDPPConstants.ALT_O_ELEMENT, null, map);
           return map;
        }
        return getAttributeMap(IDPPConstants.ALT_O_ELEMENT, dataObject, map);
     }

     /**
      * Checks if there are binary attributes
      */
     public boolean hasBinaryAttributes() {
         return false;
     }

}
