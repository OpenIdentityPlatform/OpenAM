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
import java.util.Set;
import java.util.Vector;
import org.forgerock.openam.amsessionstore.common.AMRecord;
import org.forgerock.openam.amsessionstore.resources.ReadWithSecKeyResource;
import org.restlet.Client;
import org.restlet.data.ChallengeResponse;
import org.restlet.resource.ClientResource;

/**
 *
 * @author steve
 */
public class ReadWithSecKeyTask extends AbstractTask {
    private String pKey = null;
    private String sKey = null;
    
    public ReadWithSecKeyTask(Client client,
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
                new ClientResource(resourceURL + ReadWithSecKeyResource.URI + SLASH + sKey);
        resource.setNext(client);
        resource.setChallengeResponse(response);
        ReadWithSecKeyResource secReadResource = resource.wrap(ReadWithSecKeyResource.class);

        Set<String> records = null;
        
        try { 
            records = secReadResource.readWithSecKey();
        } catch (Exception ex) {
            if (resource.getStatus().getCode() != 401) {
                if (debug.warningEnabled()) {
                    debug.warning("Unable to read skey on amsessiondb", ex);
                }
                
                throw ex;
            }
            
            clearAuth();
            response = getAuth();
            resource.setChallengeResponse(response);
            
            try {
                records = secReadResource.readWithSecKey();
            } catch (Exception ex2) {
                if (resource.getStatus().getCode() == 401) {
                    if (debug.warningEnabled()) {
                        debug.warning("Unable to read skey on amsessiondb; unauthorized", ex2);
                    }
                    
                    throw new UnauthorizedException(ex2.getMessage());
                } else {
                    if (debug.warningEnabled()) {
                        debug.warning("Unable to read skey on amsessiondb", ex2);
                    }
                    throw ex2;
                }
            }
        }
        
        AMRecord record = new AMRecord();
        record.setOperation(FAMRecord.READ_WITH_SEC_KEY);
        record.setPrimaryKey(pKey);

        @SuppressWarnings("UseOfObsoleteCollectionType")
        Vector<String> blobs = new Vector<String>();

        for (String session : records) {
            blobs.add(session);
        }
        
        record.setRecords(blobs);

        if (debug.messageEnabled()) {
            debug.message("read with sec key: " + sKey + " found: " + blobs);
        }
        
        return record;
    }
    
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(ReadTask.class).append(": pkey=").append(sKey);
        
        return output.toString();
    }    
}
