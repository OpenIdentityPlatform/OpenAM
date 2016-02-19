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
 * $Id: IDPPBaseContainer.java,v 1.2 2008/06/25 05:47:15 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.idpp.container;

import com.sun.identity.shared.datastruct.CollectionHelper;
import javax.xml.bind.JAXBException;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.lang.NumberFormatException;
import java.math.BigInteger;
import org.w3c.dom.Document;
import com.sun.identity.liberty.ws.idpp.common.*;
import com.sun.identity.liberty.ws.idpp.jaxb.*;
import com.sun.identity.liberty.ws.idpp.plugin.*;
import com.sun.identity.liberty.ws.idpp.IDPPServiceManager;


/**
 * This class <code>IDPPBaseContainer</code> is base class for common 
 * funcionality for any IDPPContainer.
 */ 

public abstract class IDPPBaseContainer implements IDPPContainer {


     protected String userDN = null;

     /**
      * Gets the container Object. Each container need to
      * to implement this method.
      * @param userMap the user Map  
      */
     public abstract Object getContainerObject(Map userMap) 
     throws IDPPException;

     /**
      * Gets container attributes. Each container need to
      * implement this method.
      */
     public abstract Set getContainerAttributes();

     /**
      * Gets the container attributes for select. Each
      * container need to implement this method.
      * @param select Select Expression
      */
     public abstract Set getContainerAttributesForSelect(String select);

     /**
      * Gets data map for select. Each container need to 
      * implement this method.
      * @param select Select expression
      * @param data a List 
      */
     public abstract Map getDataMapForSelect(String select, List data)
     throws IDPPException;

     /**
      * Returns the caller map with updated attribute and the value from the
      * given data object. 
      * @param attr IDPP attribute name.
      * @param obj object 
      * @param map  this map is passed by the caller to set the attribute
      *             and the data value from the given object. 
      * @return Map updated attribute value pair map for the caller map.  
      */
     protected Map getAttributeMap(String attr, Object obj, Map map) {
        if(attr == null || map == null) {
           IDPPUtils.debug.error("IDPPBaseContainer:getAttributeMap:nullval");
           return map;
        }
        String attrKey = getAttributeMapper().getDSAttribute(attr);
        if(attrKey == null) {
           if(IDPPUtils.debug.messageEnabled()) {
              IDPPUtils.debug.message("IDPPBaseContainer:getAttributeMap: " +
              "There's no DS Attribute mapping for this attr:" + attr);
           }
           return map;
        }
        HashSet set = new HashSet();
        if(obj != null) {
           if(obj instanceof java.util.List) {
              Set values = new HashSet();
              List list = (List)obj;
              Iterator iter = list.iterator();
              while(iter.hasNext()) {
                 Object ob = iter.next();
                 if(ob instanceof DSTString) {
                    DSTString str = (DSTString)ob;
                    String val = str.getValue();
                    values.add(val);
                 } else if(obj instanceof DSTURI) {
                    DSTURI uri = (DSTURI)ob;
                    String val = uri.getValue();
                    values.add(val);
                 }
              }
              map.put(attrKey, values);
              return map;
           }
           String value = null;
           if(obj instanceof DSTString) {
              DSTString str = (DSTString)obj;
              value = str.getValue();
           } else if (obj instanceof DSTURI) {
              DSTURI uri = (DSTURI)obj;
              value = uri.getValue();
           } else if (obj instanceof DSTDate) {
              DSTDate date = (DSTDate)obj;
              Calendar cal = date.getValue();
              if(cal != null) {
                 value = DateFormat.getDateInstance().format(cal.getTime());
              }

           } else if (obj instanceof DSTInteger) {
              DSTInteger dstInt = (DSTInteger)obj;
              value = dstInt.getValue().toString(); 

           } else if (obj instanceof DSTMonthDay) {
              DSTMonthDay dstMon = (DSTMonthDay)obj;
              value = dstMon.getValue();
           }

           if(value != null) {
              set.add(value);
           }
        }
        map.put(attrKey, set);
        return map;
     }

     /**
      * Gets the mapper attributes for a given IDPP attribute set.
      * @param set IDPP attribute set 
      * @return Set attribute set from the attribute mapper 
      */
     protected Set getMapperAttributeSet(Set set) {
        if(set == null) {
           IDPPUtils.debug.error("IDPPBaseContainer:getAttributeSet:nullval");
           return null;
        }
        Set returnSet = new HashSet();
        Iterator iter = set.iterator();
        while(iter.hasNext()) {
           String attrKey = (String)iter.next();
           String attrMap = getAttributeMapper().getDSAttribute(attrKey);
           if(attrMap == null) {
              if(IDPPUtils.debug.messageEnabled()) {
                 IDPPUtils.debug.message("IDPPBaseContainer:getAttributeSet:" +
                 "no mapping defined for this attrib:" + attrKey);
              }
              continue;
           } else {
              returnSet.add(attrMap);
           }
        }
        return returnSet;
     }

