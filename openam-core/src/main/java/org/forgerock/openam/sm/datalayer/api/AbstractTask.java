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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.cts.api.tokens.Token;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.iplanet.am.util.SystemProperties;

/**
 * Abstract task processed by the Task Processor.
 * @param <T> Connection to use.
 */
public abstract class AbstractTask<T> implements Task {
	public static Logger logger=LoggerFactory.getLogger(AbstractTask.class);

	static Integer maxSize=32000; 
	static Integer maxTtl=1000; 
	static protected Cache<String, Token> sid2token;
	static ScheduledExecutorService stats=Executors.newScheduledThreadPool(1);
	static {
		try {
			maxSize=SystemProperties.getAsInt("org.openidentityplatform.openam.token.cache.size", maxSize);
		}catch (Throwable e) {}
		try {
			maxTtl=SystemProperties.getAsInt("org.openidentityplatform.openam.token.cache.ttl", 1000);
		}catch (Throwable e) {}
		logger.info("org.openidentityplatform.openam.token.cache.size={} org.openidentityplatform.openam.token.cache.ttl={}ms org.openidentityplatform.openam.token.cache.stats={}min",maxSize,maxTtl,SystemProperties.getAsInt("org.openidentityplatform.openam.token.cache.stats", 10));
		
		sid2token=CacheBuilder.newBuilder()
				.maximumSize(maxSize)
				.expireAfterWrite(maxTtl, TimeUnit.MILLISECONDS)
				.recordStats()
				.build();
		
		stats.schedule(new Runnable() {
			@Override
			public void run() {
				logger.info("org.openidentityplatform.openam.token.cache: {}",sid2token.stats());
			}
		}, SystemProperties.getAsInt("org.openidentityplatform.openam.token.cache.stats", 10), TimeUnit.MINUTES);
	}
		
    protected final ResultHandler<T, ?> handler;
    private boolean isError = false;

    /**
     * A new abstract task constructor - requires at least a ResultHandler to be configured.
     *
     * @param handler Non null handler to notify.
     */
    public AbstractTask(ResultHandler<T, ?> handler) {
        this.handler = handler;
    }

    @Override
    public void processError(DataLayerException error) {
        isError = true;
        handler.processError(error);
    }

    @Override
    public void execute(TokenStorageAdapter adapter) throws DataLayerException {
        if (isError) {
            return;
        }

        try {
            performTask(adapter);
        } catch (DataLayerException e) {
            processError(e);
            throw e;
        }
    }

    /**
     * Performs a task.
     *
     * @param adapter Required for LDAP operations.
     * @throws DataLayerException If there was any problem creating the Token.
     */
    public abstract void performTask(TokenStorageAdapter adapter) throws DataLayerException;

}
