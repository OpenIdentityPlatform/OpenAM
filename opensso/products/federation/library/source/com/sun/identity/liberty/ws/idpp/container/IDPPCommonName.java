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
 * $Id: IDPPCommonName.java,v 1.2 2008/06/25 05:47:15 qcheng Exp $
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
 * This class <code>IDPPCommonName</code> is an implementation of 
 * <code>IDPPContainer</code>.
 */ 

public class IDPPCommonName extends IDPPBaseContainer {

     /**
      * Constructor
      */
     public IDPPCommonName() { 
         IDPPUtils.debug.message("IDPPCommonName:constructor:init.");
     }

     /**
      * Gets the common name jaxb element 
      * @param userMap user map
      * @return CommonNameElement JAXB Object.
      * @exception IDPPException.
      */
     public Object getContainerObject(Map userMap) throws IDPPException {

         IDPPUtils.debug.message("IDPPContainers:getContainerObject:Init");
         try {
             PPType ppType = IDPPUtils.getIDPPFactory().createPPElement();
             CommonNameElement ce = 
                   IDPPUtils.getIDPPFactory().createCommonNameElement();

             String cn = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.CN_ELEMENT).toLowerCase());

             if(cn != null) {
                DSTString dstString = getDSTString(cn);
                ce.setCN(dstString);
             }

             Set altCNs = (Set)userMap.get(getAttributeMapper().getDSAttribute(
                 IDPPConstants.ALT_CN_ELEMENT).toLowerCase());
             if ((altCNs != null) && (altCNs.size() > 0)) {
                 Iterator iter = altCNs.iterator();
                 while(iter.hasNext()) {
                     DSTString dstString = getDSTString((String)iter.next());
                     ce.getAltCN().add(dstString);
                 }
             }

             AnalyzedNameType analyzedName = getAnalyzedName(userMap); 
             ce.setAnalyzedName(analyzedName);

