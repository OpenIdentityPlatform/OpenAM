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
 * $Id: IDPPDemographics.java,v 1.2 2008/06/25 05:47:16 qcheng Exp $
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
 * This class <code>IDPPDemographics</code> is an implementation of 
 * <code>IDPPContainer</code>.
 */ 

public class IDPPDemographics extends IDPPBaseContainer {

     /**
      * Constructor
      */
     public IDPPDemographics() { 
         IDPPUtils.debug.message("IDPPDemographics:constructor:init.");
     }

     /**
      * Gets the common name jaxb element 
      * @param userMap user map
      * @return CommonNameElement JAXB Object.
      * @exception IDPPException.
      */
     public Object getContainerObject(Map userMap) throws IDPPException {

         IDPPUtils.debug.message("IDPPDemographics:getContainerObject:Init");

         try {
             PPType ppType = IDPPUtils.getIDPPFactory().createPPElement();
             DemographicsElement de = 
                  IDPPUtils.getIDPPFactory().createDemographicsElement();

             String displayLang = CollectionHelper.getMapAttr(userMap, 
               getAttributeMapper().getDSAttribute(
               IDPPConstants.DEMO_GRAPHICS_DISPLAY_LANG_ELEMENT).toLowerCase());

             if(displayLang != null) {
                de.setDisplayLanguage(getDSTString(displayLang));
             }

             Set languages = (Set)userMap.get(
                getAttributeMapper().getDSAttribute(
                IDPPConstants.DEMO_GRAPHICS_LANGUAGE_ELEMENT).toLowerCase());
             Iterator iter = languages.iterator();
             while(iter.hasNext()) {
                de.getLanguage().add(getDSTString((String)iter.next()));
             }

             String birthDay = CollectionHelper.getMapAttr(userMap, 
               getAttributeMapper().getDSAttribute(
               IDPPConstants.DEMO_GRAPHICS_BIRTH_DAY_ELEMENT).toLowerCase());
             if(birthDay != null) {
                de.setBirthday(getDSTMonthDay(birthDay));
             }

             String age = CollectionHelper.getMapAttr(userMap,
              getAttributeMapper().getDSAttribute(
              IDPPConstants.DEMO_GRAPHICS_AGE_ELEMENT).toLowerCase());
             if(age != null) {
                de.setAge(getDSTInteger(age));
             }

             String timeZone = CollectionHelper.getMapAttr(userMap,
                 getAttributeMapper().getDSAttribute(
                 IDPPConstants.DEMO_GRAPHICS_TIME_ZONE_ELEMENT).toLowerCase());
             if(timeZone != null) {
                de.setTimeZone(getDSTString(timeZone));
             }

             ppType.setDemographics(de);

             return ppType;
         } catch (JAXBException je) {
             IDPPUtils.debug.error(
              "IDPPDemographics:getContainerObject: JAXB failure", je); 
              throw new IDPPException(
              IDPPUtils.bundle.getString("jaxbFailure"));
         }
     }

     /**
      * Gets required common name container attributes.
      * @return a set of required container attributes 
      */
     public Set getContainerAttributes() {
         Set set = new HashSet();
         set.add(IDPPConstants.DEMO_GRAPHICS_DISPLAY_LANG_ELEMENT);
         set.add(IDPPConstants.DEMO_GRAPHICS_LANGUAGE_ELEMENT);
         set.add(IDPPConstants.DEMO_GRAPHICS_BIRTH_DAY_ELEMENT);
         set.add(IDPPConstants.DEMO_GRAPHICS_AGE_ELEMENT);
         set.add(IDPPConstants.DEMO_GRAPHICS_TIME_ZONE_ELEMENT);
         return getMapperAttributeSet(set);
     }

