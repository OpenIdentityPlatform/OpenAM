package org.forgerock.openam.sts.tokengeneration.saml2;

import com.sun.identity.sm.ServiceListener;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.openam.sts.publish.STSInstanceConfigStore;
import org.forgerock.openam.sts.rest.ServiceListenerRegistration;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.STSInstanceStateProvider
 * Base class for providing the TokenGenerationService with STSInstanceState subclasses corresponding to published rest or
 * soap sts instance state.
 * Caches state pulled from the persistent store for performance. Registers a ServiceListener in the ctor so that
 * cache entries can be invalidated when the corresponding service is updated.
 */
public abstract class STSInstanceStateProviderBase<S extends STSInstanceConfig, T extends STSInstanceState> implements STSInstanceStateProvider<T> {
    private final ConcurrentHashMap<String, T> cachedRestInstanceConfigState;
    private final STSInstanceConfigStore<S> stsInstanceConfigStore;
    private final STSInstanceStateFactory<T, S> instanceStateFactory;
    private final String serviceName;
    private final Logger logger;

    STSInstanceStateProviderBase(STSInstanceConfigStore<S> restStsInstanceStore,
                                 STSInstanceStateFactory<T, S> instanceStateFactory,
                                 ServiceListenerRegistration serviceListenerRegistration,
                                 ServiceListener serviceListener,
                                 String serviceName,
                                 String serviceVersion,
                                 Logger logger) {
        cachedRestInstanceConfigState = new ConcurrentHashMap<String, T>();
        this.stsInstanceConfigStore = restStsInstanceStore;
        this.instanceStateFactory = instanceStateFactory;
        this.serviceName = serviceName;
        this.logger = logger;
        /*
        Add the ServiceListener when the caching layer is initialized - i.e. in this ctor.
         */
        try {
            serviceListenerRegistration.registerServiceListener(serviceName,
                    serviceVersion, serviceListener);
            logger.debug("In STSInstanceStateProviderBase ctor, successfully added ServiceListener for service "
                    + serviceName);
        } catch (STSInitializationException e) {
            final String message = "Exception caught registering " + restOrSoap() +
                    " ServiceListener in the STSInstanceStateProviderBase: " + e;
            logger.error(message, e);
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR, message, e);
        }
    }

    public T getSTSInstanceState(String instanceId, String realm) throws TokenCreationException, STSPublishException {
        /*
        The instanceId can include upper-case characters. Yet the id of the rest-sts instance, as indicated to registered
        ServiceListeners, is all lower-case. So the entry in the map should be always be lower-case.
         */
        final String lowerCaseInstanceId = instanceId.toLowerCase();
        T cachedState = cachedRestInstanceConfigState.get(lowerCaseInstanceId);
        if (cachedState == null) {
            T createdState = createSTSInstanceState(instanceId, realm);
            T concurrentlyCreatedState;
            if ((concurrentlyCreatedState = cachedRestInstanceConfigState.putIfAbsent(lowerCaseInstanceId, createdState)) != null) {
                return concurrentlyCreatedState;
            } else {
                return createdState;
            }
        } else {
            return cachedState;
        }
    }

    public void invalidateCachedEntry(String instanceId) {
        if (cachedRestInstanceConfigState.remove(instanceId) != null) {
            logger.debug("In STSInstanceStateProviderBase, removed cached " + restOrSoap() +
                    " instance state for instance " + instanceId);
        }
    }

    private T createSTSInstanceState(String instanceId, String realm) throws TokenCreationException, STSPublishException {
        logger.debug("Creating " + restOrSoap() + " sts instance state for instanceId: " + instanceId);
        return instanceStateFactory.createSTSInstanceState(stsInstanceConfigStore.getSTSInstanceConfig(instanceId, realm));
    }

    private String restOrSoap() {
        return AMSTSConstants.SOAP_STS_SERVICE_NAME.equals(serviceName) ? "soap" : "rest";
    }
}
