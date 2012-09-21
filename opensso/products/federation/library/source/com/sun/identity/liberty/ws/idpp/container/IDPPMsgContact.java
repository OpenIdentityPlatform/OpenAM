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
 * $Id: IDPPMsgContact.java,v 1.3 2008/06/25 05:47:16 qcheng Exp $
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

/**
 * This class <code>IDPPMsgContact</code> is an implementation of 
 * <code>IDPPContainer</code>.
 */ 

public class IDPPMsgContact extends IDPPBaseContainer {

     /** 
      * Constructor
      */
     public IDPPMsgContact() { 
        IDPPUtils.debug.message("IDPPMsgContact:constructor:init.");
     }

     /**
      * Gets the common name jaxb element 
      * @param userMap user map
      * @return InformalNameElement JAXB Object.
      * @exception IDPPException.
      */
     public Object getContainerObject(Map userMap) throws IDPPException {
        IDPPUtils.debug.message("IDPPMsgContact:getContainerObject:Init");
        try {
            PPType ppType = IDPPUtils.getIDPPFactory().createPPElement();
            Set msgContacts = (Set)userMap.get(
                getAttributeMapper().getDSAttribute(
                IDPPConstants.MSG_CONTACT_ELEMENT).toLowerCase());

            if(msgContacts == null || msgContacts.isEmpty()) {
               throw new IDPPException(
               IDPPUtils.bundle.getString("nullValues"));
            }

            Iterator iter = msgContacts.iterator();
            while(iter.hasNext()) {
               String msgContact = (String)iter.next();
               MsgContactElement mce = parseEntry(msgContact, userMap);
               if(mce != null) {
                  ppType.getMsgContact().add(mce);
               }
            }
            return ppType;

        } catch (JAXBException je) {
            IDPPUtils.debug.error(
            "IDPPMsgContact:getContainerObject: JAXB failure", je); 
            throw new IDPPException(
            IDPPUtils.bundle.getString("jaxbFailure"));
        }
     }

     /**
      * Parses the entry to MsgContactElement JAXB Obect.
      * @param entry MsgContact Entry
      * @userMap UserData Map.
      * @return MsgContactElement. 
      * @exception JAXBException.
      */
     private MsgContactElement parseEntry(String entry, Map userMap)
        throws JAXBException {

         if(entry == null || entry.length() == 0) {
            return null;
         }

         if(entry.indexOf(IDPPConstants.ATTRIBUTE_SEPARATOR) == -1) {
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("IDPPMsgContact.parsEntry: Invalid" +
               " Entry " + entry);
            }
            return null;
         }

         MsgContactElement mse = 
              IDPPUtils.getIDPPFactory().createMsgContactElement();

         StringTokenizer st = 
             new StringTokenizer(entry, IDPPConstants.ATTRIBUTE_SEPARATOR);

