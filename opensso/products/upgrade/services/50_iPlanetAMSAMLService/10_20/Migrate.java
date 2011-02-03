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
 * $Id: Migrate.java,v 1.1 2008/07/22 22:55:16 bina Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.util.Set;
import java.util.Properties;
import java.util.HashSet;

/**
 * Updates <code>iPlanetAMSAMLService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "iPlanetAMSAMLService";
    final static String SERVICE_DIR = "50_iPlanetAMSAMLService/10_20";
    final static String SCHEMA_TYPE = "Global";
    final static String SCHEMA_FILE = "amSAML_addAttrs.xml";
    final static String i18NFileName = "fmSAMLConfiguration";
    final static String SAML_ASSERTION_VERSION = 
        "com.sun.identity.saml.assertion.version";
    final static String SAML_PROTOCOL_VERSION = 
        "com.sun.identity.saml.protocol.version";
    final static String REMOVE_ASSERTION = 
        "com.sun.identity.saml.removeassertion";

    final static String ATTR_SAML_ASSERTION_VERSION = "DefaultAssertionVersion";
    final static String ATTR_SAML_PROTOCOL_VERSION = "DefaultProtocolVersion";
    final static String ATTR_REMOVE_ASSERTION = "RemoveAssertion";


    /**
     * Updates the <code>iPlanetAMSAMLService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        String classMethod = "iPlanetSAMLService/10_20/Migrate:migrateService";
        boolean isSuccess = false;
        try {
            UpgradeUtils.seti18NFileName(SERVICE_NAME,i18NFileName);

            // add attributes to service schema
            String fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE);
            UpgradeUtils.addAttributeToSchema(
                    SERVICE_NAME, SCHEMA_TYPE, fileName);

            // migrate the SAML properties set in AMConfig.properties 
            // to iPlanetAMSAMLService

            Properties p = UpgradeUtils.getServerProperties();
            String assertionVersion = (String) p.get(SAML_ASSERTION_VERSION);
            Set defaultValues = new HashSet();
            defaultValues.add(assertionVersion);
            // set attribute value in service schema.
            UpgradeUtils.setAttributeDefaultValues(SERVICE_NAME,null,
                SCHEMA_TYPE,ATTR_SAML_ASSERTION_VERSION,defaultValues);

            defaultValues.clear();
            String protocolVersion = (String)p.get(SAML_PROTOCOL_VERSION);
            defaultValues.add(protocolVersion);
            UpgradeUtils.setAttributeDefaultValues(SERVICE_NAME,null,
                SCHEMA_TYPE,ATTR_SAML_PROTOCOL_VERSION,defaultValues);

            String removeAssertion = (String)p.get(REMOVE_ASSERTION);
            defaultValues.clear();
            defaultValues.add(removeAssertion);
            UpgradeUtils.setAttributeDefaultValues(SERVICE_NAME,null,
                SCHEMA_TYPE,ATTR_REMOVE_ASSERTION,defaultValues);
            isSuccess = true;
        } catch (UpgradeException e) {
            UpgradeUtils.debug.error(classMethod 
                +   "Error setting i18NFileName",e);
        }
        return isSuccess;
    }

    /**
     * Post Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean postMigrateTask() {
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
}
