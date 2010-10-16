/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AccountUtils.java,v 1.2 2008/06/25 05:48:04 qcheng Exp $
 *
 */

package com.sun.identity.wsfederation.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.NameIDInfoKey;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * <code>AccountUtils</code> is a utility class for setting and retrieving 
 * the <code>WS-Federation<code> account federation information.
 */

public class AccountUtils {
    private static Debug debug = WSFederationUtils.debug;
    private static final String DELIM = "|";
    private static final String NAMEID_INFO_ATTRIBUTE = 
            "com.sun.identity.wsfederation.nameidinfo.attribute";
    private static final String NAMEID_INFO_KEY_ATTRIBUTE = 
            "com.sun.identity.wsfederation.nameidinfokey.attribute";

    /**
     * Returns the account federation information of a user for the given 
     * identity provider and a service provider. 
     * @param userID user id for which account federation needs to be returned.
     * @param hostEntityID <code>EntityID</code> of the hosted entity.
     * @param remoteEntityID <code>EntityID</code> of the remote entity.
     * @return the account federation info object.
     *         null if the account federation does not exist.
     * @exception WSFederationException if account federation retrieval failed.
     */ 
    public static NameIDInfo getAccountFederation(
           String userID, 
           String hostEntityID,
           String remoteEntityID) throws WSFederationException {
        String classMethod = "AccountUtils.getAccountFederation: ";

        if (debug.messageEnabled()){
            debug.message(classMethod);
        }

        if(userID == null) {
           throw new WSFederationException(WSFederationUtils.bundle.getString(
                 "nullUserID"));
        }

        if(hostEntityID == null) {
           throw new WSFederationException(WSFederationUtils.bundle.getString(
                 "nullHostEntityID"));
        }

        if(remoteEntityID == null) {
           throw new WSFederationException(WSFederationUtils.bundle.getString(
                 "nullRemoteEntityID"));
        }

        try {
            Set set = WSFederationUtils.dsProvider.getAttribute(
                  userID, getNameIDInfoAttribute());

            if(set == null || set.isEmpty()) {
               if(WSFederationUtils.debug.messageEnabled()) {
                  WSFederationUtils.debug.message(classMethod +
                  "user does not have any account federations.");
               }
               return null;
            }
          
            String filter = hostEntityID + DELIM + remoteEntityID + DELIM;
            if(WSFederationUtils.debug.messageEnabled()) {
               WSFederationUtils.debug.message(classMethod +
               " filter = " + filter + " userID = " + userID);
            }
            String info = null;

            for(Iterator iter = set.iterator(); iter.hasNext();) {
                String value = (String)iter.next();
                if(value.startsWith(filter)) {
                   info = value;
                   break;
                }
            }
 
            if(info == null) { 
               if(WSFederationUtils.debug.messageEnabled()) {
                  WSFederationUtils.debug.message(classMethod +
                  "user does not have account federation " +
                  " corresponding to =" + filter);
               }
               return null;
            }

            return NameIDInfo.parse(info);

        } catch (DataStoreProviderException dse) {
           WSFederationUtils.debug.error(classMethod +
           "Info: DataStoreProviderException", dse);
           throw new WSFederationException(dse);
        } catch (SAML2Exception se) {
           WSFederationUtils.debug.error(classMethod +
           "Info: SAML2Exception", se);
           throw new WSFederationException(se);
        }
        
    }

    /**
     * Sets the account federation information in the datastore for a user.
     * @param info <code>NameIDInfo</code> object to be set.
     * @param userID user identifier for which the account federation to be set.
     * @exception WSFederationException if any failure.
     */
    public static void setAccountFederation(
           NameIDInfo info, String userID) throws WSFederationException {
        String classMethod = "AccountUtils.setAccountFederation: ";

        WSFederationUtils.debug.message(classMethod);

        if(info == null) {
           throw new WSFederationException(WSFederationUtils.bundle.getString(
                 "nullNameIDInfo"));
        }

        if(userID == null) {
           throw new WSFederationException(WSFederationUtils.bundle.getString(
                 "nullUserID"));
        }

        try {
            NameIDInfoKey infoKey = new NameIDInfoKey(info.getNameIDValue(),
                  info.getHostEntityID(), info.getRemoteEntityID());

            if(WSFederationUtils.debug.messageEnabled()) {
               WSFederationUtils.debug.message(classMethod +
               "info to be set:"+ info.toValueString() + ","  +
                "infoKey to be set:" + infoKey.toValueString());
            }

            String filter = info.getHostEntityID() + DELIM +
                    info.getRemoteEntityID() + DELIM;

            String nameIDInfoAttr = getNameIDInfoAttribute();
            String nameIDInfoKeyAttr = getNameIDInfoKeyAttribute();
            Set set = new HashSet();
            set.add(nameIDInfoAttr);
            set.add(nameIDInfoKeyAttr);

            Map map = new HashMap();
            Map existMap = WSFederationUtils.dsProvider.
                  getAttributes(userID, set);

            if(existMap == null || existMap.isEmpty()) {
               Set set1 = new HashSet();
               set1.add(infoKey.toValueString());
               map.put(nameIDInfoKeyAttr, set1);

               Set set2= new HashSet();
               set2.add(info.toValueString());
               map.put(nameIDInfoAttr, set2);
            } else {

               Set set1 = (Set)existMap.get(nameIDInfoAttr);
               if(set1 != null) {
                  for(Iterator iter1 = set1.iterator(); iter1.hasNext();) {
                      String value = (String)iter1.next(); 
                      if(value.startsWith(filter)) {
                         iter1.remove();
                      }
                  }
               } else {
                  set1 = new HashSet();
               }

               set1.add(info.toValueString());
               map.put(nameIDInfoAttr, set1);

               Set set2 = (Set)existMap.get(nameIDInfoKeyAttr);
               if(set2 != null) {
                  for(Iterator iter2 = set2.iterator(); iter2.hasNext();) {
                      String value = (String)iter2.next(); 
                      if(value.startsWith(filter)) {
                         iter2.remove();
                      }
                  }
               } else {
                  set2 = new HashSet();
               }

               set2.add(infoKey.toValueString());
               map.put(nameIDInfoKeyAttr, set2);
            }

            if(WSFederationUtils.debug.messageEnabled()) {
               WSFederationUtils.debug.message(classMethod +
               " set fedinfo " + map + " userID = " + userID);
            }

            WSFederationUtils.dsProvider.setAttributes(userID, map);

        } catch (DataStoreProviderException dse) {
            WSFederationUtils.debug.error(classMethod +
            "DataStoreProviderException", dse);
            throw new WSFederationException(dse);
        } catch (SAML2Exception se) {
            WSFederationUtils.debug.error(classMethod +
            "SAML2Exception", se);
            throw new WSFederationException(se);
        }
    }

