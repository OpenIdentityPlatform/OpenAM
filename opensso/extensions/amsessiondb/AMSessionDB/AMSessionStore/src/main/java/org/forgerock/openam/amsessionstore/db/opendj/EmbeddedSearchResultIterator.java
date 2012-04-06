/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.amsessionstore.db.opendj;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.forgerock.openam.amsessionstore.common.CaseInsensitiveHashMap;
import org.forgerock.openam.amsessionstore.common.Log;
import org.forgerock.openam.amsessionstore.db.StoreException;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.SearchResultEntry;

/**
 *
 * @author steve
 */
public class EmbeddedSearchResultIterator {
    private Iterator resultIter;
    private Set excludeDNs;
    private boolean hasExcludeDNs;
    private AMRecordDataEntry current;

    /**
     * Constructs a <code>SearchResultIterator</code>
     *
     * @param results LDAP Search Results object.
     * @param excludeDNs a set of distinguished names to be excluded
     */
    public EmbeddedSearchResultIterator(LinkedList results, Set excludeDNs) {
        resultIter = results.iterator();
        this.excludeDNs = excludeDNs;
        hasExcludeDNs = (excludeDNs != null) && !excludeDNs.isEmpty();
    }

    public boolean hasNext() {
        if (!resultIter.hasNext()) {
            return false;
        }

        SearchResultEntry entry = (SearchResultEntry) resultIter.next();
        String dn = entry.getDN().toString();
        
        if (hasExcludeDNs) {
            while (excludeDNs.contains(dn)) {
                if (resultIter.hasNext()) {
                    entry = (SearchResultEntry)resultIter.next();
                    dn = entry.getDN().toString();
                } else {
                    entry = null;
                    break;
                }
            }
        }

        try {
            current = (entry == null) ? null : new AMRecordDataEntry(dn,
                convertLDAPAttributeSetToMap(entry.getAttributes()));
        } catch (StoreException se) {
            Log.logger.log(Level.WARNING,"Unable to create AMRecordDataEntry ", se);
        }
            
        return (current != null);
    }

    public Object next() {
        AMRecordDataEntry tmp = current;
        current = null;
        return tmp;
    }

    public void remove() {
        // not supported.
    }

    static Map<String, Set<String>> convertLDAPAttributeSetToMap(List<Attribute> attributes) {
        Map answer = null;
        
        if ((attributes != null) && (!attributes.isEmpty())) {
            for (Attribute attr : attributes) {               
                if (attr != null) {
                    Set<String> strValues = new HashSet<String>();
                 
                    for(AttributeValue value : attr) {
                        strValues.add(value.toString());
                    }
                    
                    if (answer == null) {
                        answer = new CaseInsensitiveHashMap(10);
                    }
                  
                    answer.put(attr.getName(), strValues);
                }
            }
        }
        
        return (answer);
    }    
}
