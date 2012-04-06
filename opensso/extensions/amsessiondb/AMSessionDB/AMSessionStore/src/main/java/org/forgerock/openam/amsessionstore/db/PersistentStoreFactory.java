/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.amsessionstore.db;

import java.util.logging.Level;
import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.openam.amsessionstore.common.Constants;
import org.forgerock.openam.amsessionstore.common.Log;
import org.forgerock.openam.amsessionstore.common.SystemProperties;
import static org.forgerock.openam.amsessionstore.i18n.AmsessionstoreMessages.*;

/**
 * Singleton used to fetch the shared instance of the persistent store.
 * 
 * The persistent store is pluggable and two implementations are provided.
 * 
 * <ui>
 * <li>Memory
 * <li>OrientDB
 * </ul>
 * 
 * The OrientDB implementation is the default.
 * 
 * @author steve
 */
public class PersistentStoreFactory {
    private static PersistentStore persistentStore = null; 
    private static String persistentStoreImpl = null; 
    private static final String DEFAULT_PERSISTER_VALUE = 
        "org.forgerock.openam.amsessionstore.db.memory.MemoryPersistentStore";
    
    static {
        try {
            initialize();
        } catch (Exception e) {
            persistentStoreImpl = DEFAULT_PERSISTER_VALUE;
        }         
    }
    
    private static void initialize() 
    throws Exception {
        persistentStoreImpl = SystemProperties.get(Constants.PERSISTER_KEY,
                DEFAULT_PERSISTER_VALUE);
        final LocalizableMessage message = DB_PER_CONF.get(persistentStoreImpl);
        Log.logger.log(Level.FINE, message.toString());
    }
    
    /**
     * Return the singleton instance of the configured persistent store
     * 
     * @return The persistent store instance
     * @throws Exception 
     */
    public synchronized static PersistentStore getPersistentStore() 
    throws Exception {
        if (persistentStore == null) {
            try {
                persistentStore = (PersistentStore) Class.forName(
                persistentStoreImpl).newInstance(); 
                Log.logger.log(Level.FINE, DB_PER_CREATE.get().toString());
            } catch (Exception ex) {
                Log.logger.log(Level.SEVERE, DB_PER_FAIL.get().toString(), ex);
                throw ex;
            }
        }
        
        return persistentStore; 
    }
}
