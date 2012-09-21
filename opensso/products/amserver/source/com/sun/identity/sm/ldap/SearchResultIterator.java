/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SearchResultIterator.java,v 1.2 2009/04/02 20:22:43 veiming Exp $
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm.ldap;

import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.sm.SMSDataEntry;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class iterates through the <code>LDAPSearchResults</code> and converts the
 * LDAPEntry to a <code>SMSDataEntry</code> object.
 */
public class SearchResultIterator implements Iterator {
    private LDAPSearchResults results;
    private Set excludeDNs;
    private boolean hasExcludeDNs;
    private SMSDataEntry current;

    /**
     * Constructs a <code>SearchResultIterator</code>
     *
     * @param results LDAP Search Results object.
     * @param excludeDNs a set of distinguished names to be excluded
     */
    public SearchResultIterator(LDAPSearchResults results, Set excludeDNs) {
        this.results = results;
        this.excludeDNs = excludeDNs;
        hasExcludeDNs = (excludeDNs != null) && !excludeDNs.isEmpty();
    }

    public boolean hasNext() {
        if (!results.hasMoreElements()) {
            return false;
        }
        try {
            LDAPEntry entry = results.next();
            String dn = entry.getDN();
            if (hasExcludeDNs) {
                while (excludeDNs.contains(dn)) {
                    if (results.hasMoreElements()) {
                        entry = results.next();
                        dn = entry.getDN();
                    } else {
                        entry = null;
                        break;
                    }
                }
            }

            current = (entry == null) ? null :
                new SMSDataEntry(dn, convertLDAPAttributeSetToMap(
                    entry.getAttributeSet()));
            
            return (current != null);
        } catch (LDAPException ldape) {
            Debug.getInstance("amSMSLdap").error("SearchResultIterator.hasNext",
                ldape);
        }
        return false;
    }

    public Object next() {
        SMSDataEntry tmp = current;
        current = null;
        return tmp;
    }

    public void remove() {
        //not supported.
    }

    static Map convertLDAPAttributeSetToMap(LDAPAttributeSet attrSet) {
        Map answer = null;

        if (attrSet != null) {
            for (Enumeration enums = attrSet.getAttributes(); enums
                    .hasMoreElements();) {
                LDAPAttribute attr = (LDAPAttribute) enums.nextElement();
                if (attr != null) {
                    Set values = new HashSet();
                    String[] value = attr.getStringValueArray();
                    values.addAll(Arrays.asList(value));
                    if (answer == null) {
                        answer = new CaseInsensitiveHashMap(10);
                    }
                    answer.put(attr.getName(), values);
                }
            }
        }
        return (answer);
    }
}