    /**
     * Removes the account federation of a user.
     * @param info <code>NameIDInfo</code> object. 
     * @param userID user identifier for which the account federation is to
     *               be removed.
     * @return true if the account federation is removed successfully.
     * @exception WSFederationException if any failure.
     */
    public static boolean removeAccountFederation(
        NameIDInfo info, String userID) throws WSFederationException {
        String classMethod = "AccountUtils.removeAccountFederation: ";

         WSFederationUtils.debug.message(classMethod);
         if(info == null) {
            throw new WSFederationException(WSFederationUtils.bundle.getString(
                  "nullNameIDInfo"));
         }

         if(userID == null) {
            throw new WSFederationException(WSFederationUtils.bundle.getString(
                  "nullUserID"));
         }

         try {
             Set existingFed =  WSFederationUtils.dsProvider.
                   getAttribute(userID, getNameIDInfoAttribute()); 
             Set existingInfoKey = WSFederationUtils.dsProvider.
                   getAttribute(userID, getNameIDInfoKeyAttribute());

             if(existingFed == null || existingFed.isEmpty()) {
                if(WSFederationUtils.debug.messageEnabled()) {
                   WSFederationUtils.debug.message(classMethod +
                   "user does not have account federation infos.");
                }
                return false;
             }

 
             String infoValue = info.toValueString();
             String infoKeyValue = info.getNameIDInfoKey().toValueString();

             if(WSFederationUtils.debug.messageEnabled()) {
                WSFederationUtils.debug.message(classMethod +
                "info to be removed:"+ infoValue + "user="+ 
                 userID + "infoKeyValue = " + infoKeyValue);
             }
             
             if(existingFed.contains(infoValue)) {

                existingFed.remove(infoValue);
                if(existingInfoKey != null &&
                       existingInfoKey.contains(infoKeyValue)) {
                   existingInfoKey.remove(infoKeyValue);
                }

                Map map = new HashMap();
                map.put(getNameIDInfoAttribute(), existingFed);
                map.put(getNameIDInfoKeyAttribute(), existingInfoKey);
                WSFederationUtils.dsProvider.setAttributes(userID, map);
                return true;
             }

             if(WSFederationUtils.debug.messageEnabled()) {
                WSFederationUtils.debug.message(classMethod +
                "account federation info not found.");
             }
             return false;

         } catch (DataStoreProviderException dse) {
             WSFederationUtils.debug.error(classMethod +
             "DataStoreProviderException", dse);
             throw new WSFederationException(dse);
         } catch (SAML2Exception se) {
             WSFederationUtils.debug.error(classMethod +
             "SAML2Exception", se);
             throw new WSFederationException(se);
         }
    }
    /**
     * Returns the WS-Federation Name Identifier Info attribute name.
     * @return the WS-Federation Name Identifier Info attribute name.
     */
    public static String getNameIDInfoAttribute() {
        return SystemPropertiesManager.get(NAMEID_INFO_ATTRIBUTE, 
           WSFederationConstants.NAMEID_INFO);
    }

    /**
     * Returns the WS-Federation Name Identifier InfoKey attribute name.
     * @return the WS-Federation Name Identifier InfoKey attribute name.
     */
    public static String getNameIDInfoKeyAttribute() {
        return SystemPropertiesManager.get(NAMEID_INFO_KEY_ATTRIBUTE,
           WSFederationConstants.NAMEID_INFO_KEY);
    }
}

