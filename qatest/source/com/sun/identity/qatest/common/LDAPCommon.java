/* The contents of this file are subject to the terms
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
 * $Id: LDAPCommon.java,v 1.11 2009/05/27 23:06:35 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;
/**
 * Following import are done due to ldapjdk pkg name change 
 */
import com.sun.identity.shared.ldap.factory.JSSESocketFactory;
import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSchema;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.LDAPModificationSet;
import com.sun.identity.shared.ldap.LDAPSchema;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPv3;
import com.sun.identity.shared.ldap.util.LDIF;
import com.sun.identity.shared.ldap.util.LDIFAddContent;
import com.sun.identity.shared.ldap.util.LDIFAttributeContent;
import com.sun.identity.shared.ldap.util.LDIFContent;
import com.sun.identity.shared.ldap.util.LDIFModifyContent;
import com.sun.identity.shared.ldap.util.LDIFRecord;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * This class has helper methods to perform the LDAP related operations
 */
public class LDAPCommon extends TestCommon {
    private String dstype;
    private String dshost;
    private String dsport;
    private String dsdirmgrdn;
    private String dsdirmgrpwd;
    private String dsrootsuffix;
    private String basedir;
    private String keystore;
    private static LDAPConnection ld = null;
    
    /**
     * Creates a new instance of LDAPCommon
     */
    private LDAPCommon() {
        super("LDAPCommon");
    }
    