     /**
      * Gets a JAXB DSTString object.
      * @param value a String representing the value.
      * @return DSTString JAXB object.
      */
     protected DSTString getDSTString(String value) {
        if(value == null) {
           IDPPUtils.debug.message("IDPPBaseContainer:getDSTString:null vals");
           return null;
        }
        try {
            DSTString dstString = IDPPUtils.getIDPPFactory().createDSTString();
            dstString.setValue(value);
            return dstString;
        } catch (JAXBException je) {
            IDPPUtils.debug.error("IDPPBaseContainer:getDSTString:jaxbFail",je);
            return null;
        }
     }

     /**
      * Gets a JAXB DSTDate object.
      * @param value a String representing the value.
      * @return DSTDate JAXB object.
      */
     protected DSTDate getDSTDate(String value) {
        if(value == null) {
           IDPPUtils.debug.message("IDPPBaseContainer:getDSTDate:null vals");
           return null;
        }
        try {
            DSTDate dstDate = IDPPUtils.getIDPPFactory().createDSTDate();
            Date date = 
                 DateFormat.getDateInstance(DateFormat.MEDIUM).parse(value);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            dstDate.setValue(cal);
            return dstDate;
        } catch(Exception e) {
            IDPPUtils.debug.error("IDPPBaseContainer:getDSTDate: Exception", e);
            return null;
        }
     }

     /**
      * Gets a JAXB DSTMonthDay object.
      * @param value a String representing the value.
      * @return DSTMonthDay JAXB object.
      */
     protected DSTMonthDay getDSTMonthDay(String value) {
        if(value == null) {
           IDPPUtils.debug.message("IDPPBaseContainer:getDSTMonthDay:nullvals");
           return null;
        }
        try {
            DSTMonthDay dstMonthDay = 
                 IDPPUtils.getIDPPFactory().createDSTMonthDay();
            dstMonthDay.setValue(value);
            return dstMonthDay;
        } catch(Exception e) {
            IDPPUtils.debug.error("IDPPBaseContainer:getDSTMonthDay: " +
            "Exception", e);
            return null;
        }
     }


     /**
      * Gets a JAXB DSTURI object.
      * @param value a String representing the value.
      * @return DSTURI JAXB object.
      */
     protected DSTURI getDSTURI(String value) {
        if(value == null) {
           IDPPUtils.debug.message("IDPPBaseContainer:getDSTURI:null vals");
           return null;
        }
        try {
            DSTURI dstURI = IDPPUtils.getIDPPFactory().createDSTURI();
            dstURI.setValue(value);
            return dstURI;
        } catch(JAXBException je) {
            IDPPUtils.debug.error("IDPPBaseContainer:getDSTURI: Exception", je);
            return null;
        }
     }

     /**
      * Gets a JAXB DSTInteger object.
      * @param value a String representing the value.
      * @return DSTInteger JAXB object.
      */
     protected DSTInteger getDSTInteger(String value) {
        if(value == null) {
           IDPPUtils.debug.message("IDPPBaseContainer:getDSTInteger:null vals");
           return null;
        }
        try {
            DSTInteger dstInteger = 
                 IDPPUtils.getIDPPFactory().createDSTInteger();
            dstInteger.setValue(new BigInteger(value));
            return dstInteger;
        } catch(JAXBException je) {
            IDPPUtils.debug.error("IDPPBaseContainer:getDSTInteger:Error", je);
            return null;
        } catch(NumberFormatException nfe) {
            IDPPUtils.debug.error("IDPPBaseContainer:getDSTInteger: " +
            "Invalid number", nfe);
            return null;
        }
     }


     /**
      * Gets AnalyzedName JAXB Object.
      * @param userMap user map
      * @return AnalyzedNameType JAXB Object.
      * @exception IDPPException.
      */
     protected AnalyzedNameType getAnalyzedName(Map userMap) 
     throws IDPPException {
        IDPPUtils.debug.message("IDPPContainers:getAnalyzedName:Init");
        AnalyzedNameType analyzedName = null;
        try {
            analyzedName = IDPPUtils.getIDPPFactory().createAnalyzedNameType();

            String value = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.SN_ELEMENT).toLowerCase());
            if(value != null) {
               analyzedName.setSN(getDSTString(value));
            }

