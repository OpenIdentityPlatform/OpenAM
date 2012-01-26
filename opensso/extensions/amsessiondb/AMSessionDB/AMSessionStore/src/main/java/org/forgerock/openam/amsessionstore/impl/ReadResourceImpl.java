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

/**
 * Implements the read resource functionality
 * 
 * @author steve
 */

import org.forgerock.i18n.LocalizableMessage;
import java.util.logging.Level;
import org.forgerock.openam.amsessionstore.common.AMRecord;
import org.forgerock.openam.amsessionstore.common.Log;
import org.forgerock.openam.amsessionstore.db.NotFoundException;
import org.forgerock.openam.amsessionstore.db.PersistentStoreFactory;
import org.forgerock.openam.amsessionstore.db.StoreException;
import org.forgerock.openam.amsessionstore.resources.ReadResource;
import org.forgerock.openam.amsessionstore.shared.Statistics;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import static org.forgerock.openam.amsessionstore.i18n.AmsessionstoreMessages.*;

public class ReadResourceImpl extends ServerResource implements ReadResource {
    @Get
    @Override
    public AMRecord read() {
        String id = (String) getRequest().getAttributes().get(ReadResource.PKEY_PARAM);
        AMRecord record = null;
        long startTime = 0;
        
        if (Statistics.isEnabled()) {
            startTime = System.currentTimeMillis();
        }
        
        try {
            record = PersistentStoreFactory.getPersistentStore().read(id);
        } catch (StoreException sex) {
            final LocalizableMessage message = DB_R_READ.get(sex.getMessage());
            Log.logger.log(Level.WARNING, message.toString());
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, message.toString());
        } catch (NotFoundException nfe) {
            final LocalizableMessage message = DB_R_READ.get(nfe.getMessage());
            Log.logger.log(Level.WARNING, message.toString());
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, message.toString());
        } catch (Exception ex) {
            final LocalizableMessage message = DB_R_READ.get(ex.getMessage());
            Log.logger.log(Level.WARNING, message.toString());
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, message.toString());
        }
        
        if (Statistics.isEnabled()) {
            Statistics.getInstance().incrementTotalReads();
            
            if (startTime != 0) {
                Statistics.getInstance().updateReadTime(System.currentTimeMillis() - startTime);    
            }
        }
        
        return record;
    }
}
