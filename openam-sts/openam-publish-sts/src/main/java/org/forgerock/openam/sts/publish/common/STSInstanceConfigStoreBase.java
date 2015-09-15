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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.publish.common;

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
import org.forgerock.openam.sts.InstanceConfigMarshaller;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.openam.sts.publish.STSInstanceConfigStore;
import org.slf4j.Logger;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @see org.forgerock.openam.sts.publish.STSInstanceConfigStore
 * This class represents the concerns of accessing the SMS to persist and obtain the configuration state corresponding
 * to rest and soap sts instances - i.e. access to the state corresponding to RestSTSInstanceConfig and SoapSTSInstanceConfig
 * instances.
 */
public abstract class STSInstanceConfigStoreBase<T extends STSInstanceConfig> implements STSInstanceConfigStore<T>{
    private static final int PRIORITY_ZERO = 0;

    private final InstanceConfigMarshaller<T> instanceConfigMarshaller;
    private final String serviceName; //either AMSTSConstants.REST_STS_SERVICE_NAME or AMSTSContants.SOAP_STS_SERVICE_NAME
    private final Logger logger;

    public STSInstanceConfigStoreBase(InstanceConfigMarshaller<T> instanceConfigMarshaller, String serviceName, Logger logger) {
        this.instanceConfigMarshaller = instanceConfigMarshaller;
        this.serviceName = serviceName;
        this.logger = logger;
    }

    /**
     * Persists the STS instance into the SMS.
     * @param stsInstanceId the identifier for the to-be-published sts instance
     * @param realm The realm in which the sts instance should be deployed
     * @param instance The to-be-persisted state.
     * @throws STSPublishException if the SMS encounters a problem during persistence.
     */
    @Override
    public void persistSTSInstance(String stsInstanceId, String realm, T instance) throws STSPublishException {
        /*
          Note on having to explicitly specify the realm as a parameter, when it could, theoretically, be obtained from the T instance parameter:
          although both the RestSTSInstanceConfig and the SoapSTSInstanceConfig have a DeploymentConfig reference, it is not defined
          in STSInstanceConfig (which would allow it to be referenced in this method), because the SoapSTSInstanceConfig class
          encapsulates a DeploymentConfig subclass, the SoapDeploymentConfig, as some additional deployment information is
          required for a soap deployment. I don't want to declare the DeploymentConfig base in the STSInstanceConfig class, as this
          would require an explicit down-cast in the SoapSTSInstanceConfig, and I don't want to add some generic complexity to
          the STSInstanceConfig class to model DeploymentConfig subclasses - the builder hierarchy in the STSInstanceConfig
          hierarchy is already complicated enough. So the realm parameter is added explicitly, as the calling context knows
          whether it is dealing with a soap or rest sts instance.
         */
        try {
            /*
            Model for code below taken from AMAuthenticationManager.createAuthenticationInstance, as the 'multiple authN module per realm'
            model applies to the STS, and the AMAuthenticationManager seems to implement the SMS persistence concern of these semantics.
             */
            OrganizationConfigManager organizationConfigManager =
                    new OrganizationConfigManager(getAdminToken(), realm);
            Map<String, Set<String>> instanceConfigAttributes = instanceConfigMarshaller.toMap(instance);

            if (!organizationConfigManager.getAssignedServices().contains(serviceName)) {
                organizationConfigManager.assignService(serviceName, null);
            }
            ServiceConfig orgConfig = organizationConfigManager.getServiceConfig(serviceName);
            if (orgConfig == null) {
                orgConfig = organizationConfigManager.addServiceConfig(serviceName, null);
            }
            orgConfig.addSubConfig(stsInstanceId, ISAuthConstants.SERVER_SUBSCHEMA,
                    PRIORITY_ZERO, instanceConfigAttributes);
            if (logger.isDebugEnabled()) {
                logger.debug("Persisted " + restOrSoap() + " sts instance with id " + stsInstanceId + " in realm " + realm);
            }

        } catch (SMSException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught persisting " + restOrSoap() + " instance " + stsInstanceId + "Exception: " + e, e);
        } catch (SSOException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught persisting " + restOrSoap() + " instance" + stsInstanceId + "Exception: " + e, e);
        }
    }

