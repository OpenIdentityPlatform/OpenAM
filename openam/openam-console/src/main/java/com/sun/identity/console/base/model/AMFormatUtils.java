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
 * $Id: AMFormatUtils.java,v 1.5 2009/01/28 05:34:56 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.base.model;

import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.sm.SMSEntry;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.LDAPDN;

/**
 * <code>AMFormatUtils</code> provides a set of formating methods
 */
public class AMFormatUtils
    implements AMAdminConstants
{
    /**
     * Sorts items in a set.
     *
     * @param collection to sort.
     * @param locale of user.
     * @return list of sorted items.
     */
    public static List sortItems(Collection collection, Locale locale) {
        List sorted = Collections.EMPTY_LIST;

        if ((collection != null) && !collection.isEmpty()) {
            sorted = new ArrayList(collection);
            Collator collator = Collator.getInstance(locale);
            Collections.sort(sorted, collator);
        }
        return sorted;
    }

    /**
     * Reverses key to value String-String map i.e. key of map becomes
     * the value; and value becomes key for each entry.
     *
     * @param map Map to reverse.
     * @return reversed map.
     */
    public static Map reverseStringMap(Map map) {
        Map mapReverse = Collections.EMPTY_MAP;

        if ((map != null) && !map.isEmpty()) {
            mapReverse = new HashMap(map.size() *2);
            for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
                String key = (String)iter.next();
                mapReverse.put(map.get(key), key);
            }
        }
        return mapReverse;
    }

    /**
     * Sorts keys in a map.
     *
     * @param map to sort.
     * @param locale of user.
     * @return a list of sorted keys.
     */
    public static List sortKeyInMap(Map map, Locale locale) {
        List sorted = Collections.EMPTY_LIST;
        if ((map != null) && !map.isEmpty()) {
            sorted = sortItems(map.keySet(), locale);
        }
        return sorted;
    }

    /**
     * Returns an option list of sorted label.
     *
     * @param map Map of value to its label.
     * @param locale Locale of user.
     * @return an option list of sorted label.
     */
    public static OptionList getSortedOptionList(Map map, Locale locale) {
        OptionList optionList = new OptionList();
        Map reversed = reverseStringMap(map);
        List sorted = sortItems(reversed.keySet(), locale);

        for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
            String label = (String)iter.next();
            optionList.add(label, (String)reversed.get(label));
        }

        return optionList;
    }

    /**
     * Gets relative distinguished name
     *
     * @param model
     * @param dn - distinguished name
     * @return name of relative distinguished name
     */
    public static String DNToName(AMModel model, String dn) {
        String ret = dn;
        if (DN.isDN(dn)) {
            if (LDAPDN.equals(dn, SMSEntry.getRootSuffix())) {
                ret = model.getLocalizedString("top.level.realm");
            } else {
                String [] comps = LDAPDN.explodeDN(dn, true);
                ret = comps[0];
            }
        }
        return ret;
    }

    /**
     * Sorts keys in a map ordered by its value (String).
     *
     * @param map to sort
     * @param locale of user
     * @return a list of sorted keys
     */
    public static List sortMapByValue(Map map, Locale locale) {
        List listSorted = Collections.EMPTY_LIST;

        if ((map != null) && !map.isEmpty()) {
            Map mapReverse = reverseStringMap(map);
            List sortedKey = sortKeyInMap(mapReverse, locale);
            listSorted = new ArrayList(sortedKey.size());
            Iterator iter = sortedKey.iterator();

            while (iter.hasNext()) {
                String key = (String) iter.next();
                listSorted.addAll((Set) mapReverse.get(key));
            }
        }

        return listSorted;
    }

    /**
     * Replaces a string with another string in a String object.
     *
     * @param originalString original String.
     * @param token string to be replaced.
     * @param newString new string to replace token.
     * @return a String object after replacement.
     */
    public static String replaceString(
        String originalString,
        String token,
        String newString
    ) {
        int lenToken = token.length();
        int idx = originalString.indexOf(token);

        while (idx != -1) {
            originalString = originalString.substring(0, idx) +
                newString + originalString.substring(idx +lenToken);
            idx = originalString.indexOf(token, idx + lenToken);
        }

        return originalString;
    }

    /**
     * Returns a string of comma separated strings that are contained in a set.
     *
     * @param set Set of strings.
     */
    public static String toCommaSeparatedFormat(Set set) {
        StringBuilder buff = new StringBuilder();
        boolean firstEntry = true;
        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            if (!firstEntry) {
                buff.append(", ");
            } else {
                firstEntry = false;
            }
            buff.append((String)iter.next());
        }
        return buff.toString();
    }

    /**
     * Returns the display name of an <code>AMIdentity</code>.
     * a dispalyable format.
     * For example:
     *    cn=Static-1_ou=Groups_dc=sun_dc=com
     * would be dispalyed as
     *    Static-1 Administrator
     *
     * This will be used by the Privileges tab and Entity Subject tab views.
     *
     * @param model handle to AMModel interface.
     * @param universalId Universal Id of Entity object.
     * @return the display name of an <code>AMIdentity</code>.
     */
    public static String getIdentityDisplayName(
        AMModel model,
        String universalId
    ) {
        String name = "";
        try {
            name = getIdentityDisplayName(model,
                IdUtils.getIdentity(model.getUserSSOToken(), universalId));
        } catch (IdRepoException e) {
            AMModelBase.debug.warning("AMFormatUtils.getIdentityDisplayName " +
                "Could not get display name returning universalId " 
                + universalId);
          }
        return name;
    }

    /**
     * Returns the display name of an <code>AMIdentity</code>.
     * a dispalyable format.
     * For example:
     *    cn=Static-1_ou=Groups_dc=sun_dc=com
     * would be dispalyed as
     *    Static-1 Administrator
     *
     * This will be used by the Privileges tab and Entity Subject tab views.
     *
     * @param model handle to AMModel interface.
     * @param entity Entity object.
     * @return the display name of an <code>AMIdentity</code>.
     */
    public static String getIdentityDisplayName(
        AMModel model, 
        AMIdentity entity
    ) {
        String name = entity.getName();
        IdType type = entity.getType();

        if (type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE)) {
            String dn = name.replaceAll("_", ",");
            if (DN.isDN(dn)) {
                if (dn.endsWith(SMSEntry.getRootSuffix())) {
                    String[] rdns = LDAPDN.explodeDN(dn, true);
                    name = rdns[0] + " " + model.getLocalizedString(
                        "admin_suffix.name");
                }
            }
        } else if (type.equals(IdType.USER)) {
            name = model.getUserDisplayName(entity);
        }

        return name;
    }
}