            value = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.FN_ELEMENT).toLowerCase());
            if(value != null) {
               analyzedName.setFN(getDSTString(value));
            }

            value = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.PT_ELEMENT).toLowerCase());
            if(value != null) {
               analyzedName.setPersonalTitle(getDSTString(value));
            }

            value = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.MN_ELEMENT).toLowerCase());

            String nameScheme = 
                 IDPPServiceManager.getInstance().getNameScheme();
            if(nameScheme != null) {
               analyzedName.setNameScheme(nameScheme);
            }
            if(nameScheme != null && nameScheme.equals(
               IDPPConstants.NAME_SCHEME_MIDDLE) && value != null) {
               analyzedName.setMN(getDSTString(value));
            }
            return analyzedName;
        } catch (JAXBException je) {
            IDPPUtils.debug.error("IDPPContainers:getAnalyzedName: " +
            "JAXB failure", je);
             throw new IDPPException(
             IDPPUtils.bundle.getString("jaxbFailure"));
        }
     }

     /**
      * Converts into an XML document so that XPATH can be applied.
      * This method is an implementation of IDPPContainer. 
      * @param userMap user map
      * @return Document XML document representation of container.
      * @exception IDPPException
      */
     public Document toXMLDocument(Map userMap) 
     throws IDPPException {
        IDPPUtils.debug.message("IDPPBaseContainer:toXMLDocument: Init");
        if(userMap == null) {
           IDPPUtils.debug.error("IDPPBaseContainer:toXMLDocument:Nodata");
           throw new IDPPException(IDPPUtils.bundle.getString("noData"));
        }
        Document doc = IDPPUtils.getDocumentBuilder().newDocument();
        try {
            IDPPUtils.getMarshaller().marshal(getContainerObject(userMap),doc);
            return doc;
        } catch (JAXBException je) {
            IDPPUtils.debug.error("IDPPBaseContainer:toXMLDocument:"+
            "JAXB exception while marshalling container .", je);
            throw new IDPPException(
            IDPPUtils.bundle.getString("jaxbFailure"));
        } catch(IDPPException ie) {
            IDPPUtils.debug.error("IDPPBaseContainer:toXMLDocument:" +
            "Error retrieving common name.", ie);
             throw new IDPPException(ie);
        }
     }

     /**
      * Creates required set of attributes.
      * @return set of required attributes.
      */
     protected Set getAnalyzedNameAttributes() {
        Set set = new HashSet(); 
        set.add(IDPPConstants.FN_ELEMENT);
        set.add(IDPPConstants.SN_ELEMENT);
        set.add(IDPPConstants.PT_ELEMENT);
        String nameScheme = IDPPServiceManager.getInstance().getNameScheme();
        if(nameScheme != null &&
           nameScheme.equals(IDPPConstants.NAME_SCHEME_MIDDLE)) {
           set.add(IDPPConstants.MN_ELEMENT);
        }
        return getMapperAttributeSet(set);
     }
      
     /**
      * Gets analzedname attributes in a hashmap.
      * @param obj AnalyzedNameType JAXB object.
      * @param  map  map that sets attribute/value pairs.
      * @return Map required analyzed name hashmap.
      */
     protected Map getAnalyzedNameMap(Object obj, Map map) throws IDPPException{
        IDPPUtils.debug.message("IDPPBaseContainer:getAnalyzedNameMap:Init");
        
        DSTString fn = null, sn= null, mn= null, pt = null;
        if(obj != null) { 
           if(obj instanceof AnalyzedNameType) {
              AnalyzedNameType analyzedName = (AnalyzedNameType)obj;
              fn = analyzedName.getFN();
              sn = analyzedName.getSN();
              mn =  analyzedName.getMN();
              pt = analyzedName.getPersonalTitle();
           } else {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }

        }
        getAttributeMap(IDPPConstants.FN_ELEMENT, fn, map);
        getAttributeMap(IDPPConstants.SN_ELEMENT, sn, map);
        String nameScheme = IDPPServiceManager.getInstance().getNameScheme();
        if(nameScheme != null && 
           nameScheme.equals(IDPPConstants.NAME_SCHEME_MIDDLE)) {
           getAttributeMap(IDPPConstants.MN_ELEMENT, mn, map);
        }
        getAttributeMap(IDPPConstants.PT_ELEMENT, pt, map);
        return map;
     }
 
     /**
      * Gets Attribute Mapper
      * @return Attribute Mapper instance 
      */
     protected AttributeMapper getAttributeMapper() {
         return IDPPServiceManager.getInstance().getAttributeMapper(); 
     }
 
     /**
      * Gets extension attribute 
      * @return extension attributes
      */
     protected Set getExtensionContainerAttributes() {
         return IDPPServiceManager.getInstance().getExtensionAttributes();
     }

     /**
      * Gets container extension
      * @return IDDExtension object 
      */
     protected IDPPExtension getExtensionContainerClass() {
         return IDPPServiceManager.getInstance().getContainerExtension(
         IDPPConstants.EXTENSION_ELEMENT);
     }
  
     /**
      * Sets user DN
      * @param user userDN
      */ 
     public void setUserDN(String user) {
         if(IDPPUtils.debug.messageEnabled()) {
            IDPPUtils.debug.message("IDPPBaseContainer.setUserDN: userDN" +
                user);
         }
         this.userDN = user;
     }

}
