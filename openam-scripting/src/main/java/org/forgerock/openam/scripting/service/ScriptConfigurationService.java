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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.scripting.service;

import static org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode.*;
import static org.forgerock.openam.scripting.ScriptException.createAndLogDebug;
import static org.forgerock.openam.scripting.ScriptException.createAndLogError;
import static org.forgerock.openam.utils.Time.*;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.datastore.ScriptingDataStore;
import org.forgerock.openam.scripting.datastore.ScriptingDataStoreFactory;
import org.forgerock.util.Reject;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import java.util.Date;
import java.util.Set;

/**
 * The {@code ScriptConfigurationService} for access to the persisted {@code ScriptConfiguration}
 * instances. It is the layer on top of the {@code ScriptConfigurationDataStore}.
 *
 * @since 13.0.0
 */
public class ScriptConfigurationService implements ScriptingService {

    private final Logger logger;
    private final Subject subject;
    private final String realm;
    private final ScriptingDataStore dataStore;

    /**
     * Construct a new instance of {@code ScriptConfigurationService}.
     * @param logger The logger log any error and debug messages to.
     * @param subject The subject requesting modification to the {@code ScriptConfiguration}.
     * @param realm The realm in which the {@code ScriptConfiguration} resides in.
     * @param dataStoreFactory A factory for providing new scripting data store instances.
     */
    @Inject
    public ScriptConfigurationService(@Named("ScriptLogger") Logger logger,
                                      @Assisted Subject subject, @Assisted String realm,
                                      ScriptingDataStoreFactory dataStoreFactory) {
        Reject.ifNull(subject, realm);
        this.logger = logger;
        this.subject = subject;
        this.realm = realm;
        this.dataStore = dataStoreFactory.create(subject, realm);
    }

    @Override
    public ScriptConfiguration create(ScriptConfiguration config) throws ScriptException {
        failIfUuidExists(config.getId());
        failIfNameExists(config.getName());
        final ScriptConfiguration updatedConfig = setMetaData(config);
        dataStore.save(updatedConfig);
        return updatedConfig;
    }

    @Override
    public void delete(String uuid) throws ScriptException {
        failIfUuidDoesNotExist(uuid);
        dataStore.delete(uuid);
    }

    @Override
    public Set<ScriptConfiguration> getAll() throws ScriptException {
        return dataStore.getAll();
    }

    @Override
    public Set<ScriptConfiguration> get(QueryFilter<String> queryFilter) throws ScriptException {
        return dataStore.get(queryFilter);
    }

    @Override
    public ScriptConfiguration get(String uuid) throws ScriptException {
        failIfUuidDoesNotExist(uuid);
        return dataStore.get(uuid);
    }

    @Override
    public ScriptConfiguration update(ScriptConfiguration config) throws ScriptException {
        final ScriptConfiguration oldConfig = get(config.getId());
        if (!oldConfig.getName().equals(config.getName())) {
            failIfNameExists(config.getName());
        }
        final ScriptConfiguration updatedConfig = setMetaData(config);
        dataStore.save(updatedConfig);
        return updatedConfig;
    }

    private void failIfNameExists(String name) throws ScriptException {
        if (dataStore.containsName(name)) {
            throw createAndLogDebug(logger, SCRIPT_NAME_EXISTS, name, realm);
        }
    }

    private void failIfUuidExists(String uuid) throws ScriptException {
        if (dataStore.containsUuid(uuid)) {
            throw createAndLogError(logger, SCRIPT_UUID_EXISTS, uuid, realm);
        }
    }

    private void failIfUuidDoesNotExist(String uuid) throws ScriptException {
        if (!dataStore.containsUuid(uuid)) {
            throw createAndLogError(logger, SCRIPT_UUID_NOT_FOUND, uuid, realm);
        }
    }

    private ScriptConfiguration setMetaData(ScriptConfiguration config) throws ScriptException {
        final long now = newDate().getTime();
        final String principalName = SubjectUtils.getPrincipalId(subject);
        final ScriptConfiguration.Builder builder = config.populatedBuilder();

        if (dataStore.containsUuid(config.getId())) {
            final ScriptConfiguration oldConfig = get(config.getId());
            builder.setCreatedBy(oldConfig.getCreatedBy());
            builder.setCreationDate(oldConfig.getCreationDate());
        } else {
            builder.setCreatedBy(principalName);
            builder.setCreationDate(now);
        }
        builder.setLastModifiedBy(principalName);
        builder.setLastModifiedDate(now);

        return builder.build();
    }
}
