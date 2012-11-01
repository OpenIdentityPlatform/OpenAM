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
 * $Id: IDPPUtils.java,v 1.3 2008/08/06 17:28:09 exu Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.liberty.ws.idpp.common;

import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.security.SecureRandom;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.liberty.ws.idpp.jaxb.QueryElement;
import com.sun.identity.liberty.ws.idpp.jaxb.DSTString;
import com.sun.identity.liberty.ws.idpp.jaxb.QueryResponseElement;
import com.sun.identity.liberty.ws.idpp.jaxb.QueryResponseType;
import com.sun.identity.liberty.ws.idpp.jaxb.ObjectFactory;
import com.sun.identity.liberty.ws.idpp.jaxb.QueryType;
import com.sun.identity.liberty.ws.idpp.jaxb.ResourceIDElement;
import com.sun.identity.liberty.ws.interfaces.ResourceIDMapper;
import com.sun.identity.liberty.ws.idpp.*;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.xml.XMLUtils;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Class <code>IDPPUtils</code> is utility class used by IDPP service
 * for any common functionality in various modules.
 */

public class IDPPUtils {

    private static final String IDPP = "idpp";
    public static Debug debug = Debug.getInstance("libIDWSF");
    public static ResourceBundle bundle = 
        Locale.getInstallResourceBundle("libPersonalProfile");
    private static ObjectFactory idppFactory = new ObjectFactory();
     //This needs to change it as configurable.
    private static final String idppPrefix = "pp";
    private static HashMap idppElementTypes = new HashMap();
    public static HashMap idppIDSMap = new HashMap();
    private static DataStoreProvider userProvider = null;
    private static JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(
                IDPPConstants.IDPP_JAXB_PKG + ":" + 
                IDPPConstants.IDPP_PLUGIN_JAXB_PKG + ":" +
                IDPPConstants.XMLSIG_JAXB_PKG);
            getIDPPElementsMap();
            userProvider = DataStoreProviderManager.getInstance().
                getDataStoreProvider(IDPP);
        } catch (Exception e) {
            IDPPUtils.debug.error("IDPPCommonName:static initialization" +
                " Failed.", e);
        }
    }

    /**
     * Stores the IDPPElements map
     */
    private static void getIDPPElementsMap() {
         idppElementTypes.put(IDPPConstants.IDPP_ELEMENT, 
              new Integer(IDPPConstants.IDPP_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.INFORMAL_NAME_ELEMENT, 
              new Integer(IDPPConstants.INFORMAL_NAME_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.LINFORMAL_NAME_ELEMENT, 
              new Integer(IDPPConstants.LINFORMAL_NAME_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.COMMON_NAME_ELEMENT, 
              new Integer(IDPPConstants.COMMON_NAME_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.LEGAL_IDENTITY_ELEMENT, 
              new Integer(IDPPConstants.LEGAL_IDENTITY_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.EMPLOYMENT_IDENTITY_ELEMENT, 
              new Integer(IDPPConstants.EMPLOYMENT_IDENTITY_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.ADDRESS_CARD_ELEMENT, 
              new Integer(IDPPConstants.ADDRESS_CARD_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.MSG_CONTACT_ELEMENT, 
              new Integer(IDPPConstants.MSG_CONTACT_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.FACADE_ELEMENT, 
              new Integer(IDPPConstants.FACADE_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.DEMOGRAPHICS_ELEMENT, 
              new Integer(IDPPConstants.DEMOGRAPHICS_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.SIGN_KEY_ELEMENT, 
              new Integer(IDPPConstants.SIGN_KEY_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.ENCRYPT_KEY_ELEMENT, 
              new Integer(IDPPConstants.ENCRYPT_KEY_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.EMERGENCY_CONTACT_ELEMENT, 
             new Integer(IDPPConstants.EMERGENCY_CONTACT_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.LEMERGENCY_CONTACT_ELEMENT, 
             new Integer(IDPPConstants.LEMERGENCY_CONTACT_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.FN_ELEMENT, 
             new Integer(IDPPConstants.FN_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.SN_ELEMENT, 
             new Integer(IDPPConstants.SN_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.CN_ELEMENT, 
             new Integer(IDPPConstants.CN_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.MN_ELEMENT, 
             new Integer(IDPPConstants.MN_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.ALT_CN_ELEMENT, 
              new Integer(IDPPConstants.ALT_CN_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.PT_ELEMENT, 
              new Integer(IDPPConstants.PT_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.ANALYZED_NAME_ELEMENT, 
              new Integer(IDPPConstants.ANALYZED_NAME_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.INFORMAL_NAME_ELEMENT, 
              new Integer(IDPPConstants.INFORMAL_NAME_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.LEGAL_NAME_ELEMENT, 
              new Integer(IDPPConstants.LEGAL_NAME_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.DOB_ELEMENT, 
              new Integer(IDPPConstants.DOB_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.MARITAL_STATUS_ELEMENT, 
              new Integer(IDPPConstants.MARITAL_STATUS_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.GENDER_ELEMENT, 
              new Integer(IDPPConstants.GENDER_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.ALT_ID_ELEMENT, 
              new Integer(IDPPConstants.ALT_ID_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.ID_TYPE_ELEMENT, 
              new Integer(IDPPConstants.ID_TYPE_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.ID_VALUE_ELEMENT, 
              new Integer(IDPPConstants.ID_VALUE_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.VAT_ELEMENT, 
              new Integer(IDPPConstants.VAT_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.JOB_TITLE_ELEMENT, 
              new Integer(IDPPConstants.JOB_TITLE_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.O_ELEMENT, 
              new Integer(IDPPConstants.O_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.ALT_O_ELEMENT, 
              new Integer(IDPPConstants.ALT_O_ELEMENT_INT));
         idppElementTypes.put(IDPPConstants.EXTENSION_ELEMENT, 
              new Integer(IDPPConstants.EXTENSION_ELEMENT_INT));
     }

     //Default constructor
     public IDPPUtils() {
     }
    
     /**
      * Creates a Query Request element given a set of query expressions.
      * @param queryExpressions a list of query expressions
      * @param resourceID resource id.
      * @param includeCommonAttr include common attribute or not 
      * @return QueryElement JAXB object.
      */
     public static QueryElement createQueryElement(List queryExpressions, 
                                                   String resourceID,
                                                   boolean includeCommonAttr)
     throws JAXBException, IDPPException {
         QueryElement query = idppFactory.createQueryElement(); 
         if(queryExpressions == null || resourceID == null
            || queryExpressions.size() == 0) {
            debug.error("IDPPUtils:createQueryElement: Either query" +
            " expressions or resource id is null.");
            throw new IDPPException("ResourceID or query expressions are null");
         }
         query.setResourceID(createResourceIDElement(resourceID));
         query.setId(SAMLUtils.generateID());
         for (int i =0; i < queryExpressions.size(); i++) {
              QueryType.QueryItemType item = 
              idppFactory.createQueryTypeQueryItemType();
              item.setId(SAMLUtils.generateID()); 
              item.setIncludeCommonAttributes(includeCommonAttr);
              item.setItemID(SAMLUtils.generateID());
              item.setSelect(addIDPPPrefix((String)queryExpressions.get(i)));
              query.getQueryItem().add(item);
         }
         return query; 
     }

     /**
      * Gets the data element given a Query Response.
      * @param response QueryResponseElement  
      * @return List of data elements.
      */
     public static List getQueryDataElements(QueryResponseElement response)
      throws JAXBException, IDPPException {
         if(response == null) {
            debug.error("IDPPUtils:getQueryDataElements:response is null");
            throw new IDPPException("response is null");
         }
         return response.getData();
     }
     
     /**
      * Creates Discovery resource id type.
      * @param resourceID resource id string.
      * @return ResourceIDType JAXB object.
      */
     public static ResourceIDElement createResourceIDElement (String resourceID)
      throws JAXBException, IDPPException {
         if(resourceID == null) {
            debug.error("IDPPUtils:ResourceIDType: Resource id is null");
            throw new IDPPException("ResourceID is null");
         }
         ResourceIDElement resourceIDElement = 
               idppFactory.createResourceIDElement();
         resourceIDElement.setValue(resourceID);
         return resourceIDElement;
     }

     /**
      * Adds prefix "idpp" to the expression. This should be configurable
      * Also, need a name space prefix mapper.
      */
     private static String addIDPPPrefix(String expression) {
         if(expression == null || expression.length() == 0) {
            return expression;
         }
         if(expression.indexOf("/") == -1) {
            debug.error("IDPPUtils:addIDPPPrefix:Not a valid expression");
            return expression;
         }
         StringBuffer sb = new StringBuffer(100);
         StringTokenizer st = new StringTokenizer(expression, "/");
         while(st.hasMoreTokens()) {
              String temp = (String)st.nextToken();
              String prefixedStr = "/" + idppPrefix + ":" + temp;
              sb.append(prefixedStr);
         }
         return sb.toString();
     }

     /**
      * Looks for the expression context. For e.g. if the expression
      * is /idpp:IDPP/idpp:IDPPCommonName/idpp:AnalyzedName/idpp:FN, then
      * it one returns "FN".
      * @param select string.
      * @return context string.
      */
     public static String getExpressionContext(String select) {
        if(select == null || select.indexOf("/") == -1) {
           return null;
        }
        int i = select.lastIndexOf("/");
        if(i != -1) {
           select = select.substring(i+1);
        }
        if((i = select.indexOf(":")) == -1) {
           return select;
        } else {
          return select.substring(i+1);
        }
     }

    /**
     * Returns the user attribute values.
     * @param userDN user DN.
     * @param requiredSet a set of required attributes.
     * @return user attribute value map.
     */
    public static Map getUserAttributes(String userDN, Set requiredSet)
        throws IDPPException {

        try {
            return userProvider.getAttributes(userDN, requiredSet);
        } catch(DataStoreProviderException dspe) {
            debug.error("IDPPUtils:getUserAttributes: Error retrieving" +
                " user attributes.", dspe); 
            throw new IDPPException(dspe);
        }
    } 

    /**
     * Stores the user attribute values in the data store.
     * @param userDN user DN
     * @param map user attribute value map
     */
    public static void setUserAttributes(String userDN, Map map)
        throws IDPPException {

        try {
            userProvider.setAttributes(userDN, map);
        } catch(DataStoreProviderException dspe) {
            debug.error("IDPPUtils:setUserAttributes:Error while storing" +
                "user attributes.", dspe);
            throw new IDPPException(dspe);
        }
    }

     /**
      * Checks for if the user attribute values exist
      * for a given set.
      * @param userDN user DN.
      * @param set a set of user attributes that require check.
      * @return true if any of the user attibute values found.
      */
    public static boolean checkForUserAttributes(String userDN, Set set)
        throws IDPPException {

        try {
            Map map = userProvider.getAttributes(userDN, set);
            if ((map == null) || map.isEmpty()) {
                return false;
            }
            return true;
        } catch (DataStoreProviderException dspe) {
            debug.error("IDPPUtils:checkForUserAttributes:Error while " +
                "checking for user attributes.", dspe);
            throw new IDPPException(dspe);
        }
    }

     /**
      * Returns element type.
      * @param element a String representing an Element.
      * @return integer value of the element.
      */
     public static int getIDPPElementType(String element) {
         if(element == null) {
            debug.error("IDPPUtils:getIDPPElementType:InvalidElementType");
            return -1;
         }
         Integer elementType =  (Integer)idppElementTypes.get(element);
         if(elementType == null) {
            return -1;
         }
         return elementType.intValue();
     }

    /**
     * Checks is the user exists or not.
     * @param userDN user DN
     * @return true if the user exists.
     */
    public static boolean isUserExists(String userDN) {
        try {
            return userProvider.isUserExists(userDN);
        } catch (DataStoreProviderException dspe) {
            if (debug.messageEnabled()) {
                debug.message("IDPPUtils.isUserExists: Userentry is null",
                    dspe);
            }
            return false;
        }
    }

     /**
      * Gets the IDPPFactory JAXB Object.
      * @return ObjectFactory JAXB IDPP Factory Object.
      */
     public static ObjectFactory getIDPPFactory() {
         return idppFactory;
     }

     /**
      * Gets the marshaller
      * @return Marshaller JAXB Marshaller Object.
      */
     public static Marshaller getMarshaller() throws JAXBException {
         return jaxbContext.createMarshaller();
     }

     /**
      * Get the unmarshaller object.
      * @return Unmarshaller JAXB unmarshaller object.
      */
     public static Unmarshaller getUnmarshaller() throws JAXBException {
         return jaxbContext.createUnmarshaller();
     }

     /**
      * Gets the document builder.
      * @return DocumentBuilder dom document builder
      */
     public static DocumentBuilder getDocumentBuilder() {
        try {
            return XMLUtils.getSafeDocumentBuilder(false);
        } catch (ParserConfigurationException pce) {
            debug.error("Unable to initialize Document Builder", pce);
        }
        return null;
     }

     /**
      * Returns the resource expression from the select xpath expression.
      * @param select Xpath select expression.
      * @return String resource expression
      */
     public static String getResourceExpression(String select) {

         if(select == null || (select.indexOf("/") == -1)) {
            return select;
         }
 
         StringBuffer sb = new StringBuffer(100);
         StringTokenizer st = new StringTokenizer(select, "/");
         while(st.hasMoreTokens()) {
            String token = (String)st.nextToken();
            int index = token.indexOf("[");
            if(index != -1) {
               token = token.substring(0, index);
            }
            if(token == null) {
               continue;
            }
            index = token.indexOf(":");
            if(index != -1) {
               token = token.substring(index+1);
            }
            if(token == null) {
               continue;
            }
            sb.append("/").append(token);
         }
         return sb.toString();
         
     }

     /**
      * Gets the resource id for a given user id 
      * @param userID ID of a user
      * @return String Resource ID
      */
     public static String getResourceID(String userID) {
         IDPPServiceManager serviceManager = IDPPServiceManager.getInstance();
         ResourceIDMapper mapper = serviceManager.getResourceIDMapper();
         return mapper.getResourceID(serviceManager.getProviderID(), userID); 
     }

}
