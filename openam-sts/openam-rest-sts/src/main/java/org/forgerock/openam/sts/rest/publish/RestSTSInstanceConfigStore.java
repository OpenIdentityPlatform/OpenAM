/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.publish;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.MapMarshaller;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.publish.STSInstanceConfigStore;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @see org.forgerock.openam.sts.publish.STSInstanceConfigStore
 */
public class RestSTSInstanceConfigStore implements STSInstanceConfigStore<RestSTSInstanceConfig> {
    private static final int PRIORITY_ZERO = 0;

    private final MapMarshaller<RestSTSInstanceConfig> instanceConfigMapMarshaller;
    private final Logger logger;

    @Inject
    RestSTSInstanceConfigStore(MapMarshaller<RestSTSInstanceConfig> instanceConfigMapMarshaller, Logger logger) {
        this.instanceConfigMapMarshaller = instanceConfigMapMarshaller;
        this.logger = logger;
    }

    public synchronized void persistSTSInstance(String stsInstanceId, RestSTSInstanceConfig instance) throws STSPublishException {
        try {
            /*
            Model for code below taken from AMAuthenticationManager.createAuthenticationInstance, as the 'multiple authN module per realm'
            model applies to the STS, and the AMAuthenticationManager seems to implement the SMS persistence concern of these semantics.
             */
            OrganizationConfigManager organizationConfigManager =
                    new OrganizationConfigManager(getAdminToken(), instance.getDeploymentConfig().getRealm());
            Map<String, Set<String>> instanceConfigAttributes = instanceConfigMapMarshaller.marshallAttributesToMap(instance);

            if (!organizationConfigManager.getAssignedServices().contains(AMSTSConstants.REST_STS_SERVICE_NAME)) {
                organizationConfigManager.assignService(AMSTSConstants.REST_STS_SERVICE_NAME, null);
            }
            ServiceConfig orgConfig = organizationConfigManager.getServiceConfig(AMSTSConstants.REST_STS_SERVICE_NAME);
            if (orgConfig == null) {
                orgConfig = organizationConfigManager.addServiceConfig(AMSTSConstants.REST_STS_SERVICE_NAME, null);
            }
            orgConfig.addSubConfig(stsInstanceId, ISAuthConstants.SERVER_SUBSCHEMA,
                    PRIORITY_ZERO, instanceConfigAttributes);
            if (logger.isDebugEnabled()) {
                logger.debug("Persisted sts instance with id " + stsInstanceId + " in realm " + instance.getDeploymentConfig().getRealm());
            }

        } catch (SMSException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught persisting RestSTSInstanceConfig instance " + stsInstanceId + "Exception: " + e, e);
        } catch (SSOException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught persisting RestSTSInstanceConfig instance" + stsInstanceId + "Exception: " + e, e);
        }
    }

