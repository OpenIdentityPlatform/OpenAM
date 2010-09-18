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
 * $Id: IDPPLegalIdentity.java,v 1.2 2008/06/25 05:47:16 qcheng Exp $
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
import java.util.ArrayList;
import org.w3c.dom.Document;
import com.sun.identity.liberty.ws.idpp.common.*;
import com.sun.identity.liberty.ws.idpp.jaxb.*;
import com.sun.identity.liberty.ws.idpp.plugin.*;
import com.sun.identity.liberty.ws.idpp.IDPPServiceManager;


/**
 * This class <code>IDPPLegalIdentity</code> is an implementation of 
 * <code>IDPPContainer</code>.
 */ 

public class IDPPLegalIdentity extends IDPPBaseContainer {

     // Constructor
     public IDPPLegalIdentity() { 
         IDPPUtils.debug.message("IDPPLegalIdentity:constructor:init.");
     }

     /**
      * Gets the container object i.e. LegalIdentity JAXB Object 
      * @param userMap user map
      * @return LegalIdentityElement JAXB Object.
      * @exception IDPPException.
      */
     public Object getContainerObject(Map userMap) throws IDPPException {

         IDPPUtils.debug.message("IDPPLegalIdentity:getContainerObject:Init");
         try {
             PPType ppType = IDPPUtils.getIDPPFactory().createPPElement();
             LegalIdentityElement lIdentity = 
                  IDPPUtils.getIDPPFactory().createLegalIdentityElement();
             String value = CollectionHelper.getMapAttr(userMap, 
             getAttributeMapper().getDSAttribute(
             IDPPConstants.LEGAL_NAME_ELEMENT).toLowerCase());

             if(value != null) {
                DSTString dstString = getDSTString(value);
                lIdentity.setLegalName(dstString);
             }
             value = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
             IDPPConstants.DOB_ELEMENT).toLowerCase());
             if(value != null) {
                DSTDate date = getDSTDate(value);
                lIdentity.setDOB(date);
             }

