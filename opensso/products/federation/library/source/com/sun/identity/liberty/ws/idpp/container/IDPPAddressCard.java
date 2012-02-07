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
 * $Id: IDPPAddressCard.java,v 1.2 2008/06/25 05:47:15 qcheng Exp $
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
import java.util.StringTokenizer;
import org.w3c.dom.Document;
import com.sun.identity.liberty.ws.idpp.common.*;
import com.sun.identity.liberty.ws.idpp.jaxb.*;
import com.sun.identity.liberty.ws.idpp.plugin.*;


/**
 * This class <code>IDPPAddressCard</code> is an implementation of 
 * <code>IDPPContainer</code>.
 */ 

public class IDPPAddressCard extends IDPPBaseContainer {

     private static final String REMOVE_CARD = "RemoveCard";

     /**
      * Constructor
      */
     public IDPPAddressCard() { 
         IDPPUtils.debug.message("IDPPAddressCard:constructor:init.");
     }

     /**
      * Gets the container object i.e. LegalIdentity JAXB Object 
      * @param userMap user map
      * @return LegalIdentityElement JAXB Object.
      * @exception IDPPException.
      */
     public Object getContainerObject(Map userMap) throws IDPPException {

         IDPPUtils.debug.message("IDPPAddressCard:getContainerObject:Init");
         try {
             PPType ppType = IDPPUtils.getIDPPFactory().createPPElement();

             Set addressCards = (Set)userMap.get(
                  getAttributeMapper().getDSAttribute(
                  IDPPConstants.ADDRESS_CARD_ELEMENT).toLowerCase());

             if(addressCards == null || addressCards.isEmpty()) {
                throw new IDPPException(
                IDPPUtils.bundle.getString("nullValues"));
             }

             Iterator iter = addressCards.iterator();
             while(iter.hasNext()) {
                String addrCard = (String)iter.next();
                AddressCardElement ace = parseEntry(addrCard, userMap);
                if(ace != null) {
                   ppType.getAddressCard().add(ace);
                }
             }

             return ppType;
         } catch (JAXBException je) {
             IDPPUtils.debug.error(
              "IDPPContainers:getContainerObject: JAXB failure", je); 
              throw new IDPPException(
              IDPPUtils.bundle.getString("jaxbFailure"));
         }
     }

    /**
     * Parses the entry and creates an address card element from the given map
     */
     private AddressCardElement parseEntry(String entry, Map userMap)
       throws JAXBException {

         if(entry == null || entry.length() == 0) {
            return null;
         }

         if(entry.indexOf(IDPPConstants.ATTRIBUTE_SEPARATOR) == -1) {
            IDPPUtils.debug.error("IDPPAddressCard.parsEntry: Invalid" +
               " Entry " + entry);
            return null;
         }

         AddressCardElement ace =
            IDPPUtils.getIDPPFactory().createAddressCardElement();

         StringTokenizer st = new StringTokenizer(entry, 
              IDPPConstants.ATTRIBUTE_SEPARATOR);

         String addrType = null;
         String nick = null;
         String lComment = null;
         String postalAddress = null;
         String postalCode = null;
         String city = null;
         String state = null;
         String country = null;
         String id = null;

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

            if(attribute.equals("AddrType")) {
               addrType = value;
            } else if(attribute.equals("Nick")) {
               nick = value;
            } else if(attribute.equals("LComment")) {
               lComment = value;
            } else if(attribute.equals("PostalAddress")) {
               postalAddress = value;
            } else if(attribute.equals("PostalCode")) {
               postalCode = value;
            } else if(attribute.equals("L")) {
               city = value;
            } else if(attribute.equals("C")) {
               country = value;
            } else if(attribute.equals("St")) {
               state = value;
            } else if(attribute.equals("id")) {
               id = value;
            } 
         }
  
         if(addrType == null) {
            IDPPUtils.debug.error("IDPPAdressCard.parseEntry: Invalid entry" +
            " has no AddrType " + entry);
            return null;
         }

         AddressType ae = IDPPUtils.getIDPPFactory().createAddressElement();
         ae.setC(getDSTString(country));
         ae.setSt(getDSTString(state));
         ae.setL(getDSTString(city));
         ae.setPostalAddress(getDSTString(postalAddress));
         ae.setPostalCode(getDSTString(postalCode));

         ace.setNick(getDSTString(nick));
         ace.getAddrType().add(getDSTURI(addrType));
         ace.setAddress(ae);
         ace.setLComment(getDSTString(lComment));
         ace.setId(id);

