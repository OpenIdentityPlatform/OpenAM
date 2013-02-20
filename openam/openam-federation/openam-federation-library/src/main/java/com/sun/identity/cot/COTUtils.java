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
 * $Id: COTUtils.java,v 1.5 2008/08/06 17:26:14 exu Exp $
 *
 */

package com.sun.identity.cot;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.debug.Debug;
import java.util.logging.Level;

/**
 * This class contains circle of trust utilities.
 */
public class COTUtils {
    
    public static final String RESOURCE_BUNDLE_NAME = "libCOT";
    public static Debug debug = Debug.getInstance("libCOT");
    static String WSFED_DELIM = COTConstants.DELIMITER + COTConstants.WS_FED;
    static String SAML2_DELIM = COTConstants.DELIMITER + COTConstants.SAML2;
    static String IDFF_DELIM = COTConstants.DELIMITER + COTConstants.IDFF;
    
    /**
     * Default Constructor.
     */
    public COTUtils() {
    }
    
    /**
     * Get the first value of set by given key searching in the given map.
     * return null if <code>attrMap</code> is null or <code>key</code>
     * is null.
     *
     * @param attrMap Map of attributes name and their values 
     *                in the circle of trust service. The key
     *                is the attribute name and the value is
     *                a Set.
     * @param key the attribute name to be retrieved.
     * @return the first value of the attribute in the value set.
     */
    public static String getFirstEntry(Map attrMap, String key) {
        String retValue = null;
        
        if ((attrMap != null) && !attrMap.isEmpty()) {
            Set valueSet = (Set)attrMap.get(key);
            
            if ((valueSet != null) && !valueSet.isEmpty()) {
                retValue = (String)valueSet.iterator().next();
            }
        }
        
        return retValue;
    }
    
    /**
     * Adds a set of a given value to a map. Set will not be added if
     * <code>attrMap</code> is null or <code>value</code> is null or
     * <code>key</code> is null.
     *
     * @param attrMap Map of which set is to be added.
     * @param key Key of the entry to be added.
     * @param value Value to be added to the Set.
     */
    public static void fillEntriesInSet(Map attrMap, String key, String value) {
        if ((key != null) && (value != null) && (attrMap != null)) {
            Set valueSet = new HashSet();
            valueSet.add(value);
            attrMap.put(key, valueSet);
        }
    }
    
    /**
     * Checks if the federation protocol type is valid. The valid values
     * are IDFF or SAML2.
     *
     * @param protocolType the federation protocol type.
     * @return true if value is idff or saml2.
     */
    public static boolean isValidProtocolType(String protocolType) {       
        boolean isValid = ((protocolType != null) 
            && (protocolType.trim().length() > 0)
            && (protocolType.equalsIgnoreCase(COTConstants.IDFF)
            || protocolType.equalsIgnoreCase(COTConstants.SAML2)
            || protocolType.equalsIgnoreCase(COTConstants.WS_FED)));
        if (!isValid) {
            String[] data = { protocolType };
            LogUtil.error(Level.INFO,LogUtil.INVALID_COT_TYPE,data);
        }
        
        return isValid;
    }

