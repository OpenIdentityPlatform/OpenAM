/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All Rights Reserved
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
 * Portions Copyrighted [2012] [ForgeRock Inc]
 *
 */
package com.sun.identity.sm;

import com.iplanet.dpro.session.exceptions.StoreException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.tools.objects.MapFormat;

import java.util.HashMap;
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
     * Static section to retrieve the debug object.
     */
    private static Debug DEBUG;

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
    // Our default Top Level Root Suffix.
    // This is resolved during Initialization process.
    private static final String BASE_ROOT_DN_NAME = "BASE_ROOT";
    private static String BASE_ROOT_DN;

    private static final String SERVICES_BASE_ROOT_DN =
            "ou" + Constants.EQUALS + "services" + Constants.COMMA +
                    "{"+ BASE_ROOT_DN_NAME +"}";

    // Example of Depicted DN:
    //dn: ou=default,ou=GlobalConfig,ou=1.0,ou=iPlanetAMSessionService,
    //       ou=services,dc=openam,dc=forgerock,dc=org
    private static final String SITE_SESSION_FAILOVER_HA_SERVICES_BASE_DN_TEMPLATE =
            "ou" + Constants.EQUALS + "default" + Constants.COMMA +
                    "ou" + Constants.EQUALS + "GlobalConfig" + Constants.COMMA +
                    "ou" + Constants.EQUALS + "1.0" + Constants.COMMA +
                    "ou" + Constants.EQUALS + "{SERVICE_NAME}" + Constants.COMMA +
                    SERVICES_BASE_ROOT_DN;

    /**
     * Default locked down constructor.
     */
    private SessionHAFailoverSetupSubConfig() {
        // Obtain the Debug instance.
        DEBUG = Debug.getInstance("Session");
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
            { throw new IllegalStateException("Illegal State Exception encountered, unable to allow Creation."); }
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
        // Establish our Base Root DN Definition.
        BASE_ROOT_DN = SMSEntry.getRootSuffix();
        if ( (BASE_ROOT_DN == null) || (BASE_ROOT_DN.isEmpty()) ) {
            return false;
        }
        // Construct our DN for the Entry.
        String baseDN = getFormattedDNString(SITE_SESSION_FAILOVER_HA_SERVICES_BASE_DN_TEMPLATE,
                "SERVICE_NAME", serviceName);
        try {
            // Construct ServiceConfigManagerImpl
            ServiceConfigManagerImpl serviceConfigManagerImpl = ServiceConfigManagerImpl.
                    getInstance(adminToken, serviceName, "1.0");
            if (serviceConfigManagerImpl == null) {
                DEBUG.error("Unable to dynamically create Site Sub-Service Configuration Element, due to unable to obtain a ServiceConfigManagerImpl");
                return false;
            }
            ServiceConfigManager serviceConfigManager = new ServiceConfigManager(serviceName, adminToken);
            if (serviceConfigManager == null) {
                DEBUG.error("Unable to dynamically create Site Sub-Service Configuration Element, due to unable to obtain a ServiceConfigManager");
                return false;
            }
            // Obtain reference to Service Schema Manager.
            ServiceSchemaManagerImpl serviceSchemaManager =
                    ServiceSchemaManagerImpl.getInstance(adminToken, serviceName, "1.0");
            if (serviceSchemaManager == null) {
                DEBUG.error("Unable to dynamically create Site Sub-Service Configuration Element, due to unable to obtain a ServiceSchemaManager");
                return false;
            }
            // Obtain reference to Service Schema.
            ServiceSchemaImpl serviceSchema = serviceSchemaManager.getSchema(SchemaType.GLOBAL);
            if (serviceSchema == null) {
                DEBUG.error("Unable to dynamically create Site Sub-Service Configuration Element, due to unable to obtain a ServiceSchemaImpl");
                return false;
            }
            // Obtain the Service Configuration Implementation.
            ServiceConfigImpl serviceConfigImpl = ServiceConfigImpl.getInstance(adminToken,
                    serviceConfigManagerImpl, serviceSchema, baseDN,
                    BASE_ROOT_DN, "default", "//" + siteName, true);
            if (serviceConfigImpl == null) {
                DEBUG.error("Unable to dynamically create Site Sub-Service Configuration Element, due to unable to obtain a ServiceConfigImpl");
                return false;
            }
            // Now Finally access the service Config where we can add the Sub-Configuration Element.
            ServiceConfig serviceConfig = new ServiceConfig(serviceConfigManager, serviceConfigImpl);
            if (serviceConfig == null) {
                DEBUG.error("Unable to dynamically create Site Sub-Service Configuration Element, due to unable to obtain a ServiceConfig");
                return false;
            }
            // Create the Session HA Failover Indicator Setting for the Specified Site and
            // Add the Sub Configuration Entry.
            serviceConfig.addSubConfig(siteName, serviceID, 0, values);

            // Tell our view Cache to update with the new Value!
            serviceConfigManagerImpl.objectChanged("ou"+EQUALS+siteName+COMMA+baseDN, ServiceListener.ADDED);
            // TODO This attempt above of poking cache should work, but does not.  Fix!!!

            // Assume Success, if we hit here.
            thisCreateServiceSubConfigHasBeenUsed = true;
            return true;
        } catch (ServiceAlreadyExistsException smsException) {
                // Does Entry Already Exists?
                // Yes, in which case, assume we were successful.
                thisCreateServiceSubConfigHasBeenUsed = true;
                return true;
        } catch (SMSException smsException) {
            throw new StoreException("Unable to Dynamically Add the Session HA SF Property for DN:["
                    + baseDN + "], SMSErrorCode: " + smsException.getExceptionCode()
                    +  "], SMSException: " + smsException.getMessage(), smsException);
        } catch (SSOException ssoException) {
            throw new StoreException("Unable to Dynamically Add the Session HA SF Property for DN:["
                    + baseDN + "], OpenAM Exception: " + ssoException.getMessage(), ssoException);
        }
    }

    /**
     * Helper method to correctly format a String with a name,value pair.
     * This uses the include Open Source @see MapFormat Source.
     *
     * @param template
     * @param name - Can be Null.
     * @param value
     * @return String of Formatted Template with DN Names resolved.
     */
    private static String getFormattedDNString(String template, String name, String value) {
        Map<String,String> map = new HashMap<String,String>();
        map.put(BASE_ROOT_DN_NAME, BASE_ROOT_DN); // Always Resolve our Base Root DN with any Template.
        if ( (name != null) && (!name.isEmpty()) )
            { map.put(name,value); }
        return MapFormat.format(template, map);
    }

}