    /**
     * Create new instant for LDAPCommon
     * @param    dh  Directory Server hostname
     * @param    dp  Directory Server port number
     * @param    du  Directory Server admin dn
     * @param    dw  Directory Server password
     * @param    dr  Directory Server root suffix dn
     * @param    ks  keystore file
     */
    public LDAPCommon(String dh, String dp, String du, String dw, String dr, 
            String ks)
    throws Exception {
        super("LDAPCommon");
        try {
            dshost = dh;
            dsport = dp;
            dsdirmgrdn = du;
            dsdirmgrpwd = dw;
            dsrootsuffix = dr;
            keystore = ks;
            basedir = getBaseDir();
            log(Level.FINEST, "LDAPCommon", "LDAP info : " + dshost +
                    ":" + dsport + ":" + dsdirmgrdn + ":" + dsdirmgrpwd);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Create new instant for LDAPCommon
     * @param    dh  Directory Server hostname
     * @param    dp  Directory Server port number
     * @param    du  Directory Server admin dn
     * @param    dw  Directory Server password
     * @param    dr  Directory Server root suffix dn
     */
    public LDAPCommon(String dh, String dp, String du, String dw, String dr) 
    throws Exception {
        super("LDAPCommon");
        try {
            dshost = dh;
            dsport = dp;
            dsdirmgrdn = du;
            dsdirmgrpwd = dw;
            dsrootsuffix = dr;
            keystore = null;
            basedir = getBaseDir();
            log(Level.FINEST, "LDAPCommon", "LDAP info : " + dshost +
                    ":" + dsport + ":" + dsdirmgrdn + ":" + dsdirmgrpwd);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * This method loads Access Manager user schema files
     * @param   schemaList  a list of AM user schema file name(s)
     * @param   schemaAttr  a list of schema attributes to be checked for the
     * existing of the schema in LDAP server.  If empty or not defined, schema
     * will be loaded without checking.
     */
    public void loadAMUserSchema(String schemaList, String schemaAttr)
    throws Exception {
        entering("loadAMUserSchema", null);
        try {
            log(Level.FINE, "loadAMUserSchema",
                    "Loading AM user schema for server " +
                    dshost + ":" + dsport + ":" + dsrootsuffix + "...");
            if (!isDServerUp()) {
                log(Level.SEVERE, "loadAMUserSchema",
                        "Server is down. Cannot proceed.");
                assert false;
            } else {
                String fam_qatest_identity = getQatestLDIF();
                log(Level.FINEST, "loadAMUserSchema",
                            "fam_qatest_identity =  " + fam_qatest_identity);
                String fam_qatest_identity_FileName = basedir + fileseparator + 
                        serverName + fileseparator + "built" + fileseparator + 
                        "classes" + fileseparator + "config"  + fileseparator + 
                        "default"  + fileseparator + "fam_qatest_identity.ldif";
                BufferedWriter out = new BufferedWriter(new FileWriter(
                        fam_qatest_identity_FileName));
                String[] result = fam_qatest_identity.split("\\n");
                for (int i=0 ; i < result.length ; i++) {
                    log(Level.FINEST, "loadAMUserSchema", result[i]);
                    out.write(result[i]);
                    out.newLine();
                }
                out.close();
                
                schemaList = schemaList + ";" + fam_qatest_identity_FileName;
                List schemaFilesList = getAttributeList(schemaList, ";");
                List schemaAttrsList = getAttributeList(schemaAttr, ";");
                log(Level.FINEST, "loadAMUserSchema",
                            "User schema list is " + schemaList);
                log(Level.FINE,
                        "loadAMUserSchema", "Start loading AM user schema...");
                String schemaFile;
                String schemaAttrItem;
                String fn;
                int index;
                Map ldMap = new HashMap();
                Iterator j = schemaAttrsList.iterator();
                for (Iterator i = schemaFilesList.iterator(); i.hasNext();) {
                    schemaFile = (String)i.next();
                    if (j.hasNext())
                        schemaAttrItem = ((String)j.next()).trim();  
                    else 
                        schemaAttrItem = "";
                    log(Level.FINEST,
                            "loadAMUserSchema", "Checking whether to load" +
                            " schema file " + schemaFile + " with attribute " +
                            schemaAttrItem + "...");
                    if (schemaAttrItem.length() == 0 ||
                            !isAMUserSchemaLoad(schemaAttrItem)) {
                        log(Level.FINEST,
                            "loadAMUserSchema", "Loading schema file " +
                            schemaFile + " with attribute " + schemaAttrItem + 
                            "...");
                        index = schemaFile.lastIndexOf("/");
                        if (index >= 0) {
                            fn = basedir + fileseparator + serverName +
                                    fileseparator + "ldif" + fileseparator + 
                                    schemaFile.substring(index + 1);
                            ldMap = new HashMap();
 			    if (searchStringInFile(schemaFile, 
                                    "@ROOT_SUFFIX@")) {
                            	ldMap.put("@ROOT_SUFFIX@", dsrootsuffix); 
			    } else {
                            	ldMap.put("ROOT_SUFFIX", dsrootsuffix);
			    }
                            log(Level.FINEST, "loadAMUserSchema", "ldMap=" + 
                                     ldMap);
                            replaceStringInFile(schemaFile, fn, ldMap);
                        } else
                            fn = schemaFile;
                        createSchemaFromLDIF(fn, ld);
                        if (isAMUserSchemaLoad(schemaAttrItem) || 
                                schemaAttrItem.length() == 0)
                            log(Level.FINE, "loadAMUserSchema", 
                                    "AM user schema " +  schemaFile + 
                                    " was loaded successful.");
                        else {
                            log(Level.SEVERE, "loadAMUserSchema", 
                                    "Failed to load AM user schema " + 
                                    schemaFile);
                            assert false;
                        }
                    } else
                        log(Level.FINE, "loadAMUserSchema", 
                                "AM user schema file " +  schemaFile + 
                                " is not loaded because it was already loaded");
                }
                
            }
        } catch (Exception e) {
            log(Level.SEVERE, "loadAMUserSchema", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("loadAMUserSchema");
        
    }
    
    /**
     * This method checks if Access Manager user schema loaded
     * @oaran   schema attribute to be checked.
     * @return  true if schema exists or false if schema not found
     */
    public boolean isAMUserSchemaLoad(String attrToBeChecked)
    throws Exception {
        entering("isAMUserSchemaLoad", null);
        boolean isLoad = false;
        LDAPConnection ld = getLDAPConnection();
        LDAPSchema dirSchema = new LDAPSchema();
        try {
            dirSchema.fetchSchema(ld);
            LDAPAttributeSchema newAttrType =
                    dirSchema.getAttribute(attrToBeChecked);
            if (newAttrType != null) {
                log(Level.FINEST, "isAMUserSchemaLoad", 
                        "Attribute " + attrToBeChecked + " := " + 
                        newAttrType.toString());
                isLoad = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        exiting("isAMUserSchemaLoad");
        return isLoad;
    }
    
    /**
     * This method validates if directory server is running and can be
     * connected at the specified host and port.
     * @return  true if directory server is running.
     */
    public boolean isDServerUp()
    throws Exception {
        return (getLDAPConnection() == null) ? false : true;
    }
    
    /**
     * This method creates LDAP schema from LDIF file.
     * @param   file    file containing LDIF entries.
     * @param   ld      LDAP Connection.
     */
    public void createSchemaFromLDIF(String file, LDAPConnection ld)
    throws IOException, LDAPException {
        entering("createSchemaFromLDIF", null);
        createSchemaFromLDIF(new LDIF(file), ld);
        exiting("createSchemaFromLDIF");
    }
    
    /**
     * This method creates LDAP schema from LDIF file.
     * @param   ldif    LDIF object.
     * @param   ld      LDAP Connection.
     */
    public void createSchemaFromLDIF(LDIF ldif, LDAPConnection ld)
    throws IOException, LDAPException {
        entering("createSchemaFromLDIF", null);
        LDIFContent content = null;
        String DN = null;
        LDAPAttributeSet attrSet = null;
        LDAPAttribute[] attrs;
        LDAPEntry amEntry;
        for(LDIFRecord rec = ldif.nextRecord(); rec != null;
        rec = ldif.nextRecord()) {
            try {
                content = rec.getContent();
                DN = rec.getDN();
                if (content instanceof LDIFModifyContent) {
                    ld.modify(DN,
                            ((LDIFModifyContent)content).getModifications());
                } else if ((content instanceof LDIFAttributeContent) ||
                        (content instanceof LDIFAddContent)) {
                    attrs = (content instanceof LDIFAttributeContent) ?
                        ((LDIFAttributeContent)content).getAttributes() :
                        ((LDIFAddContent)content).getAttributes();
                    amEntry = new LDAPEntry(DN,new LDAPAttributeSet(attrs));
                    ld.add(amEntry);
                }
            } catch (LDAPException e) {
                log(Level.FINEST, "createSchemaFromLDIF", "LDAP return code " + 
                        e.getLDAPResultCode());
                if (e.getLDAPErrorMessage() != null)
                    log(Level.FINEST, "createSchemaFromLDIF", "LDAP message " + 
                        e.getLDAPErrorMessage());
                switch (e.getLDAPResultCode()) {
                    case LDAPException.ATTRIBUTE_OR_VALUE_EXISTS:
                        log(Level.FINE, "createSchemaFromLDIF", 
                                "Attribute already exists");
                        break;
                    case LDAPException.NO_SUCH_ATTRIBUTE:
                        // Ignore some attributes need to be deleted if present
                        break;
                    case LDAPException.ENTRY_ALREADY_EXISTS:
                        log(Level.FINE, "createSchemaFromLDIF", 
                                "Entry already exists");
                        LDAPModificationSet modSet = new LDAPModificationSet();
                        attrs = (content instanceof LDIFAttributeContent) ?
                            ((LDIFAttributeContent)
                            content).getAttributes() :
                            ((LDIFAddContent)content).getAttributes();
                        for (int i = 0; i < attrs.length; i++) {
                            modSet.add(LDAPModification.ADD, attrs[i]);
                        }
                        try {
                            ld.modify(DN, modSet);
                        } catch (LDAPException ex) {
                            //Ignore the exception
                        }
                        break;
                    default:
                }
            }
        }
        exiting("createSchemaFromLDIF");
    }

    /**
     * This method disconnects and terminate LDAP connection.
     */
    public void disconnectDServer()
    throws Exception {
        if ((ld != null) && ld.isConnected()) {
            try {
                ld.disconnect();
                ld = null;
            } catch (LDAPException e) {
                log(Level.SEVERE, "disconnectDServer",
                        "LDAP error with return code " + e.getLDAPResultCode());
                e.printStackTrace();
                throw e;
            }
        }
    }

    /**
     * This method gets vendor info for the directory server
     */
    public String getVendorInfo()
    throws Exception {
        LDAPEntry ldapEntry = null;
        String strAttrs[] = new String[1];
        strAttrs[0] = "vendorVersion";
        LDAPSearchResults srchRes = ld.search("", LDAPv3.SCOPE_BASE,
                "objectclass=*", strAttrs, false);
        while (srchRes.hasMoreElements()) {
            ldapEntry = (LDAPEntry)srchRes.nextElement();
        }
        if (ldapEntry != null) {
            LDAPAttributeSet ldapAS = ldapEntry.getAttributeSet();
            LDAPAttribute ldapAtt = ldapAS.getAttribute("vendorVersion");
            String[] strAttVal = ldapAtt.getStringValueArray();
            return (strAttVal[0]);
        } else
            return ("");
    }

    /**
     * This method creates a LDAP connection.
     */
    private LDAPConnection getLDAPConnection()
    throws Exception {
        if (ld == null) {
            try {
                if (keystore != null) {
                    Security.addProvider(
                            new com.sun.net.ssl.internal.ssl.Provider());
                    System.setProperty("javax.net.ssl.trustStore",keystore);
                    JSSESocketFactory lds = new JSSESocketFactory(null);
                    ld = new LDAPConnection(lds);
                } else
                    ld = new LDAPConnection();
                ld.setConnectTimeout(300);
                ld.connect(3, dshost,
                        Integer.parseInt(dsport), dsdirmgrdn, dsdirmgrpwd);
            } catch (LDAPException e) {
                disconnectDServer();
                ld = null;
                log(Level.SEVERE, "getLDAPConnection",
                        "LDAP error with return code " + e.getLDAPResultCode());
                e.printStackTrace();
                throw e;
            }
        }
        return ld;
    }
    
    /**
     * This method return the ldif containing custom attributes used by qatest 
     * for configuring user config datastore attributes.
     */
    private String getQatestLDIF() 
    throws Exception {

        String strVendor = getVendorInfo();
        log(Level.FINEST, "getQatestLDIF", "strVendor: " + strVendor);
    
        StringBuffer buff = new StringBuffer();
        buff.append("dn: ou=people, @ROOT_SUFFIX@\n")
            .append("objectClass: top\n")
            .append("objectClass: organizationalUnit\n\n")
            .append("ou: people\n\n")

            .append("dn: ou=agents, @ROOT_SUFFIX@\n")
            .append("objectClass: top\n")
            .append("objectClass: organizationalUnit\n\n")
            .append("ou: agents\n\n")

            .append("dn: ou=groups, @ROOT_SUFFIX@\n")
            .append("objectClass: top\n")
            .append("objectClass: organizationalUnit\n\n")
            .append("ou: groups\n\n")

            .append("dn: ou=dsame users, @ROOT_SUFFIX@\n")
            .append("objectClass: top\n")
            .append("objectClass: organizationalUnit\n\n")
            .append("ou: dsame users\n\n")

            .append("dn: cn=dsameuser,ou=DSAME Users, @ROOT_SUFFIX@\n")
            .append("objectclass: inetuser\n")
            .append("objectclass: organizationalperson\n")
            .append("objectclass: person\n")
            .append("objectclass: top\n")
            .append("cn: dsameuser\n")
            .append("sn: dsameuser\n")
            .append("userPassword: amsecret12\n\n");

            if (strVendor.contains("OpenDS") || strVendor.contains("OpenDJ")) {
                buff.append("dn: cn=dsameuser,ou=DSAME Users,@ROOT_SUFFIX@\n")
                .append("changetype: modify\n")
                .append("add: ds-privilege-name\n")
                .append("ds-privilege-name: password-reset\n\n");
            }

            buff.append("dn: cn=amldapuser,ou=DSAME Users, @ROOT_SUFFIX@\n")
            .append("objectclass: inetuser\n")
            .append("objectclass: organizationalperson\n")
            .append("objectclass: person\n")
            .append("objectclass: top\n")
            .append("cn: amldapuser\n")
            .append("sn: amldapuser\n")
            .append("userPassword: amsecret123\n\n")

            .append("dn: @ROOT_SUFFIX@\n")
            .append("changetype:modify\n")
            .append("add:aci\n")
            .append("aci: (target=\"ldap:///@ROOT_SUFFIX@\")" +
                    "(targetattr=\"*\")(version 3.0; acl \"S1IS special dsame" +
                    " user rights for all under the root suffix\";" +
                    " allow (all) userdn = \"ldap:///cn=dsameuser," +
                    "ou=DSAME Users, @ROOT_SUFFIX@\"; )\n\n")

            .append("dn:@ROOT_SUFFIX@\n")
            .append("changetype:modify\n")
            .append("add:aci\n")
            .append("aci: (target=\"ldap:///@ROOT_SUFFIX@\")" +
                    "(targetattr=\"*\")(version 3.0; acl \"S1IS special" +
                    " ldap auth user rights\"; allow (read,search) userdn =" +
                    " \"ldap:///cn=amldapuser,ou=DSAME Users," +
                    " @ROOT_SUFFIX@\"; )\n\n");        

            if (strVendor.contains("OpenDS") || strVendor.contains("OpenDJ")) {
                buff.append("dn:@ROOT_SUFFIX@\n")
                .append("changetype:modify\n")
                .append("delete:aci\n")
                .append("aci: (target=\"ldap:///@ROOT_SUFFIX@\")(targetattr =" +
                        " \"*\")(version 3.0; acl \"OpenSSO-FAM Services" +
                        " anonymous access\"; deny (all) userdn =" +
                        " \"ldap:///anyone\";)\n\n")

                .append("dn:@ROOT_SUFFIX@\n")
                .append("changetype:modify\n")
                .append("add:aci\n")
                .append("aci: (target=\"ldap:///ou=services,@ROOT_SUFFIX@\")" +
                        "(targetattr = \"*\")(version 3.0; acl \"OpenSSO-FAM" +
                        " Services anonymous access\"; deny (all) userdn =" +
                        " \"ldap:///anyone\";)\n\n")

                .append("dn:@ROOT_SUFFIX@\n")
                .append("changetype:modify\n")
                .append("add:aci\n")
                .append("aci:(targetcontrol = \"2.16.840.1.113730.3.4.3\")" +
                        "(version 3.0; acl \"Allow Persistent Search for the" +
                        " OpenSSO datastore config bind user\"; allow (all)" +
                        " userdn = \"ldap:///cn=dsameuser,ou=DSAME Users," +
                        "@ROOT_SUFFIX@\";)\n\n")

                .append("dn:@ROOT_SUFFIX@\n")
                .append("changetype:modify\n")
                .append("add:aci\n")
                .append("aci: (targetattr = \"objectclass || inetuserstatus" +
                        " || iplanet-am-user-login-status ||" +
                        " iplanet-am-user-account-life ||" +
                        " iplanet-am-session-quota-limit ||" +
                        " iplanet-am-user-alias-list ||" +
                        " iplanet-am-session-max-session-time ||" +
                        " iplanet-am-session-max-idle-time ||" +
                        " iplanet-am-session-get-valid-sessions ||" +
                        " iplanet-am-session-destroy-sessions ||" +
                        " iplanet-am-session-add-session-listener-" +
                        "on-all-sessions || iplanet-am-user-admin-start-dn ||" +
                        " iplanet-am-auth-post-login-process-class ||" +
                        " iplanet-am-saml-user || iplanet-am-saml-password ||" +
                        " iplanet-am-user-federation-info ||" +
                        " iplanet-am-user-federation-info-key ||" +
                        " ds-pwp-account-disabled ||" +
                        " sun-fm-saml2-nameid-info ||" +
                        " sun-fm-saml2-nameid-infokey ||" +
                        " sunAMAuthInvalidAttemptsData || memberof ||" +
                        " member\")(targetfilter=\"(!(userdn=cn=dsameuser," +
                        "ou=DSAME Users,@ROOT_SUFFIX@))\")(version 3.0;" +
                        " acl \"OpenSSO User self modification denied for" +
                        " these attributes\"; deny (write) userdn =" +
                        "\"ldap:///self\";)\n\n");
            }
            
        return buff.toString();
    }
}