    /**
     * Converts set of COT trusted providers to map.
     * Key of the map is the entity id name, value is a set of protocols the
     * entity supports.
     *
     * @param providerSet A set of trusted providers, each entry value could 
     *     contain both entity id and protocol in "|" separated format.
     * @param realm The realm the providers resides.
     * @return A map with entity id as key, protocols as value. 
     */ 
    static Map trustedProviderSetToEntityIDMap(Set providerSet, String realm) {
        if ((providerSet == null) || providerSet.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        Map map = new HashMap();
        Iterator it = providerSet.iterator();
        while (it.hasNext()) {
            String val = (String) it.next();
            if (debug.messageEnabled()) {
                debug.message("COTUtils.setToEntityIDMap: check " + val);
            } 
            if (val.endsWith(SAML2_DELIM)) {
                Set set = new HashSet();
                set.add(COTConstants.SAML2);
                map.put(val.substring(0, val.length() - SAML2_DELIM.length()),
                    set); 
            } else if (val.endsWith(IDFF_DELIM)) {
                Set set = new HashSet();
                set.add(COTConstants.IDFF);
                map.put(val.substring(0, val.length() - IDFF_DELIM.length()),
                    set); 
            } else if (val.endsWith(WSFED_DELIM)) { 
                Set set = new HashSet();
                set.add(COTConstants.WS_FED);
                map.put(val.substring(0, val.length() - WSFED_DELIM.length()),
                    set); 
            } else {
                // find out protocol for this provider
                Set protocolSet = findProtocolsForEntity(val, realm);
                if ((protocolSet != null) && !protocolSet.isEmpty()) {
                    map.put(val, protocolSet);
                }       
            }
        }
        if (debug.messageEnabled()) {
            debug.message("COTUtils.setToEntityIDMap: return " + map);
        } 
        return map;
    }

    
    /**
     * Converts set of COT trusted providers to map.
     * Key of the map is protocol name, value is a set of entity IDs which
     * speaks the protocol. Protocl name is one of COTConstants.WS_FED, 
     * COTConstants.SAML2 or COTConstants.IDFF.
     *
     * @param providerSet A set of trusted providers, each entry value could 
     *     contain both entity id and protocol in "|" separated format.
     * @param realm The realm the providers resides.
     * @return A map with protocol name as key, set of entity IDs as value. 
     */ 
    static Map trustedProviderSetToProtocolMap(Set providerSet, String realm) {
        if ((providerSet == null) || providerSet.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        Map map = new HashMap();
        Set wsfedSet = new HashSet();
        Set saml2Set = new HashSet();
        Set idffSet = new HashSet();
        Iterator it = providerSet.iterator();
        while (it.hasNext()) {
            String val = (String) it.next();
            if (debug.messageEnabled()) {
                debug.message("COTUtils.setToPrototolMap: check " + val);
            } 
            if (val.endsWith(SAML2_DELIM)) {
                saml2Set.add(
                    val.substring(0, val.length() - SAML2_DELIM.length()));
            } else if (val.endsWith(IDFF_DELIM)) {
                idffSet.add(
                    val.substring(0, val.length() - IDFF_DELIM.length())); 
            } else if (val.endsWith(WSFED_DELIM)) { 
                wsfedSet.add(
                    val.substring(0, val.length() - WSFED_DELIM.length())); 
            } else {
                // find out protocol for this provider
                Set protocolSet = findProtocolsForEntity(val, realm);
                if ((protocolSet != null) && !protocolSet.isEmpty()) {
                    Iterator pIt = protocolSet.iterator();
                    while (pIt.hasNext()) {
                        String proto = (String) pIt.next();
                        if (proto.equals(COTConstants.SAML2)) {
                            saml2Set.add(val);
                        } else if (proto.equals(COTConstants.IDFF)) {
                            idffSet.add(val);
                        } else if (proto.equals(COTConstants.WS_FED)) {
                            wsfedSet.add(val);
                        }
                    }
                }
            }
        }
        map.put(COTConstants.SAML2, saml2Set);
        map.put(COTConstants.IDFF, idffSet);
        map.put(COTConstants.WS_FED, wsfedSet);
        if (debug.messageEnabled()) {
            debug.message("COTUtils.setToProtocolMap: return " + map);
        } 
        return map;
    }
    
    /**
     * Returns set of protocol the entity supports.
     * @param entityId The ID of the entity to be checked.
     * @param realm the realm in which the entity resides.
     * @return Set of protocol the entity supports, values could be 
     *     <code>COTConstants.SAML2</code>, 
     *     <code>COTConstants.IDFF</code> or 
     *     <code>COTConstants.WS_FED</code>.
     */
    public static Set findProtocolsForEntity(String entityId, String realm) {
        try {
            Set retSet = new HashSet();
            CircleOfTrustManager manager = new CircleOfTrustManager();
            Set idffSet = manager.getAllEntities(realm, COTConstants.IDFF);
            if (idffSet.contains(entityId)) {
                retSet.add(COTConstants.IDFF);
            }
            Set saml2Set = manager.getAllEntities(realm, COTConstants.SAML2);
            if ((saml2Set != null) && saml2Set.contains(entityId)) {
                retSet.add(COTConstants.SAML2);
            }
            // TODO : hanlde WS-FED
            return retSet;
        } catch (COTException e) {
            debug.error("COTUtils.findProtocolsForEntity", e);
            return null;
        }
    }

    /**
     * Converts trusted provider protocol/entity IDs map to Set.
     * This method returns a Set with value in per entity and per protocol
     * format ("|" separated).
     *
     * @return A map with protocol name as key, set of entity IDs as value. 
     * @return A set of trusted providers with each entry value containing 
     *     both entity id and protocol in "|" separated format.
     */ 
    static Set trustedProviderProtocolMapToSet(Map providerMap) {
        if ((providerMap == null) || providerMap.isEmpty()) {
            return Collections.EMPTY_SET;
        }
        Set retSet = new HashSet();
        Set keys = providerMap.keySet();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String suffix = null;
            if (key.equals(COTConstants.SAML2)) {
                suffix = SAML2_DELIM;
            } else if (key.equals(COTConstants.IDFF)) {
                suffix = IDFF_DELIM;
            } else if (key.equals(COTConstants.WS_FED)) {
                suffix = WSFED_DELIM;
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("COTUtils.protocolMapToSet: " + 
                        "invalid protocol " + key);
                }
                continue;
            }
            Set vals = (Set) providerMap.get(key);
            Iterator it2 = vals.iterator();
            while (it2.hasNext()) {
                String val = (String) it2.next();
                retSet.add(val + suffix);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("COTUtils.protocolMapToSet: return" + retSet);
        }
        return retSet;
    }
}
