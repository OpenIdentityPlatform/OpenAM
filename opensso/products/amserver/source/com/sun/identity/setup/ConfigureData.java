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
 * $Id: ConfigureData.java,v 1.11 2009/05/02 23:05:13 kevinserwin Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.setup;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.ServletContext;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;

/**
 * Configures product bootstrap data.
 */
public class ConfigureData {
    private String baseDir;
    private SSOToken ssoToken;
    private String hostname;
    private ServletContext sctx;

    /**
     * Constructs a new instance.
     *
     * @param baseDir Directory where data is stored.
     * @param sctx Servlet Context.
     * @param hostname Host name of the machine running the product.
     * @param ssoToken Administrator Single Sign On token which to be used
     *        to configure the product.
     */
    public ConfigureData(
        String baseDir,
        ServletContext sctx,
        String hostname,
        SSOToken ssoToken
    ) {
        this.baseDir = baseDir;
        this.sctx = sctx;
        this.hostname = hostname;
        this.ssoToken = ssoToken;
    }

    /**
     * Configures the product.
     *
     * @throws SMSException if service management API failed.
     * @throws SSOException if Single Sign On token is invalid.
     * @throws IOException if IO operations failed.
     * @throws PolicyException if policy cannot be loaded.
     */
    public void configure()
        throws SMSException, SSOException, IOException, PolicyException
    {
        modifyClientDataService();
        createRealmAndPolicies();
        setRealmAttributes();
    }

    private void modifyClientDataService()
        throws SMSException, SSOException, IOException
    {
        Map map = new HashMap();
        map.put("profileManagerXML",
            getFileContentInSet("SunAMClientData.xml"));
        modifySchemaDefaultValues("SunAMClientData", SchemaType.GLOBAL,
            null, map);
    }

    private void createRealmAndPolicies()
        throws SMSException, SSOException, PolicyException, IOException,
            FileNotFoundException
    {
        createRealm("/sunamhiddenrealmdelegationservicepermissions");
        createPolicies("/sunamhiddenrealmdelegationservicepermissions",
            baseDir + "/defaultDelegationPolicies.xml");
    }

    private void setRealmAttributes()
        throws SMSException
    {
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            ssoToken, "/");
        Map map = new HashMap();
        Set set1 = new HashSet(2);
        set1.add("Active");
        map.put("sunOrganizationStatus", set1);
        Set set2 = new HashSet(2);
        Map defaultValues = ServicesDefaultValues.getDefaultValues();
        set2.add(DNToName((String)defaultValues.get(
            SetupConstants.CONFIG_VAR_ROOT_SUFFIX)));
        map.put("sunOrganizationAliases", set2);
        ocm.setAttributes("sunIdentityRepositoryService", map);
    }
    
    private static String DNToName(String dn) {
        String ret = dn;
        if (DN.isDN(dn)) {
            String[] comps = LDAPDN.explodeDN(dn, true);
            ret = comps[0];
        }
        return ret;
    }


    private void createPolicies(String realmName, String xmlFile)
        throws FileNotFoundException, PolicyException, SSOException, IOException
    {
        PolicyManager pm = new PolicyManager(ssoToken, realmName);

        InputStreamReader fin = new InputStreamReader(
            AMSetupServlet.getResourceAsStream(sctx, xmlFile));
        StringBuilder sbuf = new StringBuilder();
        char[] cbuf = new char[1024];
        int len;
        while ((len = fin.read(cbuf)) > 0) {
            sbuf.append(cbuf, 0, len);
        }
        String data = ServicesDefaultValues.tagSwap(sbuf.toString(), true);
        ByteArrayInputStream bis = new ByteArrayInputStream(
            data.getBytes());
        PolicyUtils.createPolicies(pm, bis);
    }

    private void modifySchemaDefaultValues(
        String serviceName,
        SchemaType schemaType,
        String subSchema,
        Map values
    ) throws SMSException, SSOException, IOException {
        ServiceSchema ss = getServiceSchema(serviceName, schemaType, subSchema);
        ss.setAttributeDefaults(values);
    }
        
    private ServiceSchema getServiceSchema(
        String serviceName,
        SchemaType schemaType,
        String subSchema
    ) throws SMSException, SSOException {
        ServiceSchemaManager ssm = new ServiceSchemaManager(
            serviceName, ssoToken);
        ServiceSchema ss = ssm.getSchema(schemaType);
                                                                                
        if (subSchema != null) {
            boolean done = false;
            StringTokenizer st = new StringTokenizer(subSchema, "/");

            while (st.hasMoreTokens() && !done) {
                String str = st.nextToken();
                                                                                
                if (str != null) {
                    ss = ss.getSubSchema(str);
                    if (ss == null) {
                        throw new RuntimeException(
                            "SubSchema" + str + "does not exist");
                    }
                } else {
                    done = true;
                }
            }
        }
        return ss;
    }

    private Set getFileContentInSet(String fileName)
        throws IOException
    {
        Set set = new HashSet(2);
        set.add(getFileContent(fileName));
        return set;
    }

    private String getFileContent(String fileName)
        throws IOException
    {
        StringBuilder sbuf = new StringBuilder();
        InputStreamReader fin = new InputStreamReader(
            AMSetupServlet.getResourceAsStream(sctx, baseDir + "/" + fileName));
        char[] cbuf = new char[1024];
        int len;

        while ((len = fin.read(cbuf)) > 0) {
            sbuf.append(cbuf, 0, len);
        }

        return sbuf.toString();
    }

    private void createRealm(String realmName)
        throws SMSException
    {
        String parentRealm = getParentRealm(realmName);
        String childRealm = getChildRealm(realmName);
        
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            ssoToken, parentRealm);
        ocm.createSubOrganization (childRealm, null);
    }

    private static String getParentRealm(String path) {
        String parent = "/";
        path = normalizeRealm(path);
        if ((path != null) && (path.length() > 0)) {
            int idx = path.lastIndexOf('/');
            if (idx > 0) {
                parent = path.substring(0, idx);
            }
        }
        return parent;
    }
                                                                                
    private static String getChildRealm(String path) {
        String child = "/";
        path = normalizeRealm(path);
        if ((path != null) && (path.length() > 0)) {
            int idx = path.lastIndexOf('/');
            if (idx != -1) {
                child = path.substring(idx+1);
            }
        }
        return child;
    }

    private static String normalizeRealm(String path) {
        if (path != null) {
            path = path.trim();
            if (path.length() > 0) {
                while (path.indexOf("//") != -1) {
                    path = path.replaceAll("//", "/");
                }
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() -1);
                }
            }
        }
        return path.trim();
    }
}
