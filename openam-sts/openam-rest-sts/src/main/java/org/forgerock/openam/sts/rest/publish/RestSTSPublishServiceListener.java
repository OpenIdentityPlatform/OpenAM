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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.publish.STSInstanceConfigStore;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.config.RestSTSInstanceModule;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.slf4j.Logger;

import javax.inject.Inject;

/**
 * This ServiceListener implementation will be registered in RestSTSInstancePublisherImpl#registerServiceListener, which
 * is called by the RestSTSSetupListener, which is called by the AMSetupServlet upon OpenAM startup (once the current
 * configuration is valid). It will add
 * rest-sts instances to the CREST router which are published at other site servers. In other words, LDAP replication
 * will insure that rest-sts-instance-configuration written to LDAP at another site's server is replicated to the current
 * server's LDAP. Yet for the rest-sts-instance to be operational, it must be hung off of the CREST router for
 * available rest-sts instances. This ServiceListener will listen for creation events for the RestSecurityTokenService,
 * and determines if the current instance has not been published locally (by asking the RestSTSInstancePublisher whether
 * the instance exists in the CREST router), and if not, publishes this instance locally with the publication variant
 * that does not write state to the SMS (as this would trigger an endless LDAP replication/update ping-pong between the
 * servers in a site deployment).
 */
public class RestSTSPublishServiceListener implements ServiceListener {
    /*
    Parameter to the RestSTSInstancePublisher#publishInstance, to indicate that this is a republish - i.e. only
    the CREST router needs to be mutated, not the SMS.
     */
    private static final boolean REPUBLISH_INSTANCE = true;
    private final RestSTSInstancePublisher instancePublisher;
    STSInstanceConfigStore<RestSTSInstanceConfig> restSTSInstanceConfigStore;
    private final Logger logger;

    @Inject
    RestSTSPublishServiceListener(RestSTSInstancePublisher instancePublisher,
                                  STSInstanceConfigStore<RestSTSInstanceConfig> restSTSInstanceConfigStore,
                                  Logger logger) {
        this.instancePublisher = instancePublisher;
        this.restSTSInstanceConfigStore = restSTSInstanceConfigStore;
        this.logger = logger;
    }

    public void schemaChanged(String serviceName, String version) {
        //do nothing - these updates not relevant
    }

    public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent, int type) {
        //do nothing - these updates not relevant
    }

    /*
    Called when the OrganizationConfig is modified. In this method, the serviceName is RestSecurityTokenService (as defined
    in restSTS.xml), and the serviceComponent corresponds to the actual name of the rest-sts instance, albeit in all
    lower-case. This is the value I want to use to determine if it has been published on another server in a site deployment
    and thus needs to be hung off of the CREST router in the current deployment.
     */
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName, String serviceComponent, int type) {
        final String logIdentifier = "RestSTSPublishServiceListener#organizationConfigChanged";
        /*
        It seems the serviceComponent is the full realm path, and always includes a '/' at the beginning, to represent
        the root realm. This value needs to be stripped-off, as the cache uses the rest-sts id, which is normalized to
        remove any leading/trailing '/' characters.
         */
        String normalizedServiceComponent = stripLeadingForwardSlash(serviceComponent);
        if (restSTSInstanceCreated(serviceName, version, type)) {
            if (!instancePublisher.isInstanceExposedInCrest(normalizedServiceComponent)) {
                String realm = DNMapper.orgNameToRealmName(orgName);
                RestSTSInstanceConfig createdInstance;
                try {
                    createdInstance = restSTSInstanceConfigStore.getSTSInstanceConfig(normalizedServiceComponent, realm);
                } catch (STSPublishException e) {
                    logger.error(logIdentifier + ":could not obtain newly created rest-sts instance " + serviceComponent + " from SMS. " +
                            "This means this instance will not be hung off of the CREST router. Exception: " + e);
                    return;
                }
                Injector instanceInjector;
                try {
                    instanceInjector = createInjector(createdInstance);
                } catch (ResourceException e) {
                    logger.error(logIdentifier + ":could not create injector corresponding to newly created rest-sts " +
                            "instance " + serviceComponent + ". The instanceConfig " + createdInstance.toJson() +
                            "\nThis means this instance will not be hung off of the CREST router. Exception: " + e);
                    return;
                }
                try {
                    instancePublisher.publishInstance(createdInstance, instanceInjector.getInstance(RestSTS.class), REPUBLISH_INSTANCE);
                    logger.info(logIdentifier + ": Successfully hung rest-sts instance " + createdInstance.getDeploymentSubPath()
                            + " published at another server in the site deployment off of CREST router.");
                } catch (ResourceException e) {
                    logger.error(logIdentifier + ":could not create injector corresponding to newly created rest-sts " +
                            "instance " + serviceComponent + ". The instanceConfig " + createdInstance.toJson() +
                            "\nThis means this instance will not be hung off of the CREST router. Exception: " + e);
                }
            }
        } else if (restSTSInstanceDeleted(serviceName, version, type)) {
            /*
            Check to see if this instance still remains in the CREST router. This could occur if:
            1. we are in a multi-server environment, and the rest-sts instance was deleted on another server
            2. somebody just nuked the realm containing published rest-sts instances.
             */
            if (instancePublisher.isInstanceExposedInCrest(normalizedServiceComponent)) {
                String realm = DNMapper.orgNameToRealmName(orgName);
                boolean removeOnlyFromRouter = true;
                try {
                    instancePublisher.removeInstance(normalizedServiceComponent, realm, removeOnlyFromRouter);
                    logger.info(logIdentifier + ": Removed rest-sts instance " + normalizedServiceComponent +
                            " from CREST router in site deployment following the deletion of this rest-sts instance on " +
                            "another site server.");
                } catch (STSPublishException e) {
                    logger.error(logIdentifier + ": Could not remove rest-sts instance " + normalizedServiceComponent +
                            " from CREST router in site deployment following the deletion of this rest-sts instance on " +
                            "another site server. Exception: " + e, e);
                }
            }
        }
    }

    private String stripLeadingForwardSlash(String unstripped) {
        if (AMSTSConstants.ROOT_REALM.equals(unstripped)) {
            return unstripped;
        }
        if (unstripped.startsWith(AMSTSConstants.FORWARD_SLASH)) {
            return unstripped.substring(1);
        }
        return unstripped;
    }

    private boolean restSTSInstanceCreated(String serviceName, String version, int type) {
        return (ServiceListener.ADDED == type)  &&
                restSTSServiceTargeted(serviceName, version);
    }

    private boolean restSTSInstanceDeleted(String serviceName, String version, int type) {
        return (ServiceListener.REMOVED == type)  &&
                restSTSServiceTargeted(serviceName, version);
    }

    private boolean restSTSServiceTargeted(String serviceName, String version) {
        return AMSTSConstants.REST_STS_SERVICE_NAME.equals(serviceName) &&
                AMSTSConstants.REST_STS_SERVICE_VERSION.equals(version);
    }

    private Injector createInjector(RestSTSInstanceConfig instanceConfig) throws ResourceException {
        try {
            return Guice.createInjector(new RestSTSInstanceModule(instanceConfig));
        } catch (Exception e) {
            String message = "Exception caught creating the guice injector corresponding to rest sts instance: " + e;
            throw new InternalServerErrorException(message, e);
        }
    }
}
