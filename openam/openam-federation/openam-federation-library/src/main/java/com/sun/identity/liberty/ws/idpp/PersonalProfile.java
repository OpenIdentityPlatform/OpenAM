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
 * $Id: PersonalProfile.java,v 1.2 2008/06/25 05:47:14 qcheng Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */


package com.sun.identity.liberty.ws.idpp;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.bind.JAXBException;
import org.apache.xpath.XPathAPI;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Iterator;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.liberty.ws.dst.DSTQueryItem;
import com.sun.identity.liberty.ws.dst.DSTModification;
import com.sun.identity.liberty.ws.idpp.common.*;
import com.sun.identity.liberty.ws.idpp.container.*;
import com.sun.identity.liberty.ws.idpp.plugin.*;
import com.sun.identity.liberty.ws.interfaces.Authorizer;
import com.sun.identity.liberty.ws.interfaces.ResourceIDMapper;
import com.sun.identity.liberty.ws.dst.DSTConstants;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;

/**
 * This class <code>PersonalProfile</code> is an implementation of 
 * <code>LibertyDataService</code>. The default implementation of Personal 
 * Profile leverages the XPATH technology for the queries. Inorder to make 
 * sense for the XPath expressions, the user profile needs to be in XML blob.
 * The current approach is to parse the select expression, build a container
 * level XML blob instead of the entire user profile.
 * A WSC credential may not have enough priveleges to write or read the
 * user profile data since the policy evaluation is driven through Webservices
 * POLICY component rather than through ACI driven. So, currently, we use
 * admin token for both queries and updates, but the authorization check
 * will be done for a WSC credential. 
 */

public class PersonalProfile {

    private static final String SLASH = "/";
    private static final String LEFTBR= "[";
    private static final String COLON= ":";
    private static IDPPServiceManager serviceManager = null;

    static {
        try {
            serviceManager = IDPPServiceManager.getInstance();
        } catch( Exception ex) {
            IDPPUtils.debug.error("PersonalProfile:Initialization failed", ex);
        }
    }

      /**
       * Default constructor for personal profile.
       */
      public PersonalProfile() {
         IDPPUtils.debug.message("PersonalProfile:Init");
      }

