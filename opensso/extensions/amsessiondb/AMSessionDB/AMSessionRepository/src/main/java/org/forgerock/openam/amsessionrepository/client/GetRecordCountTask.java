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

package org.forgerock.openam.amsessionrepository.client;

import com.sun.identity.ha.FAMRecord;
import java.util.HashMap;
import java.util.Map;
import org.forgerock.openam.amsessionstore.common.AMRecord;
import org.forgerock.openam.amsessionstore.resources.GetRecordCountResource;
import org.restlet.Client;
import org.restlet.data.ChallengeResponse;
import org.restlet.resource.ClientResource;

/**
 *
 * @author steve
 */
public class GetRecordCountTask extends AbstractTask {
    private String pKey = null;
    private String sKey = null;
    
    public GetRecordCountTask(Client client,
                              String resourceURL, 
                              String username, 
                              String password, 
                              String pKey, 
                              String recordToRead) {
        super(client, resourceURL, username, password);
        
        this.pKey = pKey;
        this.sKey = recordToRead;
    }
    
    @Override
    public AMRecord call() 
    throws Exception {
        ChallengeResponse response = getAuth();
        ClientResource resource = 
                new ClientResource(resourceURL + GetRecordCountResource.URI + SLASH + sKey);
        resource.setNext(client);
        resource.setChallengeResponse(response);
        GetRecordCountResource getRecordCountResource = resource.wrap(GetRecordCountResource.class);

        Map<String, Long> sessions = null;
        
        try {
            sessions = getRecordCountResource.getRecordCount();
        } catch (Exception ex) {
            if (resource.getStatus().getCode() != 401) {
                if (debug.warningEnabled()) {
                    debug.warning("Unable to get record count from amsessiondb", ex);
                }
                
                throw ex;
            }
            
            clearAuth();
            response = getAuth();
            resource.setChallengeResponse(response);
            
            try {
                sessions = getRecordCountResource.getRecordCount();
            } catch (Exception ex2) {
                if (resource.getStatus().getCode() == 401) {
                    if (debug.warningEnabled()) {
                        debug.warning("Unable to get record count from amsessiondb; unauthorized", ex2);
                    }
                    
                    throw new UnauthorizedException(ex2.getMessage());
                } else {
                    if (debug.warningEnabled()) {
                        debug.warning("Unable to get record count from amsessiondb", ex2);
                    }
                    throw ex2;
                }
            }
        }

        AMRecord record = new AMRecord();
        record.setOperation(FAMRecord.GET_RECORD_COUNT);
        record.setPrimaryKey(pKey);

        Map<String, String> newMap = new HashMap<String, String>();

        if (sessions != null) {
            for (Map.Entry<String, Long> entry : sessions.entrySet()) {
                newMap.put(entry.getKey(), entry.getValue().toString());
            }

            record.setExtraStringAttrs(newMap);
        } else {
            if (debug.warningEnabled()) {
                debug.warning("unable to get record count");
            }
        }
        
        if (debug.messageEnabled()) {
            if (sessions != null) {
                debug.message("Get Record Count for " + sKey + " size " + sessions.size());
            } else {
                debug.message("Get Record Count for " + sKey + " no results");
            }
        }
        
        return record;
    }
    
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(GetRecordCountTask.class).append(": pkey=").append(sKey);
        
        return output.toString();
    }
}
