/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
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
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010] [ForgeRock AS]
 *
 */
package com.sun.identity.sm;

import com.iplanet.dpro.session.exceptions.StoreException;
import com.iplanet.dpro.session.service.AMSessionRepository;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

import java.util.Map;

/**
 * SessionHAFailoverSetupSubConfig
 * <p/>
 * This utility helper class provides the necessary
 * process methods to perform the simple construction
 * and persistence of the configuration entry
 * to set the proper sub-configuration element
 * for Site and Session HA Failover.
 * <p/>
 *
 * This utility is only called a single time and should only be called
 * once within the lifetime of an OpenAM instance install.
 *
 * @author jeff.schenk@forgerock.com
 */
public class SessionHAFailoverSetupSubConfig implements Constants {

    /**
     * Define usage constants after install.
     * As soon as this class' method createServiceSubConfig is
     * used and successful, it will no longer be allowed to be called by any
     * component or layer. Safety Measure.
     *
     * When OpenAM instance is not during install phase/mode,
     * then this class will never be loaded.
     *
     */
    private static boolean thisCreateServiceSubConfigHasBeenUsed = false;

    /**
     * Global Constants
     */
    public static final String AM_SESSION_SERVICE = "iPlanetAMSessionService";
    private static final String DEFAULT_SITE_SERVICE_ID = "Site";

    /**
     * Instantiate Singleton Static Interface.
     */
    private static SessionHAFailoverSetupSubConfig instance =
            new SessionHAFailoverSetupSubConfig();

    /**
     * Define Session DN Constants
     */
    private static final String SM_CONFIG_ROOT_DN =
            SystemPropertiesManager.get(AMSessionRepository.SYS_PROPERTY_SM_CONFIG_ROOT_SUFFIX,
                    Constants.DEFAULT_ROOT_SUFFIX);

    private static final String SERVICES_BASE_ROOT_DN =
            "ou" + Constants.EQUALS + "services" + Constants.COMMA +
                    SM_CONFIG_ROOT_DN;

    // Example of Depicted DN:
    //dn: ou=default,ou=GlobalConfig,ou=1.0,ou=iPlanetAMSessionService,
    //       ou=services,dc=openam,dc=forgerock,dc=org
    private static final String SITE_SESSION_FAILOVER_HA_SERVICES_BASE_DN_TEMPLATE =
            "ou" + Constants.EQUALS + "default" + Constants.COMMA +
                    "ou" + Constants.EQUALS + "GlobalConfig" + Constants.COMMA +
                    "ou" + Constants.EQUALS + "1.0" + Constants.COMMA +
                    "ou" + Constants.EQUALS + "%1" + Constants.COMMA +
                    SERVICES_BASE_ROOT_DN;

    /**
     * Default locked down constructor.
     */
    private SessionHAFailoverSetupSubConfig() {
    }

    /**
     * Provide a Singleton Static instance.
     *
     * @return SessionHAFailoverSetupSubConfig Instance.
     */
    public static SessionHAFailoverSetupSubConfig getInstance() {
        return instance;
    }

    /**
     * Creates a new Site Sub configuration specifically for Session HA FO.
     *
     * @param siteName    Name of sub configuration, in our case the Site Name.
     * @param serviceName Name of the Specific Service.
     * @param values      Map of attribute name to its values.
     * @return boolean Indicates True, if method was successful or not.
     * @throws IllegalStateException - when this method was already run successfully.
     * @throws StoreException - when down stream Persistent store exception occurs.
     */
    public static synchronized boolean createSessionHAFOSubConfigEntry(SSOToken adminToken, String siteName,
                                                                       String serviceName, Map values)
            throws StoreException, IllegalStateException {
        if (thisCreateServiceSubConfigHasBeenUsed)
        { throw new IllegalStateException(""); }
        return createServiceSubConfig(adminToken, siteName, DEFAULT_SITE_SERVICE_ID, serviceName, values);
    }

    /**
     * Private Helper Method to perform the convoluted instantiation steps
     * to simple persist a single configuration element sub-entry.
     * Yikes.  Someday we will need to revisit how we abstract out a property based
     * configuration persistent store.
     *
     */
    private static synchronized boolean createServiceSubConfig(SSOToken adminToken, String siteName, String serviceID,
                                                               String serviceName, Map values) throws StoreException {
        boolean successful;
        String baseDN = SITE_SESSION_FAILOVER_HA_SERVICES_BASE_DN_TEMPLATE.
                replace("%1", serviceName);
        try {
            // Construct ServiceConfigManagerImpl
            ServiceConfigManagerImpl serviceConfigManagerImpl = ServiceConfigManagerImpl.
                    getInstance(adminToken, serviceName, "1.0");
            ServiceConfigManager serviceConfigManager = new ServiceConfigManager(serviceName, adminToken);
            // Obtain reference to Service Schema Manager.
            ServiceSchemaManagerImpl serviceSchemaManager =
                    ServiceSchemaManagerImpl.getInstance(adminToken, serviceName, "1.0");
            // Obtain reference to Service Schema.
            ServiceSchemaImpl serviceSchema = serviceSchemaManager.getSchema(SchemaType.GLOBAL);
            // Obtain the Service Configuration Implementation.
            ServiceConfigImpl serviceConfigImpl = ServiceConfigImpl.getInstance(adminToken,
                    serviceConfigManagerImpl, serviceSchema, baseDN,
                    SM_CONFIG_ROOT_DN, "default", "//" + siteName, true);
            // Now Finally access the service Config where we can add the Sub-Configuration Element.
            ServiceConfig serviceConfig = new ServiceConfig(serviceConfigManager, serviceConfigImpl);
            // Create the Session HA Failover Indicator Setting for the Specified Site and
            // Add the Sub Configuration Entry.
            serviceConfig.addSubConfig(siteName, serviceID, 0, values);
            // TODO Neither of these attempts below of poking cache work. Fix.
            // Tell our view Cache to update with the new Value!
            serviceConfigManagerImpl.objectChanged("ou"+EQUALS+siteName+COMMA+baseDN, ServiceListener.ADDED);
            // Attempt to Force it!
            serviceConfigManagerImpl.allObjectsChanged();
            // Assume Success, if we hit here.
            successful = true;
        } catch (SMSException smsException) {
            throw new StoreException("Unable to Dynamically Add the Session HA SF Property for DN:["
                    + baseDN + "], SMSException: " + smsException.getMessage(), smsException);
        } catch (SSOException ssoException) {
            throw new StoreException("Unable to Dynamically Add the Session HA SF Property for DN:["
                    + baseDN + "], SSO Exception: " + ssoException.getMessage(), ssoException);
        }
        // Set if we can allow this to be used again or not.
        thisCreateServiceSubConfigHasBeenUsed = successful;
        // return our indicator.
        return successful;
    }

}