     /**
      * Queries for the data for a specific resourceID.
      * @param credential credentials of the requesting WSC.
      * @param dstQueryItems list of DSTQueryItems.
      * @param request query DOM request. 
      * @param interactedData map for interacted data. This map will have the
      *                       key as the PP DS attribute, and the value as
      *                       it's value.
      * @return Map map of processed query items and the correspoding list 
      * of results.
      * @exception IDPPException.
      */
      public Map queryData(Object credential,
                          String resourceID, 
                          List dstQueryItems,
                          Map interactedData,
                          Document request) 
      throws IDPPException {

         IDPPUtils.debug.message("PersonalProfile: query init");
         if(credential == null || resourceID == null || 
            dstQueryItems == null || request == null) {
            IDPPUtils.debug.error("PersonalProfile:queryData: null input");
            throw new IDPPException(
            IDPPUtils.bundle.getString("nullInputParams"));
         }
         // validate the credentials of requesting WSC.
         boolean sessionValid = false;
         try {
             sessionValid = SessionManager.getProvider().isValid(credential);
         } catch (SessionException se) {
             IDPPUtils.debug.error("PersonalProfile:queryData:Invalid WSC"+
                 "credentials", se);
         }

         if (!sessionValid) {
             throw new IDPPException(IDPPUtils.bundle.getString(
                 "invalidWSCCredentials"));
         }

         String userDN = getUserDN(resourceID);
         if(userDN == null) {
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PersonalProfile: queryData:userDN" +
               "is null for a given resourceID.");
            }
            throw new IDPPException(IDPPUtils.bundle.getString("noResourceID"));
         }
         if(IDPPUtils.debug.messageEnabled()) {
            IDPPUtils.debug.message("PersonalProfile:queryData: userDN=" + 
            userDN);
         }

         // Get the User data from DS for all the given query items.
         Map userMap = null;
         try {
             userMap = getUserData(userDN, dstQueryItems);
         } catch(IDPPException ie) {
             IDPPUtils.debug.error("PersonalProfile:queryData:Error while"+
             "retrieving user data.", ie);
             throw new IDPPException(ie);
         }
         if(userMap == null || userMap.isEmpty()) {
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PersonalProfile:queryData:no data:" +
               "for the requested pp attributes.");
            }
            throw new IDPPException(IDPPUtils.bundle.getString("noData"));
         }

         if(interactedData != null && !interactedData.isEmpty()) {
           if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PersonalProfile.queryData(): " +
                                       " Contents of Interaction Map " +
                                       interactedData.toString());
           }
           userMap = updateUserDataMap(userMap, interactedData);
         }

         if(IDPPUtils.debug.messageEnabled()) {
            IDPPUtils.debug.message("PersonalProfile:queryData:requested Data "
            + userMap);
         }
         //Process each DSTQueryItem, apply Xpath.
         Map results = new HashMap();
         Iterator iter = dstQueryItems.iterator();
         while(iter.hasNext()) {
            DSTQueryItem item = (DSTQueryItem)iter.next();
            List queryResults = new ArrayList();
            String queryExpression = item.getSelect();
            String ppContainer = getContainerFromSelect(queryExpression);
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PersonalProfile:queryData: Container"+
               "processing:" + ppContainer);
            }
            if(ppContainer == null) {
               continue;
            }
            IDPPContainer container = getIDPPContainer(ppContainer, userDN);
            if(container == null) {
               continue;
            }

            Document xmlContainer = null;
            try {
                xmlContainer = container.toXMLDocument(userMap);
            } catch (IDPPException ie) {
                IDPPUtils.debug.error("PersonalProfile:queryData:Error while"+
                "converting container to an XML document.", ie);
                throw new IDPPException(ie);
            }
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PersonalProfile:queryData: Container"+
               "xml doc:" + XMLUtils.print(xmlContainer.getDocumentElement()));
            }

            Element element = request.getDocumentElement();
            element.setAttribute(IDPPConstants.XML_NS + 
            serviceManager.getPPExtensionPrefix(), IDPPConstants.PP_EXT_XML_NS);

            queryExpression = replacePrefix(queryExpression);
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PersonalProfile:queryData: query" +
               "expression before applying Xpath:" + queryExpression);
            }

            NodeList result = null;
            try {
                result = (NodeList)XPathAPI.selectNodeList(xmlContainer,
                    queryExpression , element);
            } catch (Exception ex) {
                IDPPUtils.debug.error("PersonalProfile.queryData:Invalid " +
                    "expression.", ex);
                continue;
            }

            if((result == null) || (result.getLength() == 0)) {
               IDPPUtils.debug.message("PersonalProfile.queryData:null result");
               continue;
            }

            for(int i = 0; i < result.getLength(); i++) {
                Node n = result.item(i);
                try {
                    queryResults.add(IDPPUtils.getUnmarshaller().unmarshal(n));
                } catch (JAXBException je) {
                    IDPPUtils.debug.error("PersonalProfile:queryData:JAXB" +
                    "Error while unmarshalling the results.", je); 
                    continue;
                }
            }
            results.put(item, queryResults);
         }
         return results;
      }

      /**
       * Replaces senders prefix with idpp prefix that's configured
       * in the service.
       * @param String select.
       * @return returns select string with configured idpp prefix.
       */
      private String replacePrefix(String select) {
         if(IDPPUtils.debug.messageEnabled()) {
            IDPPUtils.debug.message("PersonalProfile:replacePrefix:" +
            "Select =" + select);
         }
         if(select == null || select.indexOf(SLASH) == -1) {
            IDPPUtils.debug.error("PersonalProfile:replacePrefix:" +
            "Invalid expression.");
            return select;
         }
         StringBuffer sb = new StringBuffer(100);
         StringTokenizer st = new StringTokenizer(select, SLASH);
         while(st.hasMoreTokens()) {
               String temp = (String)st.nextToken();
               int i = temp.indexOf(COLON);
               if(i != -1) {
                  temp = temp.substring(i+1);
               }
               if(temp != null && 
                  temp.indexOf(IDPPConstants.PP_EXTENSION_ELEMENT) != -1){
                  sb.append(SLASH)
                    .append(serviceManager.getPPExtensionPrefix());
                  sb.append(COLON).append(temp);
               } else if(temp != null) {
                  sb.append(SLASH).append(serviceManager.getIDPPPrefix());
                  sb.append(COLON).append(temp);
               }
         }
         return sb.toString();
      }


      /**
       * This method parses the select expression and returns the
       * context of second level container queries. 
       * For e.g. a query expression is /idpp:IDPP/idpp:CommonName/CN, then 
       * this will return <CommonName> as a string so that the xml
       * blob can be constructed and XPath can be applied on top of it.
       */
      private String getContainerFromSelect(String selectExpression) {
           
          if(IDPPUtils.debug.messageEnabled()) {
             IDPPUtils.debug.message("PersonalProfile:getContainerFromSel:"
             + "Init: selectexpression: " + selectExpression);
          }
          StringTokenizer st = new StringTokenizer(selectExpression, SLASH);
          if(st == null) {
             if(IDPPUtils.debug.messageEnabled()) {
                IDPPUtils.debug.message("PersonalProfile:getContainerFrom "
                + "Invalid select expression.");
             }
             return selectExpression;
          }
          if (st.countTokens() == 1) {
              return IDPPConstants.IDPP_ELEMENT;
          }
          // Ignore the first token
          st.nextToken();
          String container = (String)st.nextToken();

          //Look for the xml qualifiers
          int i = container.indexOf(LEFTBR);
          if (i != -1) {
              container = container.substring(0, i);
          }
          if(container == null) {
             return selectExpression;
          }
          //Look for the name space qualifiers
          i = container.indexOf(COLON);
          if( i != -1) {
              container = container.substring(i+1, container.length());
          }
          return container;
      }
     
      /**
       * This method builds the XML blob for a specific container
       * to apply the XPath on it.
       * @param ppContainer PP container
       * @param userDN User DN.
       * @return DOM object of container values.
       */
      private IDPPContainer getIDPPContainer(
           String ppContainer, String userDN) {

          if(IDPPUtils.debug.messageEnabled()) {
             IDPPUtils.debug.message("PersonalProfile:getIDPPContainer:" +
             "Init: ContainerType: " + ppContainer);
          }
          if(ppContainer == null) {
             return null;
          }

          IDPPContainer container = null;
          Map containerClasses = serviceManager.getContainerClasses();

          if(containerClasses.containsKey(ppContainer)) { 
             container =  (IDPPContainer)containerClasses.get(ppContainer);
             container.setUserDN(userDN);
             return container;
          }
          
          int containerType = IDPPUtils.getIDPPElementType(ppContainer);

          switch(containerType) {
                case IDPPConstants.IDPP_ELEMENT_INT :
                     break;
                case IDPPConstants.COMMON_NAME_ELEMENT_INT :
                     container  = new IDPPCommonName();
                     break;
                case IDPPConstants.INFORMAL_NAME_ELEMENT_INT :
                     container  = new IDPPInformalName();
                     break;
                case IDPPConstants.LEGAL_IDENTITY_ELEMENT_INT :
                     container  = new IDPPLegalIdentity();
                     break;
                case IDPPConstants.EMPLOYMENT_IDENTITY_ELEMENT_INT :
                     container  = new IDPPEmploymentIdentity();
                     break;
                case IDPPConstants.SIGN_KEY_ELEMENT_INT :
                     container  = new IDPPSignKey();
                     break;
                case IDPPConstants.ENCRYPT_KEY_ELEMENT_INT :
                     container  = new IDPPEncryptKey();
                     break;
                case IDPPConstants.EXTENSION_ELEMENT_INT :
                     container  = new IDPPExtensionContainer();
                     break;
                case IDPPConstants.ADDRESS_CARD_ELEMENT_INT :
                     container  = new IDPPAddressCard();
                     break;
                case IDPPConstants.MSG_CONTACT_ELEMENT_INT :
                     container  = new IDPPMsgContact();
                     break;
                case IDPPConstants.FACADE_ELEMENT_INT :
                     container  = new IDPPFacade();
                     break;
                case IDPPConstants.DEMOGRAPHICS_ELEMENT_INT :
                     container  = new IDPPDemographics();
                     break;
                case IDPPConstants.EMERGENCY_CONTACT_ELEMENT_INT:
                     container  = new IDPPEmergencyContact();
                     break;
                default:
                     IDPPUtils.debug.error("PersonalProfile:getIDPPContainer:" +
                     "Invalid container type");
                     break;
          }
          container.setUserDN(userDN);
          return container;
      }

      /**
       * Gets the user data for given list of DST Query items.
       * @param String userDN.
       * @param List list of DSTQueryItems.
       * @return Map of user attribute value pairs.
       * @throws IDPPException.
       */
      private Map getUserData(String userDN, List dstQueryItems) 
      throws IDPPException {

         if(userDN == null || dstQueryItems == null) {
            throw new IDPPException(
            IDPPUtils.bundle.getString("nullInputParams"));
         }

         Map userMap = new HashMap();

         // Get all the required user attributes from all query items.
         Set querySet = new HashSet();
         Iterator iter = dstQueryItems.iterator();
         while(iter.hasNext()) {
            DSTQueryItem item = (DSTQueryItem)iter.next();
            String queryExpression = item.getSelect();
            String ppContainer = getContainerFromSelect(queryExpression);
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PersonalProfile:getUserData: Container"+
               "processing:" + ppContainer);
            }
            if(ppContainer != null) {
               IDPPContainer container = getIDPPContainer(ppContainer, userDN);
               if(container!= null) {
                  Set attrs = container.getContainerAttributes();
                  if(container.hasBinaryAttributes()) {
                     try {
                         Map tmpMap = IDPPUtils.getUserAttributes(userDN,
                             attrs);
                         addToMapWithLowerCaseKey(userMap, tmpMap);
                     } catch (Exception ex) {
                         IDPPUtils.debug.error("PersonalProfile.getUserData::"
                             + " Error in retrieving the data", ex);
                         throw new IDPPException(ex);
                     }
                     continue;
                  }
                  if(attrs != null && !attrs.isEmpty()) {
                     querySet.addAll(attrs);
                  }
               }
            }
         }
         if(IDPPUtils.debug.messageEnabled()) {
            IDPPUtils.debug.message("PersonalProfile:getUserData: Attributes"+
            " to be retrieved." + querySet);
         }

         // use admin token to get all the user attributes.
        if (querySet != null && !querySet.isEmpty()) {
            try {
                Map tmpMap = IDPPUtils.getUserAttributes(userDN, querySet);
                addToMapWithLowerCaseKey(userMap, tmpMap);
            } catch (Exception ex) {
                IDPPUtils.debug.error("PersonalProfile.getUserData::"
                    + " Error in retrieving the data", ex);
                throw new IDPPException (ex);
            }
        }
        return userMap;
    }

    /**
     * Processes modify request and update new data.
     * @param credential credential of a WSC.
     * @param resourceID resource id string
     * @param dstModifications list of DSTModification objects.
     * @param interactedData map for interacted data. This map will have the
     *                       key as the PP DS attribute, and the value as
     *                       it's value. 
     * @param request a Document object 
     * @return true if successful in modifying the data.
     * @exception IDPPException.
     */
      public boolean modifyData(Object credential,
                                String resourceID,
                                List dstModifications,
                                Map interactedData,
                                Document request)
      throws IDPPException {
 
         if(credential == null || resourceID == null || 
            dstModifications == null || dstModifications.isEmpty()) {
            //request is not being used in the case of modify, there for
            // the interface purposes. 
            IDPPUtils.debug.error("PersonalProfile:modifyData:null input");
            throw new IDPPException(
            IDPPUtils.bundle.getString("nullInputParamters"));
         }

         boolean sessionValid = false;
         try {
             sessionValid = SessionManager.getProvider().isValid(credential);
         } catch (SessionException se) {
             IDPPUtils.debug.error("PersonalProfile:modifyData:Invalid WSC"+
                 "credentials", se);
         }

         if (!sessionValid) {
             throw new IDPPException(IDPPUtils.bundle.getString(
                 "invalidWSCCredentials"));
         }

         String userDN = getUserDN(resourceID);
         if(userDN == null) {
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PersonalProfile: modifyData:userDN" +
               "is null for a given resourceID.");
            }
            throw new IDPPException(IDPPUtils.bundle.getString("noResourceID"));
         }
         if(IDPPUtils.debug.messageEnabled()) {
            IDPPUtils.debug.message("PersonalProfile:modifyData:userDN ="+
            userDN);
         }

         // Modifiable user map.
         Map modifyMap = new HashMap();
         Map binaryAttributeMap = new HashMap();
         Iterator iter = dstModifications.iterator();
         while(iter.hasNext()) {
            DSTModification modification = (DSTModification)iter.next(); 
            String select = modification.getSelect();
            boolean override = modification.isOverrideAllowed();
            List dataObject = modification.getNewDataValue(); 
            String containerContext = getContainerFromSelect(select);
            IDPPContainer container = 
                   getIDPPContainer(containerContext, userDN); 
            if(container == null) {
               if(IDPPUtils.debug.messageEnabled()) {
                  IDPPUtils.debug.message("PersonalProfile:modifyData:" +
                  "The given select expression is not in supported containers");
               }
               return false;
            }
            try {
                if (!override && IDPPUtils.checkForUserAttributes(userDN,
                    container.getContainerAttributesForSelect(select))) {
                  if(IDPPUtils.debug.messageEnabled()) {
                     IDPPUtils.debug.message("PersonalProfile:modifyData:" +
                     "override set to false and data Already exists.");
                  }
                  return false;
                }
                Map map = container.getDataMapForSelect(select, dataObject);
                if(container.hasBinaryAttributes()) {
                   binaryAttributeMap =  map;
                   continue;
                }
                if(map != null) {
                   modifyMap.putAll(map);
                }
                if(interactedData != null && !interactedData.isEmpty()) {
                   modifyMap = updateUserDataMap(modifyMap, interactedData);
                }
            } catch (IDPPException ie) {
                IDPPUtils.debug.error("PersonalProfile:modifyData: error while"
                 + "converting the data into a data map.", ie);
                return false;
            }
         }

        if (binaryAttributeMap != null && !binaryAttributeMap.isEmpty()) {
            try {
                IDPPUtils.setUserAttributes(userDN, binaryAttributeMap);
            } catch (Exception ie) {
                IDPPUtils.debug.error("PersonalProfile:modifyMap:Error while" +
                    "modifying the user data.", ie);            
                return false;
            }
        }

         if(modifyMap == null || modifyMap.isEmpty()) {
            IDPPUtils.debug.message("PersonalProfile:modifyData:map is null");
            if(!binaryAttributeMap.isEmpty()) {
               return true;
            } else {
               return false;
            }
         }

         if(IDPPUtils.debug.messageEnabled()) {
            IDPPUtils.debug.message("PersonalProfile:modifyData:data " +
            "to be modified"  + modifyMap);
         }

        try {
            IDPPUtils.setUserAttributes(userDN, modifyMap);
            return true;
        } catch (Exception ie) {
            IDPPUtils.debug.error("PersonalProfile:modifyMap:Error while" +
                "modifying the user data.", ie);            
            return false;
        }
    }

     /**
      * Checks if the select data is supported by the PP service.
      * @param select Select expression.
      * @return true if supported.
      */
     public boolean isSelectDataSupported(String select) {
        IDPPUtils.debug.message("PersonalProfile:isSelectDataSupported:Init"); 
        String container = getContainerFromSelect(select);
        int index = container.indexOf("[");
        if(index != -1) {
           container = container.substring(0, index);
        }
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("PersonalProfile.isSelectDataSupported: " +
           " Accessing container = " + container);
        }
        Set supportedContainers = serviceManager.getSupportedContainers();
        if(container == null || supportedContainers == null || 
           supportedContainers.isEmpty()) {
           return false;
        }
        if(supportedContainers.contains(container)) {
           return true;
        }
        return false;
     }

    /**
     * Checks if the resource id is valid.
     * @param resourceID resource id.
     * @return true if the resource id is valid.
     */
    public boolean isResourceIDValid(String resourceID) {
        IDPPUtils.debug.message("PersonalProfile:isResourceIDValid:Init");
        ResourceIDMapper resourceIDMapper= serviceManager.getResourceIDMapper();

        if(resourceIDMapper == null) {
           if(IDPPUtils.debug.warningEnabled()) {
              IDPPUtils.debug.warning("PersonalProfile.isResourceIDValid." +
               "unable to get resoureid mapper");
           }
           return false;
        }

        String userID = resourceIDMapper.getUserID(
               serviceManager.getProviderID(), resourceID);

        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("PersonalProfile.isResourceIDValid." 
               + userID);
        }

        return IDPPUtils.isUserExists(userID);
    }

     /**
      * Gets Authorization map for the list of select expressions.
      * @param credential credential object.
      * @param action request action query or modify
      * @param select resource being accessed
      * @param env Environment map that the policy could use
      * @return Authorization decision action.
      */
     public String getAuthZAction(Object credential,
                                  String action,
                                  String select,
                                  Map env)
     throws IDPPException {

         IDPPUtils.debug.message("PersonalProfile.getAuthorizationMap:Init");
         if(credential == null || action == null || select == null) {
            IDPPUtils.debug.error("PersonalProfile.getAuthZAction:null vals");
            throw new IDPPException(
            IDPPUtils.bundle.getString("nullInputParams"));
         }

         if(action.equals(DSTConstants.QUERY_ACTION) &&
            !serviceManager.isQueryPolicyEvalRequired()) {
            return IDPPConstants.AUTHZ_ALLOW;
         }

         if(action.equals(DSTConstants.MODIFY_ACTION) &&
            !serviceManager.isModifyPolicyEvalRequired()) {
            return IDPPConstants.AUTHZ_ALLOW;
         }
            
         try {
             Authorizer authorizer = serviceManager.getAuthorizer();
             return (String)authorizer.getAuthorizationDecision(
                credential, action, select, env);
         } catch (Exception e) {
             IDPPUtils.debug.error("PersonalProfile.getAuthZAction:" +
             "Exception while getting authorization info");
             throw new IDPPException(e); 
         }
     }

     /**
      * Gets the user dn for a specified resource id.
      * @param  resourceID resource id
      * @return String userDN.
      */
     public String getUserDN(String resourceID) {
        IDPPUtils.debug.message("PersonalProfile:getUserDN:Init");
        ResourceIDMapper resourceIDMapper =serviceManager.getResourceIDMapper();
        if(resourceIDMapper == null) {
           return null;
        }
        return resourceIDMapper.getUserID(
               serviceManager.getProviderID(), resourceID);
     }

     /**
      * Updates the user data map with interacted data map
      * @param userMap extracted or to be modified data map
      * @param interactedData Interacted data map
      * @return Map updated user data map
      */
     private Map updateUserDataMap(Map userMap, Map interactedData) {
         if(interactedData == null || userMap == null ||
            interactedData.isEmpty() || userMap.isEmpty()) {
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PersonalProfile.updateUserDataMap:"+
               "Interacted data or the user data map is empty");
            }
            return userMap;
         }
         Iterator iter = interactedData.keySet().iterator(); 
         while(iter.hasNext()) {
            String key = (String)iter.next();
            if(!userMap.containsKey(key.toLowerCase())) {
               if(IDPPUtils.debug.messageEnabled()) {
                  IDPPUtils.debug.message("PersonalProfile.updateUserDataMap"+
                  ":Interacted key " + key + " isnotPart of the query");
               }
               continue;
            }
            String attrValue = (String)interactedData.get(key);
            Set set = new HashSet();
            set.add(attrValue);
            userMap.put(key.toLowerCase(), set);
         }
         return userMap;
     }

     private void addToMapWithLowerCaseKey(Map dstMap, Map srcMap) {
         if (dstMap == null || srcMap == null || srcMap.isEmpty()) {
             return;
         }

         for(Iterator iter = srcMap.keySet().iterator(); iter.hasNext();) {
             String key = (String)iter.next();
             dstMap.put(key.toLowerCase(), srcMap.get(key));
         }
     }
}
