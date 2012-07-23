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
 * $Id: HaDBEnv.java,v 1.2 2008/06/25 05:43:28 qcheng Exp $
 *
 */

package com.sun.identity.ha.jmqdb.client;

import java.io.File;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

public class HaDBEnv {

    private Environment sessionEnv;
    private EntityStore store;

    // Our constructor does nothing
    public HaDBEnv() {}

    // The setup() method opens the environment and store
    // for us.
    public void setup(File envHome, boolean readOnly) 
        throws DatabaseException {

        EnvironmentConfig ssEnvConfig = new EnvironmentConfig();
        StoreConfig storeConfig = new StoreConfig();

        ssEnvConfig.setReadOnly(readOnly);
        storeConfig.setReadOnly(readOnly);

        // If the environment is opened for write, then we want to be 
        // able to create the environment and entity store if 
        // they do not exist.
        ssEnvConfig.setAllowCreate(!readOnly);
        storeConfig.setAllowCreate(!readOnly);

        // Allow transactions if we are writing to the store.
        ssEnvConfig.setTransactional(!readOnly);
        storeConfig.setTransactional(!readOnly);
        ssEnvConfig.setCachePercent(50);
        ssEnvConfig.setTxnNoSync(true);

        // Open the environment and entity store
        sessionEnv = new Environment(envHome, ssEnvConfig);
        store = new EntityStore(sessionEnv, "EntityStore", storeConfig);
    }

    // Return a handle to the entity store
    public EntityStore getEntityStore() {
        return store;
    }

    // Return a handle to the environment
    public Environment getEnv() {
        return sessionEnv;
    }


    // Close the store and environment
    public void close() 
    throws DatabaseException {
        if (store != null) {
            store.close();
        }

        if (sessionEnv != null) {
            // Finally, close the store and environment.
            sessionEnv.close();

        }
    }
}

