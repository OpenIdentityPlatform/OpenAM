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
 * $Id: AccountUtils.java,v 1.2 2008/06/25 05:47:45 qcheng Exp $
 *
 */

package com.sun.identity.saml2.common;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * This class <code>AccountUtils</code> is a utility class for
 * setting and retrieving the <code>SAML2<code> account federation information.
 */

public class AccountUtils {

    private static final String DELIM = "|";
    private static final String NAMEID_INFO_ATTRIBUTE = 
            "com.sun.identity.saml2.nameidinfo.attribute";
    private static final String NAMEID_INFO_KEY_ATTRIBUTE = 
            "com.sun.identity.saml2.nameidinfokey.attribute";
    static SAML2MetaManager metaManager = null;
    
    static {
        try {
            metaManager= new SAML2MetaManager();
        } catch (SAML2MetaException se) {
            SAML2Utils.debug.error("Unable to obtain Meta Manager.", se);
        }
    }

    /**
     * Returns the account federation information of a user for the given 
     * identity provider and a service provider. 
     * @param userID user id for which account federation needs to be returned.
     * @param hostEntityID <code>EntityID</code> of the hosted entity.
     * @param remoteEntityID <code>EntityID</code> of the remote entity.
     * @return the account federation info object.
     *         null if the account federation does not exist.
     * @exception SAML2Exception if account federation retrieval is failed.
     */ 
    public static NameIDInfo getAccountFederation(
           String userID, 
           String hostEntityID,
           String remoteEntityID) throws SAML2Exception {

        SAML2Utils.debug.message("AccountUtils.getAccountFederation:");

        if(userID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullUserID"));
        }

        if(hostEntityID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullHostEntityID"));
        }

        if(remoteEntityID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullRemoteEntityID"));
        }

        try {
            Set set = SAML2Utils.getDataStoreProvider().getAttribute(
                  userID, getNameIDInfoAttribute());

            if(set == null || set.isEmpty()) {
               if(SAML2Utils.debug.messageEnabled()) {
                  SAML2Utils.debug.message("AccountUtils.getAccount" +
                  "Federation : user does not have any account federations.");
               }
               return null;
            }
          
            String filter = hostEntityID + DELIM + remoteEntityID + DELIM;
            if(SAML2Utils.debug.messageEnabled()) {
               SAML2Utils.debug.message("AccountUtils.getAccountFederation: "+
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
               if(SAML2Utils.debug.messageEnabled()) {
                  SAML2Utils.debug.message("AccountUtils.getAccount" +
                  "Federation : user does not have account federation " +
                  " corresponding to =" + filter);
               }
               return null;
            }

            return NameIDInfo.parse(info);

        } catch (DataStoreProviderException dse) {

           SAML2Utils.debug.error("AccountUtils.readAccountFederation" +
           "Info: DataStoreProviderException", dse);
           throw new SAML2Exception(dse.getMessage());
        }
        
    }

    /**
     * Sets the account federation information to the datastore for a user.
     * @param info <code>NameIDInfo</code> object to be set.
     * @param userID user identifier for which the account federation to be set.
     * @exception SAML2Exception if any failure.
     */
    public static void setAccountFederation(
           NameIDInfo info, String userID) throws SAML2Exception {

        SAML2Utils.debug.message("AccountUtils.setAccountFederation:");

        if(info == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullNameIDInfo"));
        }

        if(userID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullUserID"));
        }

        NameIDInfoKey infoKey = new NameIDInfoKey(info.getNameIDValue(),
              info.getHostEntityID(), info.getRemoteEntityID());

        if(SAML2Utils.debug.messageEnabled()) {
           SAML2Utils.debug.message("AccountUtils.setAccountFederation: "+
           "info to be set:"+ info.toValueString() + ","  +
            "infoKey to be set:" + infoKey.toValueString());
        }

        String filter = info.getHostEntityID() + DELIM +
                info.getRemoteEntityID() + DELIM;

        try {
            String nameIDInfoAttr = getNameIDInfoAttribute();
            String nameIDInfoKeyAttr = getNameIDInfoKeyAttribute();
            Set set = new HashSet();
            set.add(nameIDInfoAttr);
            set.add(nameIDInfoKeyAttr);

            Map map = new HashMap();
            Map existMap = SAML2Utils.getDataStoreProvider().
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

            if(SAML2Utils.debug.messageEnabled()) {
               SAML2Utils.debug.message("AccountUtils.setAccountFederation: "+
               " set fedinfo " + map + " userID = " + userID);
            }

            SAML2Utils.getDataStoreProvider().setAttributes(userID, map);

        } catch (DataStoreProviderException dse) {
            SAML2Utils.debug.error("SAML2Utils.setAccountFederation: " +
            "DataStoreProviderException", dse);
            throw new SAML2Exception(dse.getMessage());
        }
    }

    /**
     * Removes the account federation of a user.
     * @param info <code>NameIDInfo</code> object. 
     * @param userID user identifie for which the account federation needs to
     *               be removed.
     * @return true if the account federation is removed successfully.
     * @exception SAML2Exception if any failure.
     */
    public static boolean removeAccountFederation(
        NameIDInfo info, String userID) throws SAML2Exception {

         SAML2Utils.debug.message("AccountUtils.removeAccountFederation:");
         if(info == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                  "nullNameIDInfo"));
         }

         if(userID == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                  "nullUserID"));
         }

         try {
             Set existingFed =  SAML2Utils.getDataStoreProvider().
                   getAttribute(userID, getNameIDInfoAttribute()); 
             Set existingInfoKey = SAML2Utils.getDataStoreProvider().
                   getAttribute(userID, getNameIDInfoKeyAttribute());

             if(existingFed == null || existingFed.isEmpty()) {
                if(SAML2Utils.debug.messageEnabled()) {
                   SAML2Utils.debug.message("AccountUtils.removeAccount" +
                   "Federation: user does not have account federation infos.");
                }
                return false;
             }

 
             String infoValue = info.toValueString();
             String infoKeyValue = info.getNameIDInfoKey().toValueString();

             if(SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("AccountUtils.removeAccount" +
                "Federation: info to be removed:"+ infoValue + "user="+ 
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
                SAML2Utils.getDataStoreProvider().setAttributes(userID, map);
                return true;
             }

             if(SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("AccountUtils.removeAccount" +
                "Federation: account federation info not found.");
             }
             return false;

         } catch (DataStoreProviderException dse) {
             SAML2Utils.debug.error("SAML2Utils.removeAccountFederation: " +
             "DataStoreProviderException", dse);
             throw new SAML2Exception(dse.getMessage());
         }
    }
    /**
     * Returns the SAML2 Name Identifier Info attribute name.
     * @return the SAML2 Name Identifier Info attribute name.
     */
    public static String getNameIDInfoAttribute() {
        return SystemPropertiesManager.get(NAMEID_INFO_ATTRIBUTE, 
           SAML2Constants.NAMEID_INFO);
    }

    /**
     * Returns the SAML2 Name Identifier InfoKey attribute name.
     * @return the SAML2 Name Identifier InfoKey attribute name.
     */
    public static String getNameIDInfoKeyAttribute() {
        return SystemPropertiesManager.get(NAMEID_INFO_KEY_ATTRIBUTE,
           SAML2Constants.NAMEID_INFO_KEY);
    }
}

