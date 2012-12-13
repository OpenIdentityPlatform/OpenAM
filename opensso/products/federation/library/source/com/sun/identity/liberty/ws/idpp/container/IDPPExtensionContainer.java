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
 * $Id: IDPPExtensionContainer.java,v 1.2 2008/06/25 05:47:16 qcheng Exp $
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
import java.util.Iterator;
import java.util.StringTokenizer;
import org.w3c.dom.Document;
import com.sun.identity.liberty.ws.idpp.common.*;
import com.sun.identity.liberty.ws.idpp.jaxb.*;
import com.sun.identity.liberty.ws.idpp.plugin.*;
import com.sun.identity.liberty.ws.idpp.plugin.jaxb.PPISExtensionElement;
import com.sun.identity.liberty.ws.idpp.IDPPServiceManager;


/**
 * This class <code>IDPPExtensionContainer</code> is an implementation of 
 * <code>IDPPContainer</code> for the IDPP extensions.
 */ 

public class IDPPExtensionContainer extends IDPPBaseContainer {


     /**
      * Constructor
      */
     public IDPPExtensionContainer() { 
        IDPPUtils.debug.message("IDPPExtensionContainer:constructor:init.");
     }

     /**
      * Gets the container extension jaxb object.
      * @param userMap user map
      * @return ExtensionElement JAXB Object.
      * @exception IDPPException.
      */
     public Object getContainerObject(Map userMap) throws IDPPException {
        IDPPUtils.debug.message("IDPPContainers:getContainerObject:Init");
        try {
            PPType ppType = IDPPUtils.getIDPPFactory().createPPElement();
            ExtensionElement ee = 
                     IDPPUtils.getIDPPFactory().createExtensionElement();

            IDPPExtension extension = getExtensionContainerClass(); 
            if(extension != null) {
               ee.getAny().addAll(extension.getExtAttributes());
               ppType.setExtension(ee);
               return ppType;
            }

            Set extensionAttributes = getExtensionContainerAttributes();
            if(extensionAttributes == null || extensionAttributes.isEmpty()) {
               ppType.setExtension(ee);
               return ppType;
            }
        
            Iterator iter = extensionAttributes.iterator();
            while(iter.hasNext()) {
               String extName = (String)iter.next();
               String extValue = CollectionHelper.getMapAttr(userMap,
                    getAttributeMapper().getDSAttribute(extName).toLowerCase());
               if(extValue != null) {
                  ee.getAny().add(getISExtension(extName, extValue));
               }
            }

            ppType.setExtension(ee);
            return ppType;
        } catch (JAXBException je) {
            IDPPUtils.debug.error(
            "IDPPExtensionContainer:getContainerObject: JAXB failure", je); 
            throw new IDPPException(
            IDPPUtils.bundle.getString("jaxbFailure"));
        }
     }

     /**
      * Gets the extension container attributes.
      * @return Set set of required container attributes 
      */
     public Set getContainerAttributes() {
         if(getExtensionContainerClass() != null) {
            return new HashSet();
         }
         return getMapperAttributeSet(getExtensionContainerAttributes());
     }

     /**
      * Gets the container attributes for a given select expression.
      * @param select Select expression.
      * @return Set set of required user attributes.
      */
     public Set getContainerAttributesForSelect(String select) {

         if(IDPPUtils.debug.messageEnabled()) {
            IDPPUtils.debug.message("IDPPExtensionContainer.getContainer" +
            "AttributesForSelect:Init");
         }

         String expContext = IDPPUtils.getExpressionContext(select);
         if(expContext == null) {
            return new HashSet();
         }

         if(expContext.equals(IDPPConstants.EXTENSION_ELEMENT)) {
            return getContainerAttributes();
         }
         
         Set set = new HashSet();
         String dsAttribute = getExtAttributeName(select);
         if(dsAttribute != null) {
            set.add(dsAttribute);
         }

         return set;
     }

     /**
      * Gets the pp ds attribute name for a given select expression context.
      * @param select Select expression.
      * @return String dsattribute name
     */
     private String getExtAttributeName(String select) {

         if(IDPPUtils.debug.messageEnabled()) {
            IDPPUtils.debug.message("IDPPExtensionContainer.getExtAttribute" +
            "Name:Init");
         }
         String expContext = IDPPUtils.getExpressionContext(select);
         StringTokenizer st = new StringTokenizer(expContext, "'");
         if(st.countTokens() != 3) {
            return null;
         }
         st.nextToken();
         
         return getAttributeMapper().getDSAttribute((String)st.nextToken());

     }

     /**
      * Processes modify container values and returns key value/pair to be
      * modified.
      * @param select Select expression.
      * @param data list of new data objects.
      * @return Attribute key value pair for the given select.
      * @exception IDPPException.
      */
     public Map getDataMapForSelect(String select, List data) 
     throws IDPPException {
         IDPPUtils.debug.message("IDPPExtensionContainer.getDataMapForSelect:");
         Map map = new HashMap();
         if(select == null) {
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("IDPPExtensionContainer.getDataMapFor" +
               "Select: nullInput Paramters");
            }
            return map;
         }

         if(data == null || data.isEmpty()) {
            String attrName = getExtAttributeName(select);
            if(attrName != null) {
               map.put(attrName, new HashSet());
            }
            return map; 
         }

         Iterator iter = data.iterator();
         while(iter.hasNext()) {
             try {
                 PPISExtensionElement extension = 
                        (PPISExtensionElement)iter.next();
                 String attribute = getAttributeMapper().
                        getDSAttribute(extension.getName()); 
                 if(IDPPUtils.debug.messageEnabled()) {
                    IDPPUtils.debug.message("IDPPExtensionContainer.getData" +
                    "MapForSelect: Attribute name: " + attribute);
                 }
                 if(attribute == null) {
                    continue;
                 }
                 Set set = new HashSet();
                 set.add(extension.getValue());
                 map.put(attribute, set);
             } catch(Exception ce) {
                 IDPPUtils.debug.error("IDPPExtensionContainer.getDataMap" +
                 "ForSelect:", ce); 
                 throw new IDPPException(ce);
             }
         }
         if(IDPPUtils.debug.messageEnabled()) {
            IDPPUtils.debug.message("IDPPExtensionContainer.getDataMapFor"+
            "Select: Map to be extracted: " + map);
         }
         return map;
     }

     /**
      * Gets the PP ISExtension element.
      * @param attrName Extension attribute name.
      * @param attrValue Extension attribute value.
      * @exception IDPPException.
      */ 
     private PPISExtensionElement getISExtension(
         String attrName, String attrValue) throws IDPPException {
         IDPPUtils.debug.message("IDPPExtensionContainer.getISExtension:Init");
         try {
             com.sun.identity.liberty.ws.idpp.plugin.jaxb.ObjectFactory fac =
             new com.sun.identity.liberty.ws.idpp.plugin.jaxb.ObjectFactory();
             PPISExtensionElement ext = fac.createPPISExtensionElement();
             ext.setName(attrName);
             ext.setValue(attrValue);
             return ext;
         } catch (JAXBException je) {
             IDPPUtils.debug.error("IDPPExtensionContainer.getISExtension:" +
             "Fails in creating PP Extension element.", je);
             throw new IDPPException(IDPPUtils.bundle.getString("jaxbFailure"));
         }
     }
 
     /**
      * Checks if there are any binary attributes.
      */ 
     public boolean hasBinaryAttributes() {
         return false;
     }


}
