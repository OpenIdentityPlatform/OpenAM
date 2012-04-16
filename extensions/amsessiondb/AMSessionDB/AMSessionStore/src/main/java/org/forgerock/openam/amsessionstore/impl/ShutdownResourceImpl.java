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

package org.forgerock.openam.amsessionstore.impl;

import java.util.logging.Level;
import org.forgerock.openam.amsessionstore.AMSessionStoreServer;
import org.forgerock.openam.amsessionstore.common.Log;
import org.forgerock.openam.amsessionstore.db.PersistentStoreFactory;
import org.forgerock.openam.amsessionstore.resources.ShutdownResource;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import static org.forgerock.openam.amsessionstore.i18n.AmsessionstoreMessages.*;

/**
 * This class implements the shutdown REST call sent by OpenAM when it is 
 * shutting down. Currently does nothing.
 * 
 * TODO: Could be clean up expired records?
 * 
 * @author steve
 */
public class ShutdownResourceImpl extends ServerResource implements ShutdownResource {
    @Get
    @Override
    public void shutdown() {
        Log.logger.log(Level.FINEST, DB_AM_SHUT.get().toString());
        
        try {
            PersistentStoreFactory.getPersistentStore().shutdown();
        } catch (Exception ex) {
            Log.logger.log(Level.WARNING, DB_AM_SHUT_FAIL.get().toString(), ex);
        }
        
        AMSessionStoreServer.stop();
    }
}