         while(st.hasMoreTokens()) {
            String token = st.nextToken();
            if(token.indexOf("=") == -1) {
               continue;
            }

            StringTokenizer tokenizer = new StringTokenizer(token, "=");
            if(tokenizer.countTokens() != 2) {
               continue;
            }

            String attribute = tokenizer.nextToken();
            String value = null;
            String mappedAttribute = 
                  getAttributeMapper().getDSAttribute(attribute); 

            if(mappedAttribute == null || mappedAttribute.equals(attribute)) {
               value = tokenizer.nextToken();
            } else {
               value = CollectionHelper.getMapAttr(userMap,
                   mappedAttribute.toLowerCase());
            }

            if(value == null) {
               continue;
            }

            if(attribute.equals("MsgType")) {
               mse.getMsgType().add(getDSTURI(value));
            } else if(attribute.equals("Nick")) {
               mse.setNick(getDSTString(value));
            } else if(attribute.equals("LComment")) {
               mse.setLComment(getDSTString(value));
            } else if(attribute.equals("MsgMethod")) {
               mse.getMsgMethod().add(getDSTURI(value));
            } else if(attribute.equals("MsgTechnology")) {
               mse.getMsgTechnology().add(getDSTURI(value));
            } else if(attribute.equals("MsgAccount")) {
               mse.setMsgAccount(getDSTString(value));
            } else if(attribute.equals("MsgSubAccount")) {
               mse.setMsgSubaccount(getDSTString(value));
            } else if(attribute.equals("MsgProvider")) {
               mse.setMsgProvider(getDSTString(value));
            } else if(attribute.equals("id")) {
               mse.setId(value);
            }
         } 
         return mse;
     } 

     /**
      * Gets required common name container attributes.
      * @return Set set of required container attributes
      */
     public Set getContainerAttributes() {
        IDPPUtils.debug.message("IDPPMsgContact:getContainerAttributes:Init");
        Set set = new HashSet();
        set.add(IDPPConstants.MSG_CONTACT_ELEMENT);
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
      * Process modify container values and returns key value/pair to be
      * modified.
      * @param select Select expression.
      * @param data list of new data objects.
      * @return Attribute key value pair for the given select.
      * @throws IDPPException.
      */
     public Map getDataMapForSelect(String select, List data) 
        throws IDPPException {

        IDPPUtils.debug.message("IDPPMsgContact:getDataMapForSelect:Init");

        String filter = getMsgFilter(select);

        if(filter == null) {
           throw new IDPPException(
           IDPPUtils.bundle.getString("invalidSelect"));
        }
       
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPMsgContact.getDataMapForSelect: " +
           "MsgContact filter: " + filter);
        }

        if(data == null || data.isEmpty()) {
           return getDataMap(null, filter);
        }

        if(data != null && !data.isEmpty()) {
           Iterator iter = data.iterator();

           while(iter.hasNext()) {
              Object dataElement = iter.next();
              if(dataElement instanceof MsgContactElement) {
                 MsgContactElement mse = (MsgContactElement)dataElement;
                 return getDataMap(mse, filter);
              } else {
                 throw new IDPPException(
                 IDPPUtils.bundle.getString("invalidElement"));
              }
           }

        }

        return null;

     }
       
     /**
      * Gets the data map from the requested data element.
      * @param mse Message contact element 
      * @param filter filter string 
      * @return data map
      */
     private Map getDataMap(MsgContactElement mse, String filter) {

         Map map = new HashMap();
         Set existingSet = getMsgContacts();
         Set newSet = new HashSet();
         if(existingSet.isEmpty()) {
            newSet.add(modifyEntry(null, mse)); 
         } else {
            Iterator iter = existingSet.iterator();
            while(iter.hasNext()) {
               String entry = (String)iter.next();
               if(entry.indexOf(filter) != -1) {
                  if(mse == null) {
                     continue;
                  }
                  entry = modifyEntry(entry, mse); 
                  newSet.add(entry);
               } else {
                  newSet.add(entry);
               }
            }
         }

         map.put(getAttributeMapper().getDSAttribute(
             IDPPConstants.MSG_CONTACT_ELEMENT), newSet);
         return map; 
     }

     /**
      * Gets the existing message contacts.
      * @return message contacts
      */
     private Set getMsgContacts() {
        try {
            Set set = new HashSet();
            String attrib = getAttributeMapper().getDSAttribute(
                  IDPPConstants.MSG_CONTACT_ELEMENT); 
            set.add(attrib);
            return (Set)IDPPUtils.getUserAttributes(userDN, set).get(attrib);
             
        } catch (IDPPException ie) {
            IDPPUtils.debug.error("IDPPMsgContact.getMsgContacts: Exception" +
            "while trying to get existing msg contacts.", ie);
            return new HashSet(); 
        }
     }

     /**
      * Modifies the existing entry.
      */
     private String modifyEntry(String entry, MsgContactElement mse) {

         StringBuffer sb = new StringBuffer(200);

         DSTString dstString = mse.getNick();
         if(dstString != null) {
            sb.append("Nick").append("=")
              .append(dstString.getValue()).append("|"); 
         }

         dstString = mse.getLComment();
         if(dstString != null) {
            sb.append("LComment").append("=")
              .append(dstString.getValue()).append("|"); 
         }

         dstString = mse.getMsgProvider();
         if(dstString != null) {
            sb.append("MsgProvider").append("=")
              .append(dstString.getValue()).append("|"); 
         }
       
         dstString = mse.getMsgAccount(); 
         if(dstString != null) {
            sb.append("MsgAccount").append("=")
              .append(dstString.getValue()).append("|"); 
         }

         dstString = mse.getMsgSubaccount();
         if(dstString != null) {
            sb.append("MsgSubAccount").append("=")
              .append(dstString.getValue()).append("|"); 
         }

         DSTURI dstURI = (DSTURI)mse.getMsgType().get(0);
         if(dstURI != null) {
            sb.append("MsgType").append("=")
              .append(dstURI.getValue()).append("|"); 
         }

         dstURI = (DSTURI)mse.getMsgMethod().get(0);
         if(dstURI != null) {
            sb.append("MsgMethod").append("=")
              .append(dstURI.getValue()).append("|"); 
         }

         dstURI = (DSTURI)mse.getMsgTechnology().get(0);
         if(dstURI != null) {
            sb.append("MsgTechnology").append("=")
              .append(dstURI.getValue()); 
         }

         String id = mse.getId();
         if(id != null) {
            sb.append("id").append("=").append(id); 
         }

         return sb.toString();
     }

     /**
      * Gets the MsgContact filter from a given a select expression.
      * @param select Select expression
      * @return message contact filter
      */
     private String getMsgFilter(String select) {

         if(IDPPUtils.debug.messageEnabled()) {
            IDPPUtils.debug.message("IDPPMsgContact.getMsgFilter: original" +
            " Expression = " + select);
         }

         int index1 = select.indexOf("\"");
         if(index1 == -1) {
            return null;
         }
         
         select = select.substring(index1+1, select.length());
         int index2 = select.indexOf("\"");
         if(index2 == -1) {
            return null;
         }

         return select.substring(0, index2);
     }

    /**
      * Checks if there are any binary attributes.
      */
     public boolean hasBinaryAttributes() {
         return false;
     }

}