    /**
     * Removes the sts instance corresponding to the id from the SMS.
     * @param stsInstanceId The id of the to-be-removed sts instance.
     * @param realm the realm in which the sts instance was deployed. Necessary for SMS lookup.
     * @throws STSPublishException if the SMS encounters an error during the removal.
     */
    @Override
    public synchronized void removeSTSInstance(String stsInstanceId, String realm) throws STSPublishException {
        /*
        Model for code below taken from AMAuthenticationManager.deleteAuthenticationInstance, as the 'multiple authN module per realm'
        model applies to the STS, and the AMAuthenticationManager seems to implement the SMS persistence concern of these semantics.
         */
        ServiceConfig baseService;
        try {
            baseService = new ServiceConfigManager(serviceName,
                    getAdminToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                baseService.removeSubConfig(stsInstanceId);
                if (logger.isDebugEnabled()) {
                    logger.debug(restOrSoap() + "sts instance " + stsInstanceId + " in realm " + realm
                            + " removed from persistent store.");
                }
            } else {
                throw new STSPublishException(ResourceException.NOT_FOUND,
                        "Could not create ServiceConfigManager for realm " + realm +
                                " in order to remove " + restOrSoap() + " sts instance with id " + stsInstanceId);
            }
        } catch (SMSException | SSOException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught removing " + restOrSoap() + " sts instance with id " + stsInstanceId + " from realm "
                            + realm +". Exception: " + e, e);
        }
    }

    /**
     * Updates attributes corresponding to the existing STS instance.
     * @param stsInstanceId the identifier for the to-be-updated sts instance
     * @param realm The realm in which the sts instance should be deployed
     * @param instance The updated STSInstanceConfig subclass
     * @throws STSPublishException if the SMS encounters a problem during persistence.
     */
    @Override
    public synchronized void updateSTSInstance(String stsInstanceId, String realm, T instance) throws STSPublishException {
        /*
        Model for code below taken from AuthPropertiesModelImpl#setValues
         */
        ServiceConfig baseService;
        try {
            baseService = new ServiceConfigManager(serviceName,
                    getAdminToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                ServiceConfig serviceConfig = baseService.getSubConfig(stsInstanceId);
                if (serviceConfig != null) {
                    serviceConfig.setAttributes(instanceConfigMarshaller.toMap(instance));
                    logger.debug(restOrSoap() + "sts instance " + stsInstanceId + " in realm " + realm
                            + " updated in persistent store.");
                } else {
                    throw new STSPublishException(ResourceException.NOT_FOUND,
                            "Could not create ServiceConfig for realm " + realm +
                                    " in order to update " + restOrSoap() + " sts instance with id " + stsInstanceId);
                }
            } else {
                throw new STSPublishException(ResourceException.NOT_FOUND,
                        "Could not create ServiceConfigManager for realm " + realm +
                                " in order to update " + restOrSoap() + " sts instance with id " + stsInstanceId);
            }
        } catch (SMSException | SSOException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught updating " + restOrSoap() + " sts instance with id " + stsInstanceId + " in realm "
                            + realm +". Exception: " + e, e);
        }
    }


    /**
     * Returns the STSInstanceConfig subclass corresponding to the stsInstanceId parameter.
     * @param stsInstanceId the identifier of the sts
     * @param realm the realm in which the sts instance was deployed. Necessary for SMS lookup.
     * @return the SoapSTSInstanceConfig or RestSTSInstanceConfig instance corresponding to this published instance.
     * @throws STSPublishException if no instance could be found, or if the SMS encountered an error during the lookup.
     */
    @Override
    @SuppressWarnings("unchecked")
    public T getSTSInstanceConfig(String stsInstanceId, String realm) throws STSPublishException {
        try {
            /*
            Model for code below taken from AMAuthenticationManager.getAuthenticationInstance, as the 'multiple authN module per realm'
            model applies to the STS, and the AMAuthenticationManager seems to implement the SMS persistence concern of these semantics.
             */
            ServiceConfig baseService = new ServiceConfigManager(serviceName,
                    getAdminToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                ServiceConfig instanceService = baseService.getSubConfig(stsInstanceId);
                if (instanceService != null) {
                    Map<String, Set<String>> instanceAttrs = instanceService.getAttributes();
                    return instanceConfigMarshaller.fromMapAttributes(instanceAttrs);
                } else {
                    throw new STSPublishException(ResourceException.NOT_FOUND,
                            "Error reading " + restOrSoap() + " sts instance from SMS: no instance state in realm " + realm
                                    + " corresponding to instance id " + stsInstanceId);
                }
            } else {
                throw new STSPublishException(ResourceException.NOT_FOUND,
                        "Error reading " + restOrSoap() + " sts instance from SMS: no base instance state in realm " + realm);
            }
        } catch (SSOException | SMSException | IllegalStateException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "Exception caught reading " + restOrSoap() + " sts instance from SMS: " + e, e);
        }
    }

    /**
     * @return A non-null, but possibly empty list containing all of the STSInstanceConfig subclasses corresponding to
     * published rest or soap sts instances.
     * @throws STSPublishException if the realm names, necessary to obtain the sts instances published in each, could not
     * be obtained.
     */
    @Override
    public List<T> getAllPublishedInstances() throws STSPublishException {
        List<T> instances = new ArrayList<>();
        for (String realm : getAllRealmNames()) {
            try {
                instances.addAll(getPublishedInstances(realm));
            } catch (STSPublishException e) {
                logger.error("Could not obtain " + restOrSoap() + " sts instances for realm " + realm +
                        ". Instances for this realm cannot be returned from getAllPublishedInstances. "
                        + "Exception: " + e, e);
                continue;
            }
        }
        return instances;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getPublishedInstances(String realm) throws STSPublishException {
        List<T> instances = new ArrayList<>();
        ServiceConfig baseService;
        try {
            baseService = new ServiceConfigManager(serviceName,
                    getAdminToken()).getOrganizationConfig(realm, null);
        } catch (SMSException | SSOException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR, "Could not obtain ServiceConfig instance for realm " + realm +
                    "." + restOrSoap() + " sts instances for this realm cannot be returned from getAllPublishedInstances(String realm). "
                    + "Exception: " + e, e);
        }
        if (baseService != null) {
            Set<String> subConfigNames;
            try {
                subConfigNames = baseService.getSubConfigNames();
            } catch (SMSException e) {
                throw new STSPublishException(ResourceException.INTERNAL_ERROR, "Could not get list of " + restOrSoap() +
                        "sts instances in realm " + realm + ". Exception: " + e, e);
            }
            for (String stsInstanceId : subConfigNames) {
                ServiceConfig instanceService;
                try {
                    instanceService = baseService.getSubConfig(stsInstanceId);
                } catch (SSOException | SMSException e) {
                    logger.error("Could not get " + restOrSoap() + " sts state for id " + stsInstanceId + " in realm "
                            + realm + ". Exception: " + e);
                    continue;
                }
                if (instanceService != null) {
                    Map<String, Set<String>> instanceAttrs = instanceService.getAttributes();
                    try {
                        instances.add(instanceConfigMarshaller.fromMapAttributes(instanceAttrs));
                    } catch (STSPublishException e) {
                        logger.error("Exception caught in getAllPublishedInstances(String realm) marshalling attributes " +
                                "corresponding to sts " + stsInstanceId +" in realm + " + realm + "; Exception: " + e, e);
                    }
                } else {
                    logger.error("Could not obtain the " + restOrSoap() + " sts state for instance with id " + stsInstanceId
                            + " in realm " + realm);
                }
            }
        } else {
            logger.error("Could not obtain ServiceConfig instance for realm " + realm +
                    "." + restOrSoap() + " sts instances for this realm cannot be returned from getAllPublishedInstances.");
        }
        return instances;
    }

    @Override
    public boolean isInstancePresent(String stsId, String realm) throws STSPublishException {
        try {
            ServiceConfig baseService = new ServiceConfigManager(serviceName,
                    getAdminToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                return baseService.getSubConfig(stsId) != null;
            } else {
                return false;
            }
        } catch (SSOException e) {
            throw new STSPublishException(ResourceException.NOT_FOUND,
                    "Exception caught reading "  + restOrSoap() + " sts instance " + stsId + " from SMS: " + e, e);
        } catch (SMSException e) {
            throw new STSPublishException(ResourceException.NOT_FOUND,
                    "Exception caught reading " + restOrSoap() + " sts instance " + stsId + "from SMS: " + e, e);
        }
    }

    private Set<String> getAllRealmNames() throws STSPublishException {
        Set<String> realmNames = new HashSet<>();
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
                            "This means list of previously-published " + restOrSoap() + " sts instances cannot be returned. "
                            + "Exception: " + e);
        }

    }

    @SuppressWarnings("unchecked")
    private Set<String> getSubrealms(Set<String> currentRealms) throws SMSException {
        Set<String> subrealms = new HashSet<>();
        for (String realm : currentRealms) {
            OrganizationConfigManager ocm = new OrganizationConfigManager(getAdminToken(), realm);
            subrealms.addAll(catenateRealmNames(realm, ocm.getSubOrganizationNames()));
        }
        return subrealms;
    }

    private Set<String> catenateRealmNames(String currentRealm, Set<String> subrealms) {
        Set<String> catenatedNames = new HashSet<>(subrealms.size());
        if (AMSTSConstants.ROOT_REALM.equals(currentRealm)) {
            catenatedNames.addAll(subrealms);
        } else {
            for (String subrealm : subrealms) {
                catenatedNames.add(currentRealm + AMSTSConstants.FORWARD_SLASH + subrealm);
            }
        }
        return catenatedNames;
    }

    private String restOrSoap() {
        return (AMSTSConstants.SOAP_STS_SERVICE_NAME.equals(serviceName) ? "soap" : "rest");
    }

    private SSOToken getAdminToken()  {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }
}