    public synchronized void removeSTSInstance(String stsInstanceId, String realm) throws STSPublishException {
        /*
        Model for code below taken from AMAuthenticationManager.deleteAuthenticationInstance, as the 'multiple authN module per realm'
        model applies to the STS, and the AMAuthenticationManager seems to implement the SMS persistence concern of these semantics.
         */
        ServiceConfig baseService;
        try {
            baseService = new ServiceConfigManager(AMSTSConstants.REST_STS_SERVICE_NAME,
                    getAdminToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                baseService.removeSubConfig(stsInstanceId);
                if (logger.isDebugEnabled()) {
                    logger.debug("REST STS instance " + stsInstanceId + " in realm " + realm + " removed from persistent store.");
                }
            } else {
                throw new STSPublishException(ResourceException.NOT_FOUND,
                        "Could not create ServiceConfigManager for realm " + realm +
                                " in order to remove Rest STS instance with id " + stsInstanceId);
            }
        } catch (SMSException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught removing Rest STS instance with id " + stsInstanceId + " from realm "
                            + realm +". Exception: " + e, e);
        } catch (SSOException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught removing Rest STS instance with id " + stsInstanceId + " from realm "
                            + realm +". Exception: " + e, e);
        }
    }

    public RestSTSInstanceConfig getSTSInstanceConfig(String stsInstanceId, String realm) throws STSPublishException {
        try {
            /*
            Model for code below taken from AMAuthenticationManager.getAuthenticationInstance, as the 'multiple authN module per realm'
            model applies to the STS, and the AMAuthenticationManager seems to implement the SMS persistence concern of these semantics.
             */
            ServiceConfig baseService = new ServiceConfigManager(AMSTSConstants.REST_STS_SERVICE_NAME,
                    getAdminToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                ServiceConfig instanceService = baseService.getSubConfig(stsInstanceId);
                if (instanceService != null) {
                    Map<String, Set<String>> instanceAttrs = instanceService.getAttributes();
                    return RestSTSInstanceConfig.marshalFromAttributeMap(instanceAttrs);
                } else {
                    throw new STSPublishException(ResourceException.NOT_FOUND,
                            "Error reading RestSTSInstanceConfig instance from SMS: no instance state in realm " + realm
                                    + " corresponding to instance id " + stsInstanceId);
                }
            } else {
                throw new STSPublishException(ResourceException.NOT_FOUND,
                        "Error reading RestSTSInstanceConfig instance from SMS: no base instance state in realm " + realm);
            }
        } catch (SSOException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught reading RestSTSInstanceConfig instance from SMS: " + e, e);
        } catch (SMSException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught reading RestSTSInstanceConfig instance from SMS: " + e, e);
        } catch (IllegalStateException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught reading RestSTSInstanceConfig instance from SMS: " + e, e);
        }
    }

    public List<RestSTSInstanceConfig> getAllPublishedInstances() throws STSPublishException {
        List<RestSTSInstanceConfig> instances = new ArrayList<RestSTSInstanceConfig>();
        for (String realm : getAllRealmNames()) {
            ServiceConfig baseService;
            try {
                baseService = new ServiceConfigManager(AMSTSConstants.REST_STS_SERVICE_NAME,
                        getAdminToken()).getOrganizationConfig(realm, null);
            } catch (SMSException e) {
                logger.error("Could not obtain ServiceConfig instance for realm " + realm +
                        ". Rest STS instances for this realm cannot be returned from getAllPublishedInstances. " +
                        "Exception: " + e);
                continue;
            } catch (SSOException e) {
                logger.error("Could not obtain ServiceConfig instance for realm " + realm +
                        ". Rest STS instances for this realm cannot be returned from getAllPublishedInstances. " +
                        "Exception: " + e);
                continue;
            }
            if (baseService != null) {
                Set<String> subConfigNames;
                try {
                    subConfigNames = baseService.getSubConfigNames();
                } catch (SMSException e) {
                    logger.error("Could not get list of RestSTSInstances in realm " + realm + ". Exception: " + e);
                    continue;
                }
                for (String stsInstanceId : subConfigNames) {
                    ServiceConfig instanceService;
                    try {
                        instanceService = baseService.getSubConfig(stsInstanceId);
                    } catch (SSOException e) {
                        logger.error("Could not get RestSTSInstance state for id " + stsInstanceId + " in realm " + realm + ". Exception: " + e);
                        continue;
                    } catch (SMSException e) {
                        logger.error("Could not get RestSTSInstance state for id " + stsInstanceId + " in realm " + realm + ". Exception: " + e);
                        continue;
                    }
                    if (instanceService != null) {
                        Map<String, Set<String>> instanceAttrs = instanceService.getAttributes();
                        //TODO - if the marshalFromAttributeMap throws a STSPublishException, I should catch it here, log and continue.
                        instances.add(RestSTSInstanceConfig.marshalFromAttributeMap(instanceAttrs));
                    } else {
                        logger.error("Could not obtain the RestSTSInstanceConfig state for instance with id " + stsInstanceId + " in realm " + realm);
                    }
                }
            } else {
                logger.error("Could not obtain ServiceConfig instance for realm " + realm +
                        ". Rest STS instances for this realm cannot be returned from getAllPublishedInstances.");
            }
        }
        return instances;
    }

    public boolean isInstancePresent(String stsId, String realm) throws STSPublishException {
        try {
            ServiceConfig baseService = new ServiceConfigManager(AMSTSConstants.REST_STS_SERVICE_NAME,
                    getAdminToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                return baseService.getSubConfig(stsId) != null;
            } else {
                return false;
            }
        } catch (SSOException e) {
            throw new STSPublishException(ResourceException.NOT_FOUND,
                    "Exception caught reading RestSTSInstanceConfig instance " + stsId + " from SMS: " + e, e);
        } catch (SMSException e) {
            throw new STSPublishException(ResourceException.NOT_FOUND,
                    "Exception caught reading RestSTSInstanceConfig instance " + stsId + "from SMS: " + e, e);
        }
    }

    private Set<String> getAllRealmNames() throws STSPublishException {
        Set<String> realmNames = new HashSet<String>();
        /*
        The OrganizationConfigManager#SubOrganizationNames only returns realms under the root realm. The root
        realm needs to be added separately
         */
        realmNames.add(AMSTSConstants.ROOT_REALM);
        try {
            Set<String> subRealms = getSubrealms(realmNames);
            while (!subRealms.isEmpty()) {
                realmNames.addAll(subRealms);
                subRealms = getSubrealms(subRealms);
            }
            return realmNames;
        } catch (SMSException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Could not obtain list of realms from the OrganizationConfigManager. " +
                            "This means list of previously-published rest sts instances cannot be returned. Exception: " + e);
        }

    }

    private Set<String> getSubrealms(Set<String> currentRealms) throws SMSException {
        Set<String> subrealms = new HashSet<String>();
        for (String realm : currentRealms) {
            OrganizationConfigManager ocm = new OrganizationConfigManager(getAdminToken(), realm);
            subrealms.addAll(catenateRealmNames(realm, ocm.getSubOrganizationNames()));
        }
        return subrealms;
    }

    private Set<String> catenateRealmNames(String currentRealm, Set<String> subrealms) {
        Set<String> catenatedNames = new HashSet<String>(subrealms.size());
        if (AMSTSConstants.ROOT_REALM.equals(currentRealm)) {
            catenatedNames.addAll(subrealms);
        } else {
            for (String subrealm : subrealms) {
                catenatedNames.add(currentRealm + AMSTSConstants.FORWARD_SLASH + subrealm);
            }
        }
        return catenatedNames;
    }
    private SSOToken getAdminToken()  {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }
}
