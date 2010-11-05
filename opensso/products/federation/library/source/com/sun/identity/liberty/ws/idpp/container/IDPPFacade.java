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
 * $Id: IDPPFacade.java,v 1.2 2008/06/25 05:47:16 qcheng Exp $
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
 * This class <code>IDPPFacade</code> is an implementation of 
 * <code>IDPPContainer</code>.
 */ 

public class IDPPFacade extends IDPPBaseContainer {

     /**
      * Constructor
      */
     public IDPPFacade() { 
         IDPPUtils.debug.message("IDPPFacade:constructor:init.");
     }

     /**
      * Gets the Facade JAXB Element. 
      * @param userMap user map
      * @return FacadeElement JAXB Object.
      * @exception IDPPException.
      */
     public Object getContainerObject(Map userMap) throws IDPPException {

         IDPPUtils.debug.message("IDPPFacade:getContainerObject:Init");
         try {
             PPType ppType = IDPPUtils.getIDPPFactory().createPPElement();
             FacadeElement fe = 
                  IDPPUtils.getIDPPFactory().createFacadeElement();

             String mugShot = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.MUGSHOT_ELEMENT).toLowerCase());

             if(mugShot != null) {
                fe.setMugShot(getDSTURI(mugShot));
             }

             String webSite = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.WEBSITE_ELEMENT).toLowerCase());
             if(webSite != null) {
                fe.setWebSite(getDSTURI(webSite));
             }

             String namePronounced = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.NAME_PRONOUNCED_ELEMENT).toLowerCase());
             if(namePronounced != null) {
                fe.setNamePronounced(getDSTURI(namePronounced));
             }

             String greetSound = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.GREET_SOUND_ELEMENT).toLowerCase());
             if(greetSound != null) {
                fe.setGreetSound(getDSTURI(greetSound));
             }

             String greetMeSound = CollectionHelper.getMapAttr(
                userMap, getAttributeMapper().getDSAttribute(
                    IDPPConstants.GREET_ME_SOUND_ELEMENT).toLowerCase());
             if(greetMeSound != null) {
                fe.setGreetMeSound(getDSTURI(greetMeSound));
             }
             ppType.setFacade(fe);

             return ppType;
         } catch (JAXBException je) {
             IDPPUtils.debug.error(
              "IDPPFacade:getContainerObject: JAXB failure", je); 
              throw new IDPPException(
              IDPPUtils.bundle.getString("jaxbFailure"));
         }
     }

     /**
      * Gets the Facade Attribute Set.
      * @return Set set of required container attributes 
      */
     public Set getContainerAttributes() {
         IDPPUtils.debug.message("IDPPFacade:getContainerAttributes:Init");
         Set set = new HashSet();
         set.add(IDPPConstants.MUGSHOT_ELEMENT);
         set.add(IDPPConstants.WEBSITE_ELEMENT);
         set.add(IDPPConstants.NAME_PRONOUNCED_ELEMENT);
         set.add(IDPPConstants.GREET_SOUND_ELEMENT);
         set.add(IDPPConstants.GREET_ME_SOUND_ELEMENT);
         return getMapperAttributeSet(set);
     }

     /**
      * Gets the container attributes for a given select expression.
      * @param select Select expression.
      * @return Set set of required user attributes.
      */
     public Set getContainerAttributesForSelect(String select) {

        String expContext = IDPPUtils.getExpressionContext(select);
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPFacade:getContainerAttribSelect:" +
           "exp context = " + expContext);
        }
        Set set = new HashSet();
        if(expContext == null || expContext.length() == 0) {
           return set;
        }
        if(expContext.equals(IDPPConstants.FACADE_ELEMENT)) {
           return getContainerAttributes();
        } else { 
           set.add(expContext);
           return getMapperAttributeSet(set);
        }
     }

     /**
      * Gets the data key value map that needs to be modified for the
      * given select expression. 
      * @param select Select Expression.
      * @param data list of new data objects.
      * @return Map Key/value data map.
      * @exception IDPPException.
      */
     public Map getDataMapForSelect(String select, List data) 
     throws IDPPException {
        IDPPUtils.debug.message("IDPPFacade:getDataMapForSelect:Init");
        Map map = new HashMap();

        String expContext = IDPPUtils.getExpressionContext(select);
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPFacade:getDataMapForSelect:" +
           "exp context = " + expContext);
        }

        if(expContext == null || expContext.length() == 0) {
           return map;
        }
        Object dataElement = null;
        if(data != null && !data.isEmpty()) {
           dataElement = data.get(0);
        }

        if(expContext.equals(IDPPConstants.MUGSHOT_ELEMENT)) {
           if((dataElement == null) || 
                      (dataElement instanceof MugShotElement)) {
              map = getAttributeMap(expContext, dataElement, map);
           } else  {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }
        } else if(expContext.equals(IDPPConstants.WEBSITE_ELEMENT)) {
           if((dataElement == null) || 
                      (dataElement instanceof WebSiteElement)) {
              map = getAttributeMap(expContext, dataElement, map);
           } else  {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }
        } else if(expContext.equals(IDPPConstants.NAME_PRONOUNCED_ELEMENT)) {
           if((dataElement == null) || 
                      (dataElement instanceof NamePronouncedElement)) {
              map = getAttributeMap(expContext, dataElement, map);
           } else  {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }
        } else if(expContext.equals(IDPPConstants.GREET_SOUND_ELEMENT)) {
           if((dataElement == null) || 
                      (dataElement instanceof GreetSoundElement)) {
              map = getAttributeMap(expContext, dataElement, map);
           } else  {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }
        } else if(expContext.equals(IDPPConstants.GREET_ME_SOUND_ELEMENT)) {
           if((dataElement == null) || 
                      (dataElement instanceof GreetMeSoundElement)) {
              map = getAttributeMap(expContext, dataElement, map);
           } else  {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }
        } else if(expContext.equals(IDPPConstants.FACADE_ELEMENT)) {
           if((dataElement == null) || 
                      (dataElement instanceof FacadeElement)) {
               map = getFacadeMap(dataElement, map);
           } else  {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalid Element"));
           }
          
        } else {
           throw new IDPPException(
           IDPPUtils.bundle.getString("invalid Element"));
        }

        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPFacade:getDataMapForSelect:" +
           "Attr map to be modified." + map);
        }
        return map;
     }

     /**
      * Gets the facade object to be modified.
      */
     private Map getFacadeMap(Object obj, Map map) {
         DSTURI mugShot = null;
         DSTURI webSite = null;
         DSTURI namePronounced = null;
         DSTURI greetSound = null;
         DSTURI greetMeSound = null;
         if(obj != null) {
            FacadeElement fe = (FacadeElement)obj;
            mugShot = fe.getMugShot();
            webSite = fe.getWebSite();
            namePronounced = fe.getNamePronounced();
            greetSound = fe.getGreetSound();
            greetMeSound = fe.getGreetMeSound();
         }
         getAttributeMap(IDPPConstants.MUGSHOT_ELEMENT, mugShot, map);
         getAttributeMap(IDPPConstants.WEBSITE_ELEMENT, webSite, map);
         getAttributeMap(IDPPConstants.NAME_PRONOUNCED_ELEMENT, 
                    namePronounced, map);
         getAttributeMap(IDPPConstants.GREET_SOUND_ELEMENT, greetSound, map);
         getAttributeMap(IDPPConstants.GREET_ME_SOUND_ELEMENT, 
                    greetMeSound, map);
         return map;
     } 

     /**
      * Checks if there are any binary attributes.
      */
     public boolean hasBinaryAttributes() {
         return false;
     }
}
