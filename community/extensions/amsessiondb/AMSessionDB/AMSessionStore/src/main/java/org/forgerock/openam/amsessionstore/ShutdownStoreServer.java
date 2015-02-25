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

package org.forgerock.openam.amsessionstore;

import org.forgerock.openam.amsessionstore.common.Constants;
import org.forgerock.openam.amsessionstore.common.SystemProperties;
import org.forgerock.openam.amsessionstore.resources.ShutdownResource;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.resource.ClientResource;
import static org.forgerock.openam.amsessionstore.i18n.AmsessionstoreMessages.*;

/**
 * Calls the shutdown method on the server
 * 
 * @author steve
 */
public class ShutdownStoreServer {    
    private String resourceUrl = null;
    private String username = null;
    private String password = null;
    
    public static void main(String[] argv) {
        ShutdownStoreServer dbServer = new ShutdownStoreServer();
        System.out.print(DB_START_MSG.get());
        dbServer.shutdown();
        System.out.println(DB_COMPLETE.get());
    }
    
    public ShutdownStoreServer() {
        resourceUrl = SystemProperties.get(Constants.HOST_URL);
        username = SystemProperties.get(Constants.USERNAME);
        password = SystemProperties.get(Constants.PASSWORD);
    }
    
    public void shutdown() {
        ChallengeResponse challengeResponse = null;
        String shutdownResourceUrl = resourceUrl + ShutdownResource.URI;
        ClientResource authResource = new ClientResource(shutdownResourceUrl);
        ShutdownResource shutdownResource = authResource.wrap(ShutdownResource.class);
        authResource.setChallengeResponse(ChallengeScheme.HTTP_DIGEST, username, password); 
        
        try {
            shutdownResource.shutdown();
        } catch (Exception ex) {
            // do nothing
        }
        
        ChallengeRequest c1 = null;
        for (ChallengeRequest challengeRequest : authResource
                .getChallengeRequests()) {
            if (ChallengeScheme.HTTP_DIGEST
                    .equals(challengeRequest.getScheme())) {
                c1 = challengeRequest;
                break;
            }
        }
        
        challengeResponse = new ChallengeResponse(c1, authResource.getResponse(), 
                username, password.toCharArray());
        authResource.setChallengeResponse(challengeResponse);
        ShutdownResource shutdown = authResource.wrap(ShutdownResource.class);
            
        try {
            shutdown.shutdown();
        } catch (Exception ex) {
            System.err.println(DB_SHUT_FAIL.get());

            if (authResource.getStatus().getCode() == 401) {
                System.err.println(DB_SHUT_NOAUTH.get());
            }
        }
    }
}
