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

import com.sun.identity.ha.FAMRecordUtils;
import com.sun.identity.shared.debug.Debug;
import java.util.concurrent.Callable;
import org.forgerock.openam.amsessionstore.common.AMRecord;
import org.forgerock.openam.amsessionstore.resources.ConfigResource;
import org.restlet.Client;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.resource.ClientResource;

/**
 * Abstract class to the session persister tasks
 * 
 * TODO: Would be nice to have a retry count and be able to put failed tasks
 * back in the queue to be retried.
 * 
 * @author steve
 */
public abstract class AbstractTask implements Callable<AMRecord> {
    protected static Debug debug = null;
    protected String resourceURL = null;
    protected Client client = null;
    protected String username = null;
    protected String password = null;
    protected static ChallengeResponse authResponse = null;
    
    public static final String SLASH = "/";
    
    static {
        initialize();
    }
    
    private static void initialize() {
        debug = FAMRecordUtils.debug;
    }
    
    protected AbstractTask(Client client, 
                           String resourceUrl, 
                           String username, 
                           String password) {
        this.client = client;
        this.resourceURL = resourceUrl;
        this.username = username;
        this.password = password;
    }
        
    protected synchronized ChallengeResponse getAuth() {
        if (authResponse != null) {
            return authResponse;
        }
                
        ClientResource authRes = new ClientResource(resourceURL + ConfigResource.URI);
        authRes.setNext(client);
        authRes.setChallengeResponse(ChallengeScheme.HTTP_DIGEST, "login", "test");
        
        try {
            authRes.get();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
        
        if (debug.messageEnabled()) {
            debug.message("Fetching Authentication Context; Initial Request: " + authRes.getStatus());
        }
        
        ChallengeRequest c1 = null;
        
        for (ChallengeRequest challengeRequest : authRes.getChallengeRequests()) {
            if (ChallengeScheme.HTTP_DIGEST.equals(challengeRequest.getScheme())) {
                c1 = challengeRequest;
                break;
            }
        }
        
        authResponse = new ChallengeResponse(c1, authRes.getResponse(), username, password.toCharArray());
        authRes.setChallengeResponse(authResponse);
        
        try {
            authRes.get();
        } catch (Exception ex) {
            debug.error("Unable to establish authentication", ex);
        }
        
        if (debug.messageEnabled()) {
            debug.message("Fetching Authentication Context; Second Request: " + authRes.getStatus());
        }
                
        return authResponse;
    }
    
    public synchronized void clearAuth() {
        authResponse = null;
    }
    
    @Override
    public abstract AMRecord call() throws Exception;
}