         return ace;
     }

     /**
      * Gets required common name container attributes.
      *  
      * @return Set set of required container attributes 
      */
     public Set getContainerAttributes() {
         IDPPUtils.debug.message("IDPPAddressCard:getContainerAttrib:Init");
         Set set = new HashSet();
         set.add(IDPPConstants.ADDRESS_CARD_ELEMENT);
         return getMapperAttributeSet(set);
     }

     /**
      * Sets the container attributes for a given select expression.
      * @param select Select Expression.
      * @return Set set of required user attributes.
      */
     public Set getContainerAttributesForSelect(String select) {
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPAddressCard:getContainerAttributes" +
           "ForSelect:Init");
        }
        return getContainerAttributes();
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

        IDPPUtils.debug.message("IDPPAddressCard:getDataMapForSelect:Init");
        String expContext = IDPPUtils.getExpressionContext(select);

        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPAddressCard:getDataMapForSelect:" +
           "exp context = " + expContext);
        }

        Map map = new HashMap();
        if(expContext == null || expContext.length() == 0) {
           return map ; 
        }

        if(data == null || data.isEmpty()) {
           return getDataMap(expContext, null);
        } else {
           Iterator iter = data.iterator();
           while(iter.hasNext()) {
              Object dataElement = iter.next();
              Map tmpMap = getDataMap(expContext, dataElement);
              if(tmpMap != null) {
                 map.putAll(tmpMap);
              }
           }
        }
        return map;
        
    }

    /**
     * Gets the modifiable map for the expression context from the given
     * data element.
     * @param expContext Given select expression context.
     * @param dataElement DataElement. 
     * @return Map Modifiable attribute value pair.
     * @exception IDPPException.
     */
    private Map getDataMap(String expContext, Object dataElement)
       throws IDPPException {

        String addressType = null;
        String entry = null;
        Map addressMap = getAddressMap();

        if(expContext.indexOf("[") != -1) {
           addressType = getAddressType(expContext); 
           if(IDPPUtils.debug.messageEnabled()) {
              IDPPUtils.debug.message("IDPPAddressCard.getDataMap: " +
              "AddressType=" + addressType); 
           }
           if(addressType != null) {
             entry = (String)addressMap.get(addressType);
           }
        }

        if(expContext.startsWith("AddressCard")) {
           if(dataElement == null) {
              if(addressType == null) {
                // There is no data and no specific address type, so remove all
                 Map removeMap = new HashMap(1);
                 removeMap.put(getAttributeMapper().getDSAttribute(
                 IDPPConstants.ADDRESS_CARD_ELEMENT), new HashSet()); 
                 return removeMap;
              } else {
                 Map tmpMap = new HashMap(1);
                 tmpMap.put(addressType, REMOVE_CARD);
                 return setAddressMap(tmpMap);
              } 
           } else if(dataElement instanceof AddressCardElement) {
              AddressCardElement addr = (AddressCardElement)dataElement;
              if(addressType == null || addressType.length() == 0) {
                 List list = addr.getAddrType();
                 if(list != null && list.size() != 0) {
                    DSTURI addressURI = (DSTURI)list.get(0);
                    addressType = addressURI.getValue(); 
                 }
              }
              if(IDPPUtils.debug.messageEnabled()) {
                 IDPPUtils.debug.message("IDPPAddressCard.getDataMap: " +
                 "AddressType= " + addressType); 
              }
              entry = (String)addressMap.get(addressType);
              if(entry == null) {
                 entry = createAddressCard(addr, addressType);
              } else {
                 entry = modifyAddressCard(entry, addr);
              }

              if(entry == null) {
                 throw new IDPPException(
                 IDPPUtils.bundle.getString("nullValues"));
              }

              if(IDPPUtils.debug.messageEnabled()) {
                 IDPPUtils.debug.message("IDPPAddressCard.getDataMap: Entry" +
                 " to be modified." + entry);
              }

              Map tmpMap = new HashMap(1);
              tmpMap.put(addressType, entry); 
              return setAddressMap(tmpMap);

           } else {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalidElement")); 
           } 

        } else if(expContext.equals("Nick")
            || expContext.equals("PostalAddress") 
            || expContext.equals("LComment")
            || expContext.equals("L") 
            || expContext.equals("St")
            || expContext.equals("C")
            || expContext.equals("PostalCode") ) {

           if(dataElement == null) {
              entry = modifyEntry(entry, expContext, null);

           } else if(dataElement instanceof DSTString) {
              DSTString dstString = (DSTString)dataElement;
              entry =  modifyEntry(entry, expContext, dstString);

           } else {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalidElement")); 
           }
        } else if(expContext.equals("Address")) {

           if(dataElement == null) {
              entry = modifyAddress(entry, null);

           } else if(dataElement instanceof AddressElement) {
              AddressElement ae = (AddressElement)dataElement;
              entry = modifyAddress(entry, ae);

           } else {
              throw new IDPPException(
              IDPPUtils.bundle.getString("invalidElement")); 
           }
        }

        Map tmpMap = new HashMap(1);
        tmpMap.put(addressType, entry);

        return setAddressMap(tmpMap);
     }

     /**
      * Modifies the entry in the existing map with a given value.
      */
     private String modifyEntry(String entry, 
            String element, DSTString dstString) {

        StringBuffer sb = new StringBuffer(100);
        StringTokenizer st = new StringTokenizer(entry, "|");

        while(st.hasMoreTokens()) {
           String token = st.nextToken();
           StringTokenizer tokenizer = new StringTokenizer(token, "=");
           String newToken = tokenizer.nextToken();
           if(element.equals(newToken)) {
              String value = null;
              if(dstString != null) {
                 value = dstString.getValue();
              }
              if(value != null) {
                 sb.append(element)
                    .append("=").append(dstString.getValue()).append("|");
              }
           } else {
              sb.append(token).append("|");
           }
        }
        return sb.toString();
     }

     /**
      * Create a new address card.
      */
     private String createAddressCard(AddressCardElement ace, 
                 String addressType) {

        StringBuffer sb = new StringBuffer();
        sb.append("AddrType").append("=").append(addressType).append("|");

        AddressType ae = ace.getAddress(); 
        if(ae == null) {
           IDPPUtils.debug.error("IDPPAddressContainer.createAddressCard:" +
            "Address Element is null");
            return null;
        }

        String address = createAddress(ae);
        if(address != null) {
           sb.append(address);
        }

        DSTString nickName = ace.getNick();
        if(nickName != null) {
           sb.append("Nick=").append(nickName.getValue()).append("|");
        }

        DSTString comment = ace.getLComment();
        if(comment != null) {
           sb.append("LComment=").append(comment.getValue());
        }

        return sb.toString();

     }

     /**
      * Creates an address
      */
     private String createAddress(AddressType ae) {

         StringBuffer sb = new StringBuffer();

         DSTString postalAddress = ae.getPostalAddress();
         if(postalAddress != null) {
            sb.append("PostalAddress=").append(postalAddress.getValue())
           .append("|");
         }

         DSTString city = ae.getL();
         if(city != null) {
            sb.append("L=").append(city.getValue()).append("|");
         }

         DSTString state = ae.getSt();
         if(state != null) {
            sb.append("St=").append(state.getValue()).append("|");
         }

         DSTString postalCode = ae.getPostalCode();
         if(postalCode != null) {
            sb.append("PostalCode=").append(postalCode.getValue()).append("|");
         }

         DSTString country = ae.getC();
         if(country != null) {
            sb.append("C=").append(country.getValue()).append("|");
         }

         return sb.toString(); 
     }

     /**
      * Modifies the address entry.
      */
     private String modifyAddress(String entry, AddressElement ae) {

        StringBuffer sb = new StringBuffer(100);
        StringTokenizer st = new StringTokenizer(entry, "|");
        while(st.hasMoreTokens()) {
           String token = st.nextToken();   
           if(token.startsWith("PostalAddress")) {
              if(ae == null) {
                 continue;
              }
              DSTString postalAddress = ae.getPostalAddress();
              if(postalAddress != null) {
                 sb.append("PostalAddress")
                   .append("=").append(postalAddress.getValue()).append("|");
              } else {
                 sb.append(token).append("|");
              }
           } else if(token.startsWith("PostalCode")) {
              if(ae == null) {
                 continue;
              }
              DSTString postalCode = ae.getPostalCode();
              if(postalCode != null) {
                 sb.append("PostalCode")
                   .append("=").append(postalCode.getValue()).append("|");
              } else {
                 sb.append(token).append("|");
              }
           } else if (token.startsWith("L")) {
              if(ae == null) {
                 continue;
              }
              DSTString city = ae.getL();
              if(city != null) {
                 sb.append("L")
                   .append("=").append(city.getValue()).append("|");
              } else {
                 sb.append(token).append("|");
              }
           } else if(token.startsWith("St")) {
              if(ae == null) {
                 continue;
              }
              DSTString state = ae.getSt();
              if(state != null) {
                 sb.append("St")
                   .append("=").append(state.getValue()).append("|");
              } else {
                 sb.append(token).append("|");
              }

           } else if(token.startsWith("C")) {
              if(ae == null) {
                 continue;
              }
              DSTString country = ae.getC();
              if(country != null) {
                 sb.append("C")
                   .append("=").append(country.getValue()).append("|");
              } else {
                 sb.append(token).append("|");
              }
           } else {
              sb.append(token).append("|");
           }
        }
        return sb.toString();
 
     }

     /**
      * Modifies the address card entry.
      */
     private String modifyAddressCard(String entry, AddressCardElement ace) {

         StringBuffer sb = new StringBuffer(100);

         AddressElement ae = (AddressElement)ace.getAddress();
         String address = modifyAddress(entry, ae);
         StringTokenizer st = new StringTokenizer(address, "|");

         while(st.hasMoreTokens()) {

            String token = st.nextToken();
            if(token.startsWith("Nick")) {
               DSTString nick = ace.getNick();
               if(nick != null) {
                  sb.append("Nick")
                    .append("=").append(nick.getValue()).append("|");
               } else {
                  sb.append(token).append("|");
               }
            } else if(token.startsWith("LComment")) {
               DSTString lComment = ace.getLComment();
               if(lComment != null) {
                  sb.append("LComment")
                    .append("=").append(lComment.getValue()).append("|");
               } else {
                  sb.append(token).append("|");
               }
            } else if(token.startsWith("id")) {
               String id = ace.getId(); 
               if(id != null) {
                  sb.append("id").append("=").append(id).append("|");
               }
            } else {
               sb.append(token).append("|");
            }
         }

         return sb.toString();
     }


     /**
      * Gets the address map for existing entries.
      */
     private Map getAddressMap() {
        Set set = new HashSet();
        set.add(getAttributeMapper().getDSAttribute(
            IDPPConstants.ADDRESS_CARD_ELEMENT));

        Map map = null;
        try {
            map = IDPPUtils.getUserAttributes(userDN, set);
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("IDPPAddressCard.getAddressMap: " +
               "map"+ map);
            }
        } catch (IDPPException ie) {
            IDPPUtils.debug.error("IDPPAddressCard.getAddressMap: Error", ie);
            return null;
        }
 
        Set values = (Set)map.get(getAttributeMapper().getDSAttribute(
            IDPPConstants.ADDRESS_CARD_ELEMENT).toLowerCase());

        Map addressMap = new HashMap();
        Iterator iter = values.iterator();
        while(iter.hasNext()) {
           String value = (String)iter.next();
           addressMap.put(getAddressType(value), value); 
        }
        
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPAddressCard.getAddressMap: "  +
           "address map " + addressMap);
        }

        return addressMap;
     }

     /**
      * Sets the address entries using new modifiable map.
      */
     private Map setAddressMap(Map modifyMap) {

        Map existingMap = getAddressMap();
        Set keys = existingMap.keySet();
        Set set = new HashSet();

        if(keys != null &&  !keys.isEmpty()) {
           
           Iterator iter = keys.iterator();

           while(iter.hasNext()) {
              String key = (String)iter.next();
              if(modifyMap.containsKey(key)) {
                 String value = (String)modifyMap.get(key);

                 if(value.equals(REMOVE_CARD)) {
                    existingMap.remove(key);
                 } else {
                    set.add(value);
                 }

              } else {
                 set.add((String)existingMap.get(key));
              }
           }

        } else {
           set.addAll(modifyMap.values()); 
           
        }
        Map map = new HashMap();
        map.put(getAttributeMapper().getDSAttribute(
        IDPPConstants.ADDRESS_CARD_ELEMENT), set);
        return map;
     }

     /**
      * Gets the address type from the entry or select expression.
      */
     private String getAddressType(String value) {

        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("IDPPAddressCard.getAddressType: " +
           " value=" + value);
        }

        if(value.indexOf("|") != -1) {
           StringTokenizer st = new StringTokenizer(value, "|");
           while(st.hasMoreTokens()) {
              String token = st.nextToken();
              if(token.startsWith("AddrType")) { 
                 StringTokenizer tokenizer = new StringTokenizer(token, "=");
                 tokenizer.nextToken();
                 return tokenizer.nextToken();
              }
           }
           return null;
        }

       
        int index = value.indexOf("AddrType");
        if(index != -1) {
           value = value.substring(index + 9);
           int index2 = value.indexOf("\"");
           if (index2 != -1) {
               value = value.substring(index2+1, value.length());
               value = value.substring(0, value.indexOf("\""));
               if(IDPPUtils.debug.messageEnabled()) {
                  IDPPUtils.debug.message("IDPPAddressCard.getAddressType: " +
                  "address type:" + value);
               }
               return value;
           }
        }
        return null; 
     }


     /**
      * Checks if there are any binary attributes.
      * 
      * @return false
      */
     public boolean hasBinaryAttributes() {
         return false;
     }


}
