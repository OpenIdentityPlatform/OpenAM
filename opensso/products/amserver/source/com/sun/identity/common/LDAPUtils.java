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
 * $Id: LDAPUtils.java,v 1.9 2009/08/05 20:39:01 hengming Exp $
 *
 */

package com.sun.identity.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Enumeration;
import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.LDAPModificationSet;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.LDIF;
import com.sun.identity.shared.ldap.util.LDIFAddContent;
import com.sun.identity.shared.ldap.util.LDIFAttributeContent;
import com.sun.identity.shared.ldap.util.LDIFContent;
import com.sun.identity.shared.ldap.util.LDIFModifyContent;
import com.sun.identity.shared.ldap.util.LDIFRecord;

public class LDAPUtils {
    
    private LDAPUtils() {
    }
    
    /**
     * Creates LDAP schema from LDIF file.
     *
     * @param file file containing LDIF entries.
     * @param ld LDAP Connection.
     */
    public static void createSchemaFromLDIF(
        String file, 
        LDAPConnection ld
    ) throws IOException, LDAPException {
        createSchemaFromLDIF(new LDIF(file), ld);
    }


    /**
     * Creates LDAP schema from LDIF file.
     *
     * @param stream Data input stream containing LDIF entries.
     * @param ld LDAP Connection.
     */
    public static void createSchemaFromLDIF(
        DataInputStream stream, 
        LDAPConnection ld
    ) throws IOException, LDAPException {
        createSchemaFromLDIF(new LDIF(stream), ld);
    }
    

    /**
     * Creates LDAP schema from LDIF file.
     *
     * @param ldif LDIF object.
     * @param ld LDAP Connection.
     */
    public static void createSchemaFromLDIF(
        LDIF ldif,
        LDAPConnection ld
    ) throws IOException, LDAPException {
        DN ripDN = new DN(
            "cn=referential integrity postoperation,cn=plugins,cn=config");
        CaseInsensitiveHashSet existPlugins = null;
        int nextPluginIndex = 0;
        for(LDIFRecord rec = ldif.nextRecord(); rec != null; 
            rec = ldif.nextRecord()
        ) {
            LDIFContent content = null;
            String ldifDN = null;
            
            try {
                content = rec.getContent();
                ldifDN = rec.getDN();

                if (content instanceof LDIFModifyContent) {

                    LDAPModification[] mods =
                        ((LDIFModifyContent)content).getModifications();
                    if (ripDN.equals(new DN(ldifDN))) {
                        LDAPModificationSet modSet = new LDAPModificationSet();
                        for(int i=0; i<mods.length; i++) {
                            LDAPAttribute attr = mods[i].getAttribute();
                            String attrName = attr.getName().toLowerCase();
                            if (!attrName.startsWith("nsslapd-pluginarg")) {
                                modSet.add(mods[i].getOp(), attr);
                                continue;
                            }

                            if (existPlugins == null) {
                                existPlugins = new CaseInsensitiveHashSet();
                                LDAPEntry entry = ld.read(ldifDN);
                                    
                                while(true) {
                                    LDAPAttribute pluginAttr =
                                        entry.getAttribute("nsslapd-pluginarg" +
                                        nextPluginIndex);
                                    if (pluginAttr == null) {
                                        break;
                                    }

                                    existPlugins.add(pluginAttr.
                                        getStringValueArray()[0]);
                                    nextPluginIndex++;
                                }
                            }
                            String value = attr.getStringValueArray()[0];
                            if (!existPlugins.contains(value)) {
                                attr = new LDAPAttribute(
                                    "nsslapd-pluginarg" + nextPluginIndex,
                                    value);
                                modSet.add(mods[i].getOp(), attr);
                                nextPluginIndex++;
                            }
                        }
                        if (modSet.size() > 0) {
                            ld.modify(ldifDN, modSet);
                        }
                    } else {
                        ld.modify(ldifDN, mods);
                    }
                } else if ((content instanceof LDIFAttributeContent) ||
                    (content instanceof LDIFAddContent)
                    ) {
                    LDAPAttributeSet attrSet = null;
                    LDAPAttribute[] attrs =
                        (content instanceof LDIFAttributeContent) ?
                            ((LDIFAttributeContent)content).getAttributes():
                            ((LDIFAddContent)content).getAttributes();
                    LDAPEntry amEntry = new LDAPEntry(ldifDN,
                        new LDAPAttributeSet(attrs));
                    ld.add(amEntry);
                }
                
                
            } catch (LDAPException e) {
                switch (e.getLDAPResultCode()) {
                    case LDAPException.ENTRY_ALREADY_EXISTS:
                        LDAPModificationSet modSet =
                            new LDAPModificationSet();
                        LDAPAttribute[] attrs =
                            (content instanceof LDIFAttributeContent) ?
                                ((LDIFAttributeContent)content).getAttributes():
                                ((LDIFAddContent)content).getAttributes();
                        
                        for (int i = 0; i < attrs.length; i++) {
                            modSet.add(LDAPModification.ADD, attrs[i]);
                        }
                        try {
                            ld.modify(ldifDN, modSet);
                        } catch (LDAPException ex) {
                            //Ignore the exception
                        }
                        break;
                    default:
                        // let it thru
                }
            }
        }
    }
    
    public static String getDBName(String suffix, LDAPConnection ld)
        throws LDAPException {
        String dbName = null;
        String filter = "cn=" + suffix; 

        try {
            LDAPSearchResults results = 
                ld.search("cn=mapping tree,cn=config",
                LDAPConnection.SCOPE_SUB, filter, null, false);
            while (results.hasMoreElements()) {
                LDAPEntry entry = results.next();
                String dn = entry.getDN();
                LDAPAttributeSet set = entry.getAttributeSet();
                Enumeration e = set.getAttributes();
                while (e.hasMoreElements() && (dbName == null)) {
                    LDAPAttribute attr = 
                        (LDAPAttribute) e.nextElement();
                    String name = attr.getName();
                    if (name.equals("nsslapd-backend")) {
                        String[] value = attr.getStringValueArray();
                        if (value.length > 0) {
                            dbName = value[0];
                        }
                    }
                }
            }
        } catch (LDAPException lde) {
            // If not S1DS, then cn=mapping tree wouldn't exist.
            // Hence return userRoot as DBNAME.
        }
        return (dbName != null) ? dbName : "userRoot";
    }
}
