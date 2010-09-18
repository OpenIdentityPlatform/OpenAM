/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Migrate.java,v 1.4 2008/08/19 19:14:58 veiming Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Creates new service schema for <code>sunFMCOTConfigService</code>.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final String SCHEMA_FILE = "fmCOTConfig.xml";
    final String SERVICE_NAME = "sunFMCOTConfigService";
    final String SERVICE_VERSION = "1.0";
    final String SUBCONFIG_ID = "cot";
    final String IDFF_COT_SERVICE_NAME = 
            "iPlanetAMAuthenticationDomainConfigService";
    final String SAML2_COT_SERVICE_NAME = "sunSAML2COTConfigService";
    final String IDFF_COT_DESCRIPTION = 
            "iplanet-am-authenticationdomain-description";
    final String IDFF_WRITER_URL = "iplanet-am-writerservice-url";
    final String IDFF_READER_URL = "iplanet-am-readerservice-url";
    final String IDFF_COT_STATUS = "iplanet-am-authenticationdomain-status";
    final String SAML2_COT_DESCRIPTION = "sun-saml2-cot-description";
    final String SAML2_WRITER_URL = "sun-saml2-writerservice-url";
    final String SAML2_READER_URL = "sun-saml2-readerservice-url";
    final String SAML2_COT_STATUS = "sun-saml2-cot-status";
    final String SAML2_TRUSTED_PROVIDERS = "sun-saml2-trusted-providers";
    final String FAM_COT_DESCRIPTION = "sun-fm-cot-description";
    final String FAM_COT_STATUS = "sun-fm-cot-status";
    final String FAM_IDFF_WRITER_URL = "sun-fm-idff-writerservice-url";
    final String FAM_IDFF_READER_URL = "sun-fm-idff-readerservice-url";
    final String FAM_SAML2_WRITER_URL = "sun-fm-saml2-writerservice-url";
    final String FAM_SAML2_READER_URL = "sun-fm-saml2-readerservice-url";
    final String FAM_TRUSTED_PROVIDERS = "sun-fm-trusted-providers";

    /**
     * Creates service schema for <code>sunFMCOTConfigService</code>
     * service.
     *
     * @return true if service creation is successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            String fileName = UpgradeUtils.getNewServiceNamePath(SCHEMA_FILE);
            UpgradeUtils.createService(fileName);
            isSuccess = true;
        } catch (UpgradeException e) {
            UpgradeUtils.debug.error("Error loading service schema", e);
        }
        return isSuccess;
    }

    /**
     * Post Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean postMigrateTask() {
        // retreives saml2 & idff cot service attributes.
        Map idffCOTAttrs = UpgradeUtils.getSubConfigAttributes(
                IDFF_COT_SERVICE_NAME,SERVICE_VERSION);
        migrateIDFFCOTConfig(idffCOTAttrs);

        Map saml2COTAttrs = UpgradeUtils.getSubConfigAttributes(
                SAML2_COT_SERVICE_NAME, SERVICE_VERSION);
        migrateSAML2COTConfig(saml2COTAttrs);

        return true;
    }

    /**
     * Pre Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean preMigrateTask() {
        return true;
    }

    /**
     * Migrates the IDFF Authentication Domain Service Configuration
     * to OpenSSO COT Service
     */
    private void migrateIDFFCOTConfig(Map idffCOTAttrs) {
        Set subConfigNames = idffCOTAttrs.keySet();
        Map famCOTAttrsMap = new HashMap();
        Iterator i = subConfigNames.iterator();
        while (i.hasNext()) {
            String subConfigName = (String) i.next();
            Map attrs = (Map) idffCOTAttrs.get(subConfigName);
            Set description = (Set) attrs.get(IDFF_COT_DESCRIPTION);
            famCOTAttrsMap.put(FAM_COT_DESCRIPTION, description);
            Set status = (Set) attrs.get(IDFF_COT_STATUS);
            famCOTAttrsMap.put(FAM_COT_STATUS, status);
            Set writerURL = (Set) attrs.get(IDFF_WRITER_URL);
            famCOTAttrsMap.put(FAM_IDFF_WRITER_URL, writerURL);
            Set readerURL = (Set) attrs.get(IDFF_READER_URL);
            famCOTAttrsMap.put(FAM_IDFF_READER_URL, readerURL);
            famCOTAttrsMap.put(FAM_TRUSTED_PROVIDERS, new HashSet());
            UpgradeUtils.createOrgSubConfig(SERVICE_NAME, SERVICE_VERSION, 
                    SUBCONFIG_ID, subConfigName, famCOTAttrsMap);
        }
    }

    /**
     * Migrates the SAMLv2 COT Service Configuration to OpenSSO COT
     * Service
     */
    private void migrateSAML2COTConfig(Map saml2COTAttrs) {
        Set subConfigNames = saml2COTAttrs.keySet();
        Map famCOTAttrsMap = new HashMap();
        Iterator i = subConfigNames.iterator();
        while (i.hasNext()) {
            String subConfigName = (String) i.next();
            Map attrs = (Map) saml2COTAttrs.get(subConfigName);
            Set description = (Set) attrs.get(SAML2_COT_DESCRIPTION);
            famCOTAttrsMap.put(FAM_COT_DESCRIPTION, description);
            Set status = (Set) attrs.get(SAML2_COT_STATUS);
            famCOTAttrsMap.put(FAM_COT_STATUS, status);
            famCOTAttrsMap.put(FAM_COT_STATUS, status);
            Set writerURL = (Set) attrs.get(SAML2_WRITER_URL);
            famCOTAttrsMap.put(FAM_SAML2_WRITER_URL, writerURL);
            Set readerURL = (Set) attrs.get(SAML2_READER_URL);
            famCOTAttrsMap.put(FAM_SAML2_READER_URL, readerURL);
            Set trustedProviders = (Set) attrs.get(SAML2_TRUSTED_PROVIDERS);
            famCOTAttrsMap.put(FAM_TRUSTED_PROVIDERS, trustedProviders);
            UpgradeUtils.createOrgSubConfig(SERVICE_NAME, SERVICE_VERSION,
                    SUBCONFIG_ID, subConfigName, famCOTAttrsMap);
        }
    }
}