             value = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
             IDPPConstants.GENDER_ELEMENT).toLowerCase());
             if(value != null) {
                DSTURI gender = getDSTURI(value);
                lIdentity.setGender(gender);
             }
             value = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
             IDPPConstants.MARITAL_STATUS_ELEMENT).toLowerCase());
             if(value != null) {
                DSTURI mStatus = getDSTURI(value);
                lIdentity.setMaritalStatus(mStatus);
             }

             AltIDType altID = getAltID(userMap);
             if(altID != null) {
                List list = new ArrayList(); 
                list.add(altID);
                lIdentity.getAltID().addAll(list);
             }

             VATType vType = getVAT(userMap);
             if(vType != null) {
                lIdentity.setVAT(vType);
             }

             AnalyzedNameType analyzedName = getAnalyzedName(userMap); 
             if(analyzedName != null) {
                lIdentity.setAnalyzedName(analyzedName);
             } 
             ppType.setLegalIdentity(lIdentity);
             return ppType;
         } catch (JAXBException je) {
             IDPPUtils.debug.error(
              "IDPPContainers:getContainerObject: JAXB failure", je); 
              throw new IDPPException(
              IDPPUtils.bundle.getString("jaxbFailure"));
         } catch (IDPPException ie) {
              IDPPUtils.debug.error("IDPPContainers:getContainerObject:" +
              "Error while creating legal identity.", ie);
              throw new IDPPException(ie);
         }
     }

     /**
      * Gets the AltIDType JAXB Object.
      * @param userMap user data
      * @return AltIDType JAXB Object
      * @exception IDPPException
      */
     private AltIDType getAltID(Map userMap) throws IDPPException {
        IDPPUtils.debug.message("IDPPLegalIdentity:getAltID:Init");
        AltIDType altID = null;
        try {
            altID = IDPPUtils.getIDPPFactory().createAltIDType();
            String altIDType = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
            IDPPConstants.ALT_ID_TYPE_ELEMENT).toLowerCase());

            if(altIDType != null) {
               DSTURI uri = getDSTURI(altIDType);
               altID.setIDType(uri);
            }

            String altIDValue = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
            IDPPConstants.ALT_ID_VALUE_ELEMENT).toLowerCase());

            if(altIDValue != null) {
               DSTString str = getDSTString(altIDValue);
               altID.setIDValue(str);
            }

            if(altIDType != null && altIDValue != null) {
               return altID;
            }

            return null;
        } catch (JAXBException je) {
            IDPPUtils.debug.error("IDPPContainers:getAltID: JAXB failure", je);
            throw new IDPPException(
            IDPPUtils.bundle.getString("jaxbFailure"));
        }
     }

     /**
      * Gets the VATType JAXB Object.
      * @param userMap user data
      * @return VATType JAXB Object
      * @exception IDPPException
      */
     private VATType getVAT(Map userMap) throws IDPPException {
        IDPPUtils.debug.message("IDPPLegalIdentity:getVATType:Init");
        VATType vType = null;
        try {
            vType = IDPPUtils.getIDPPFactory().createVATType();
            String value = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.ID_TYPE_ELEMENT).toLowerCase());
            if(value != null) {
               DSTURI uri = getDSTURI(value);
               vType.setIDType(uri);
            }
            value = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.ID_VALUE_ELEMENT).toLowerCase());
            if(value != null) {
               DSTString str = getDSTString(value);
               vType.setIDValue(str);
            } else {
               IDPPUtils.debug.message("IDPPContainers:getVAT: VAT value" +
                "is not configured in legal dentity");
               return null;
            }
            return vType;
        } catch (JAXBException je) {
            IDPPUtils.debug.error("IDPPContainers:getVAT: JAXB failure", je);
            throw new IDPPException(
            IDPPUtils.bundle.getString("jaxbFailure"));
        }
     }

     /**
      * Gets required common name container attributes.
      * @return Set set of required container attributes 
      */
     public Set getContainerAttributes() {
         IDPPUtils.debug.message("IDPPLegalIdentity:getContainerAttrib:Init");
         Set set = new HashSet();
         set.add(IDPPConstants.FN_ELEMENT);
         set.add(IDPPConstants.SN_ELEMENT);
         set.add(IDPPConstants.PT_ELEMENT);
         String nameScheme = IDPPServiceManager.getInstance().getNameScheme();
         if(nameScheme != null && 
            nameScheme.equals(IDPPConstants.NAME_SCHEME_MIDDLE)) {
            set.add(IDPPConstants.MN_ELEMENT);
         }
         set.add(IDPPConstants.LEGAL_NAME_ELEMENT);
         set.add(IDPPConstants.ALT_ID_TYPE_ELEMENT);
         set.add(IDPPConstants.ALT_ID_VALUE_ELEMENT);
         set.add(IDPPConstants.GENDER_ELEMENT);
         set.add(IDPPConstants.DOB_ELEMENT);
         set.add(IDPPConstants.MARITAL_STATUS_ELEMENT);
         set.add(IDPPConstants.ID_TYPE_ELEMENT);
         set.add(IDPPConstants.ID_VALUE_ELEMENT);
         return getMapperAttributeSet(set);
     }

     /**
      * Gets the container attributes for a given select expression.
      * @param select Select Expression.
      * @return Set set of required user attributes.
      */
     public Set getContainerAttributesForSelect(String select) {
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPLegalIdentity:getContainerAttributes" +
           "ForSelect:Init");
        }
        String expContext = IDPPUtils.getExpressionContext(select);
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPLegalIdentity:getContainerAttributes" +
           "ForSelect:exp context = " + expContext);
        }
        int attrType = IDPPUtils.getIDPPElementType(expContext);
        Set set = new HashSet();
        switch(attrType) {
                 case IDPPConstants.SN_ELEMENT_INT:
                 case IDPPConstants.FN_ELEMENT_INT:
                 case IDPPConstants.MN_ELEMENT_INT:
                 case IDPPConstants.PT_ELEMENT_INT:
                 case IDPPConstants.LEGAL_NAME_ELEMENT_INT:
                 case IDPPConstants.DOB_ELEMENT_INT:
                 case IDPPConstants.GENDER_ELEMENT_INT:
                 case IDPPConstants.MARITAL_STATUS_ELEMENT_INT:
                 case IDPPConstants.ID_TYPE_ELEMENT_INT:
                 case IDPPConstants.ID_VALUE_ELEMENT_INT:
                      set.add(expContext);
                      break;
                 case IDPPConstants.ALT_ID_ELEMENT_INT:
                      set.add(IDPPConstants.ALT_ID_TYPE_ELEMENT);
                      set.add(IDPPConstants.ALT_ID_VALUE_ELEMENT);
                      break;
                 case IDPPConstants.VAT_ELEMENT_INT:
                      set.add(IDPPConstants.ID_VALUE_ELEMENT);
                      set.add(IDPPConstants.ID_TYPE_ELEMENT);
                      break;
                 case IDPPConstants.ANALYZED_NAME_ELEMENT_INT:
                      return getAnalyzedNameAttributes();
                 case IDPPConstants.LEGAL_IDENTITY_ELEMENT_INT:
                      return getContainerAttributes();
                 default:
                      IDPPUtils.debug.error("IDPPLegalIdentity:getContainer"+
                      "AttributesForSelect. Invalid select.");
                      break;
         }
         return getMapperAttributeSet(set);
     }

     /**
      * Processes modify container values and returns key value/pair to be
      * modified.
      * @param select select expression
      * @param data list of new data objects.
      * @return Attribute key value pair for the given select.
      * @exception IDPPException.
      */
     public Map getDataMapForSelect(String select, List data) 
     throws IDPPException {
        IDPPUtils.debug.message("IDPPLegalIdentity:getDataMapForSelect:Init");
        Map map = new HashMap();
        String expContext = IDPPUtils.getExpressionContext(select);
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPLegalIdentity:getDataMapForSelect:" +
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
               case IDPPConstants.GENDER_ELEMENT_INT:
                   if((dataElement == null) ||
                      (dataElement instanceof GenderElement)) {
                      map = getAttributeMap(expContext, dataElement, map);
                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
                   break;
               case IDPPConstants.DOB_ELEMENT_INT:
                   if((dataElement == null) ||
                      (dataElement instanceof DOBElement)) {
                      map = getAttributeMap(expContext, dataElement, map);
                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
                   break;
               case IDPPConstants.ID_VALUE_ELEMENT_INT:
                   if((dataElement == null) ||
                      (dataElement instanceof IDValueElement)) {
                      String idType = getIDType(select);
                      if(IDPPUtils.debug.messageEnabled()) {
                         IDPPUtils.debug.message("IDPPLegalIdentity.getData" +
                         "MapForSelect: IDType= " + idType);
                      }

                      String idTypeKey = null;
                      String idValueKey = null;
                      DSTString idValue = (DSTString)dataElement;
                    
                      if(select.indexOf("AltID") != -1) {
                         idTypeKey = getAttributeMapper().getDSAttribute(
                           IDPPConstants.ALT_ID_TYPE_ELEMENT);
                         idValueKey = getAttributeMapper().getDSAttribute(
                           IDPPConstants.ALT_ID_VALUE_ELEMENT);
                      } else if(select.indexOf("VAT") != -1) {
                         idTypeKey = getAttributeMapper().getDSAttribute(
                           IDPPConstants.ID_TYPE_ELEMENT);
                         idValueKey = getAttributeMapper().getDSAttribute(
                           IDPPConstants.ID_VALUE_ELEMENT);
                      } else {
                         throw new IDPPException(
                         IDPPUtils.bundle.getString("invalid Element"));
                      }

                      Set idTypes = new HashSet();
                      idTypes.add(idType);
                      map.put(idTypeKey, idTypes);

                      Set idValues = new HashSet();
                      idValues.add(idValue.getValue());
                      map.put(idValueKey, idValues);

                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
                   break;
               case IDPPConstants.MARITAL_STATUS_ELEMENT_INT:
                   if((dataElement == null) ||
                      (dataElement instanceof MaritalStatusElement)) {
                      map = getAttributeMap(expContext, dataElement, map);
                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
                   break;
               case IDPPConstants.LEGAL_NAME_ELEMENT_INT:
                   if((dataElement == null) ||
                      (dataElement instanceof LegalNameElement)) {
                      map = getAttributeMap(expContext, dataElement, map);
                   } else {
                      throw new IDPPException(
                      IDPPUtils.bundle.getString("invalid Element"));
                   }
                   break;
               case IDPPConstants.ALT_ID_ELEMENT_INT:
                   map = getAltIDMap(data, map);
                   break;
               case IDPPConstants.VAT_ELEMENT_INT:
                   map = getVATMap(dataElement, map);
                   break;
               case IDPPConstants.ANALYZED_NAME_ELEMENT_INT:
                   map = getAnalyzedNameMap(dataElement, map);
                   break;
               case IDPPConstants.LEGAL_IDENTITY_ELEMENT_INT:
                   map = getLegalIdentityMap(dataElement, map);
                   break;
               default:
                   IDPPUtils.debug.error("IDPPLegalIdentity:getDataMapForSelect"
                   + "Unsupported element");
                   break;
          }
          if(IDPPUtils.debug.messageEnabled()) {
             IDPPUtils.debug.message("IDPPLegalIdentity:getDataMapForSelect:" +
             "Attr map to be modified." + map);
          }
          return map;
     }

     /**
      * Gets LegalIdentity attributes in a hashmap.
      * @param obj LegalIdentityType JAXB object.
      * @param map  map that sets attribute/value pairs.
      * @return Map required legal identity attrs hashmap.
      */
     private Map getLegalIdentityMap(Object obj, Map map) 
     throws IDPPException {
        IDPPUtils.debug.message("IDPPLegalIdentity:getLegalIdentityMap:Init");
        AnalyzedNameType analyzedName = null;
        VATType vat = null;
        List altIDs = null;
        DSTURI gender = null;
        DSTURI mStatus = null;
        DSTDate dob = null;
        DSTString lName = null;
        if(obj != null) {
           if(obj instanceof LegalIdentityType) {
              LegalIdentityType lType = (LegalIdentityType)obj;
              analyzedName = lType.getAnalyzedName();
              vat = lType.getVAT();
              altIDs = lType.getAltID();
              dob = lType.getDOB();
              mStatus = lType.getMaritalStatus();
              gender = lType.getGender();
              lName = lType.getLegalName();
           } else {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }
        }
        getAnalyzedNameMap(analyzedName, map);
        getVATMap(vat, map);
        getAltIDMap(altIDs, map);
        getAttributeMap(IDPPConstants.GENDER_ELEMENT, gender, map);
        getAttributeMap(IDPPConstants.MARITAL_STATUS_ELEMENT, mStatus, map);
        getAttributeMap(IDPPConstants.LEGAL_NAME_ELEMENT, lName, map);
        getAttributeMap(IDPPConstants.DOB_ELEMENT, dob, map);
        return map;
     }

     /**
      * Gets the AltID attributes map
      * @param dataObject List of altID objects
      * @param map map of attrib/val pairs
      * @return Map map of attrib/val pairs
      * @exception IDPPException
      */ 
     private Map getAltIDMap(List dataObject, Map map) throws IDPPException {
        //Currently, we handle only one AltID.
        DSTURI altIDType = null;
        DSTString altIDValue = null;
        if(dataObject != null && dataObject.size() != 0) {
           int size = dataObject.size();
           for(int i= 0; i < size; i++) {
               Object dataElement = dataObject.get(i);
               if(dataElement instanceof AltIDElement) {
                  AltIDType altID = (AltIDType)dataElement;
                  altIDType = altID.getIDType(); 
                  altIDValue = altID.getIDValue();
               } else {
                  throw new IDPPException(
                  IDPPUtils.bundle.getString("invalid Element"));
               }
           }
        }
        getAttributeMap(IDPPConstants.ALT_ID_TYPE_ELEMENT, altIDType, map);
        getAttributeMap(IDPPConstants.ALT_ID_VALUE_ELEMENT, altIDValue, map);
        return map;
     }

     /**
      * Gets VAT attributes Map
      * @param obj VATType JAXB Object
      * @param map map of attrib vals
      * @return Map updated map
      */
     private Map getVATMap(Object obj, Map map) throws IDPPException {
        IDPPUtils.debug.message("IDPPLegalIdentity:getVATMap:Init");
        DSTURI idType = null;
        DSTString idValue = null;
        if(obj != null) {
           if(obj instanceof VATType) {
              VATType vType = (VATType)obj;
              idType = vType.getIDType();
              idValue = vType.getIDValue();
           } else {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }
        }
        getAttributeMap(IDPPConstants.ID_TYPE_ELEMENT, idType, map);
        getAttributeMap(IDPPConstants.ID_VALUE_ELEMENT, idValue, map);
        return map;
     }

     /**
      * Returns false for the IDPPLegalIdentity Container.
      */
     public boolean isSingleAttributeContainer() {
         return false;
     }

     /**
      * Checks if there are any binary attributes.
      */
     public boolean hasBinaryAttributes() {
        return false;
     }

     private String getIDType(String select) throws IDPPException {

        if(select.indexOf("[") == -1) {
           throw new IDPPException(IDPPUtils.bundle.getString("invalidSelect"));
        }

        int index1 = select.indexOf("IDType");
        if(index1 == -1) {
           throw new IDPPException(IDPPUtils.bundle.getString("invalidSelect"));
        } 

        String idType = select.substring(index1 + 7);
        int index2 = idType.indexOf("\"");
        if(index2 == -1 ) {
           throw new IDPPException(IDPPUtils.bundle.getString("invalidSelect"));
        }

        idType = idType.substring(index2+1, idType.length());
        int index3 = idType.indexOf("\"");
        if(index3 == -1 ) {
           throw new IDPPException(IDPPUtils.bundle.getString("invalidSelect"));
        }

        return idType.substring(0, index3);
     }


}