     /**
      * Gets the container attributes for a given select expression.
      * @param select Select expression.
      * @return a set of required user attributes.
      */
     public Set getContainerAttributesForSelect(String select) {
        
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPDemographics:getContainer" +
           "AttributesForSelect:Init");
        }

        String expContext = IDPPUtils.getExpressionContext(select);
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPDemographics:getContainer" +
           "AttributesForSelect:exp context = " + expContext);
        }
 
        Set set = new HashSet();
        if(expContext == null || expContext.length() == 0) {
           return set;
        }

        if(expContext.equals(IDPPConstants.DEMOGRAPHICS_ELEMENT)) {
           return getContainerAttributes();
        } else { 
           set.add(expContext);
           return getMapperAttributeSet(set);
        }

     }

     /**
      * Processes modify container values and returns key value/pair to be
      * modified.
      * @param select Select expression.
      * @param data list of new data objects.
      * @return Map Attribute key value pair map for the given select.
      * @exception IDPPException.
      */
     public Map getDataMapForSelect(String select, List data) 
     throws IDPPException {

        IDPPUtils.debug.message("IDPPDemographics:getDataMapForSelect:Init");

        Map map = new HashMap();
        String expContext = IDPPUtils.getExpressionContext(select);
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPDemographics:getDataMapForSelect:" +
           "exp context = " + expContext);
        }

        if(expContext == null && expContext.length() == 0) {
           return map;
        }

        Object dataElement = null;
        if(data != null && !data.isEmpty()) {
           dataElement = data.get(0);
        }
        if(expContext.equals(IDPPConstants.DEMOGRAPHICS_ELEMENT)) {
           if((dataElement == null) || 
              (dataElement instanceof DemographicsElement)) {
               map = getDemographicsMap(dataElement, map);
           } else {
               throw new IDPPException(
               IDPPUtils.bundle.getString("invalid Element"));
           }
        } else if(expContext.equals(
              IDPPConstants.DEMO_GRAPHICS_DISPLAY_LANG_ELEMENT)) {
           if((dataElement == null) || 
              (dataElement instanceof DisplayLanguageElement)) {
               map = getAttributeMap(
               IDPPConstants.DEMO_GRAPHICS_DISPLAY_LANG_ELEMENT, 
               dataElement, map);
           } else {
               throw new IDPPException(
               IDPPUtils.bundle.getString("invalid Element"));
           }
        } else if(expContext.equals(
              IDPPConstants.DEMO_GRAPHICS_BIRTH_DAY_ELEMENT)) {
           if((dataElement == null) || 
              (dataElement instanceof BirthdayElement)) {
               map = getAttributeMap(
               IDPPConstants.DEMO_GRAPHICS_BIRTH_DAY_ELEMENT,
               dataElement, map);
           } else {
               throw new IDPPException(
               IDPPUtils.bundle.getString("invalid Element"));
           }
        } else if(expContext.equals(
              IDPPConstants.DEMO_GRAPHICS_AGE_ELEMENT)) {
           if((dataElement == null) || 
              (dataElement instanceof AgeElement)) {
               map = getAttributeMap(IDPPConstants.DEMO_GRAPHICS_AGE_ELEMENT,
               dataElement, map);
           } else {
               throw new IDPPException(
               IDPPUtils.bundle.getString("invalid Element"));
           }
        } else if(expContext.equals(
           IDPPConstants.DEMO_GRAPHICS_LANGUAGE_ELEMENT)) {
           if((dataElement == null) || 
              (dataElement instanceof LanguageElement)) {
               map = getLanguageMap(data, map);
           } else {
               throw new IDPPException(
               IDPPUtils.bundle.getString("invalid Element"));
           }
        } else {
           IDPPUtils.debug.error("IDPPDemographics:getDataMapForSelect"
           + "Unsupported element");
        }

        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPDemographics:getDataMapForSelect:" +
           "Attr map to be modified." + map);
        }
        return map;

     }

     /**
      * Gets the Demographics container attributes in a hashmap.
      * @param obj DemographicsType JAXB object.
      * @param map map that sets attribute/value pairs.
      * @return Map Attribute value pair map that needs to be modified. 
      * @exception IDPPException.
      */
     private Map getDemographicsMap(Object obj, Map map) 
        throws IDPPException {

        IDPPUtils.debug.message("IDPPDemographics:getDemographicsMap:Init");

        DSTString displayLang = null; 
        DSTInteger age = null; 
        DSTMonthDay  birthDay = null;
        List languages = null;
        DSTString timeZone = null;
         
        if(obj != null) {
           if(obj instanceof DemographicsType) {
              DemographicsType demoGraphs = (DemographicsType)obj;
              displayLang = demoGraphs.getDisplayLanguage();
              age = demoGraphs.getAge();
              birthDay = demoGraphs.getBirthday();
              languages = demoGraphs.getLanguage();
              timeZone = demoGraphs.getTimeZone();
           } else {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }
        }
        getLanguageMap(languages, map);
        getAttributeMap(IDPPConstants.DEMO_GRAPHICS_DISPLAY_LANG_ELEMENT,
                 displayLang, map);
        getAttributeMap(IDPPConstants.DEMO_GRAPHICS_AGE_ELEMENT, age, map);
        getAttributeMap(IDPPConstants.DEMO_GRAPHICS_TIME_ZONE_ELEMENT,
                 timeZone, map);
        getAttributeMap(IDPPConstants.DEMO_GRAPHICS_BIRTH_DAY_ELEMENT,
                 birthDay, map);
        return map;
     }

     /**
      * Gets the Lanaguage attributes map
      * @param dataObject list of data objects
      * @param map map to be filled in
      * @return Map map to be returned
      * @exception IDPPException
      */
     private Map getLanguageMap(List dataObject, Map map) throws IDPPException {
        IDPPUtils.debug.message("IDPPDemographics:getLanguageMap:Init");

        if(dataObject == null) {
           return getAttributeMap(
           IDPPConstants.DEMO_GRAPHICS_LANGUAGE_ELEMENT, null, map);
        }

        return getAttributeMap(IDPPConstants.DEMO_GRAPHICS_LANGUAGE_ELEMENT, 
            dataObject, map);
     }

     /**
      * Checks if there are any binary attributes.
      */
     public boolean hasBinaryAttributes() {
         return false;
     }

}