             ppType.setCommonName(ce);
             return ppType;
         } catch (JAXBException je) {
             IDPPUtils.debug.error(
              "IDPPContainers:getContainerObject: JAXB failure", je); 
              throw new IDPPException(
              IDPPUtils.bundle.getString("jaxbFailure"));
         } catch (IDPPException ie) {
              IDPPUtils.debug.error("IDPPContainers:getContainerObject:" +
              "Error while creating common name.", ie);
              throw new IDPPException(ie);
         }
     }

     /**
      * Gets required common name container attributes.
      * @return Set set of required container attributes 
      */
     public Set getContainerAttributes() {
         IDPPUtils.debug.message("IDPPCommonName:getContainerAttributes:Init");
         Set set = new HashSet();
         set.add(IDPPConstants.FN_ELEMENT);
         set.add(IDPPConstants.SN_ELEMENT);
         set.add(IDPPConstants.PT_ELEMENT);
         String nameScheme = IDPPServiceManager.getInstance().getNameScheme();
         if(nameScheme != null &&
            nameScheme.equals(IDPPConstants.NAME_SCHEME_MIDDLE)) {
            set.add(IDPPConstants.MN_ELEMENT);
         }
         set.add(IDPPConstants.CN_ELEMENT);
         set.add(IDPPConstants.ALT_CN_ELEMENT);
         return getMapperAttributeSet(set);
     }

     /**
      * Gets the container attributes for a given select expression.
      * @param select Select expression.
      * @return Set set of required user attributes.
      */
     public Set getContainerAttributesForSelect(String select) {
        IDPPUtils.debug.message("IDPPCommonName:getContainerAttribSelect:Init");
        String expContext = IDPPUtils.getExpressionContext(select);
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPCommonName:getContainerAttribSelect:" +
           "exp context = " + expContext);
        }
        int attrType = IDPPUtils.getIDPPElementType(expContext);
        Set set = new HashSet();
        switch(attrType) {
                 case IDPPConstants.SN_ELEMENT_INT:
                 case IDPPConstants.FN_ELEMENT_INT:
                 case IDPPConstants.MN_ELEMENT_INT:
                 case IDPPConstants.PT_ELEMENT_INT:
                 case IDPPConstants.CN_ELEMENT_INT:
                 case IDPPConstants.ALT_CN_ELEMENT_INT:
                      set.add(expContext);
                      break;
                 case IDPPConstants.ANALYZED_NAME_ELEMENT_INT:
                      return getAnalyzedNameAttributes();
                 case IDPPConstants.COMMON_NAME_ELEMENT_INT:
                      return getContainerAttributes();
                 default:
                      IDPPUtils.debug.error("IDPPCommonName:getContainer"+
                      "AttributesForSelect. Invalid select.");
         }
         return getMapperAttributeSet(set);
     }

     /**
      * Processes modify container values and returns key value/pair to be
      * modified.
      * @param select Select expression.
      * @param data list of new data objects.
      * @return Attribute key value pair for the given select.
      * @throws IDPPException.
      */
     public Map getDataMapForSelect(String select, List data) 
     throws IDPPException {
        IDPPUtils.debug.message("IDPPCommonName:getDataMapForSelect:Init");
        Map map = new HashMap();
        String expContext = IDPPUtils.getExpressionContext(select);
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPCommonName:getDataMapForSelect:" +
           "exp context = " + expContext);
        }
        int attrType = IDPPUtils.getIDPPElementType(expContext);
        Object dataElement = null;
        if(data != null && !data.isEmpty()) {
           dataElement = data.get(0);
        }
        switch(attrType) {
               case IDPPConstants.SN_ELEMENT_INT:
                   if((dataElement == null) || 
                      (dataElement instanceof SNElement)) {
                      map = getAttributeMap(expContext, dataElement, map);
                      break;
                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
               case IDPPConstants.FN_ELEMENT_INT:
                   if((dataElement == null) || 
                      (dataElement instanceof FNElement)) {
                      map = getAttributeMap(expContext, dataElement, map);
                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
                   break;
               case IDPPConstants.MN_ELEMENT_INT:
                   if((dataElement == null) || 
                      (dataElement instanceof MNElement)) {
                      map = getAttributeMap(expContext, dataElement, map);
                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
                   break;
               case IDPPConstants.PT_ELEMENT_INT:
                   if((dataElement == null) || 
                      (dataElement instanceof PersonalTitleElement)) {
                      map = getAttributeMap(expContext, dataElement, map);
                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
                   break;
               case IDPPConstants.CN_ELEMENT_INT:
                   if((dataElement == null) ||
                      (dataElement instanceof CNElement)) {
                      map = getAttributeMap(expContext, dataElement, map);
                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
                   break;
               case IDPPConstants.ALT_CN_ELEMENT_INT:
                   map = getAltCNMap(data, map);
                   break;
               case IDPPConstants.ANALYZED_NAME_ELEMENT_INT:
                   map = getAnalyzedNameMap(dataElement, map);
                   break;
               case IDPPConstants.COMMON_NAME_ELEMENT_INT:
                   map = getCommonNameMap(dataElement, map);
                   break;
               default:
                   IDPPUtils.debug.error("IDPPCommonName:getDataMapForSelect"
                   + "Unsupported element");
                   break;
          }
          if(IDPPUtils.debug.messageEnabled()) {
             IDPPUtils.debug.message("IDPPCommonName:getDataMapForSelect:" +
             "Attr map to be modified." + map);
          }
          return map;
     }

     /**
      * Gets CommonName attributes in a hashmap.
      * @param obj CommonNameType JAXB object.
      * @param map map that sets attribute/value pairs.
      * @return required common name hashmap.
      */
     private Map getCommonNameMap(Object obj, Map map) 
     throws IDPPException {
        IDPPUtils.debug.message("IDPPCommonName:getCommonNameMap:Init");
        AnalyzedNameType analyzedName = null;
        DSTString cn = null;
        List altCNs = null;
        if(obj != null) {
           if(obj instanceof CommonNameType) {
              CommonNameType cnType = (CommonNameType)obj;
              analyzedName = cnType.getAnalyzedName();
              cn = cnType.getCN();
              altCNs = cnType.getAltCN();
           } else {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }
        }
        getAnalyzedNameMap(analyzedName, map);
        getAltCNMap(altCNs, map);
        getAttributeMap(IDPPConstants.CN_ELEMENT, cn, map);
        return map;
     }

     /**
      * Get the AltCN attributes map
      * @param dataObject list of data objects
      * @param map map to be filled in
      * @return Map map to be returned
      * @exception IDPPException
      */
     private Map getAltCNMap(List dataObject, Map map) throws IDPPException {
        IDPPUtils.debug.message("IDPPCommonName:getAltCNMap:Init");
        if(dataObject == null) {
           map = getAttributeMap(IDPPConstants.ALT_CN_ELEMENT, null, map);
           return map;
        }
        return getAttributeMap(IDPPConstants.ALT_CN_ELEMENT, dataObject, map);
     }

     /**
      * Checks if the container has any binary attributes.
      */
     public boolean hasBinaryAttributes() {
         return false;
     }

}
