/* The contents of this file are subject to the terms
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
 * FvValidationProxy.java
 *
 * Created on 2007/09/20, 21:11 
 * @author yasushi.iwakata@sun.com
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.authentication.modules.fvauth;

import java.net.URL;
import javax.xml.namespace.QName;
import jp.hitachisoft.aug.AuthService;
import jp.hitachisoft.aug.AuthServiceSoap;
import jp.hitachisoft.aug.ChallengeSet;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import java.util.ResourceBundle;
import java.io.IOException;

/**
 * Singleton class used to communicate with AuthService.
 */
public class FvValidationProxy {

    private static FvValidationProxy prox_inst = null;
    private static final String AM_FV_AUTH = "amFvAuth";
    private static Object lockObj = new Object();
    private AuthServiceSoap port = null;
    private String domainName = null;

    /**
     * Creates <code>FvValidationProxy</code> object.
     *
     * @throws IOException if input/output error occurs.
     */
    private FvValidationProxy() throws IOException {
        URL wsdlLocation;
        QName serviceName;
        AuthService authservice;

        AMResourceBundleCache amCache = AMResourceBundleCache.getInstance();
        ResourceBundle bundle = amCache.getResBundle(AM_FV_AUTH,
                java.util.Locale.getDefault());
        wsdlLocation = new URL(bundle.getString("am.fvauth.wsdlLocation"));
        serviceName = new QName(bundle.getString("am.fvauth.targetNamespace"),
                bundle.getString("am.fvauth.serviceName"));
        domainName = bundle.getString("am.fvauth.domainName");
        authservice = new AuthService(wsdlLocation, serviceName);
        port = authservice.getAuthServiceSoap12();

    }

    /**
     * Get the singleton instance of <code>FvValidation Proxy</code>.
     * Double-checked locking is used; the inner check is rarely necessary.
     * 
     * @return <code>FvValidationProxy</code>.
     * @throws IOException if input/output error occurs.
     */
    public static FvValidationProxy getInstance() throws IOException {
        /* Outer check */
        if (prox_inst != null) {
            return prox_inst;
        }
        synchronized (lockObj) {
            /* Inner check -- Rarely enter this block */
            if (prox_inst == null) {
                prox_inst = new FvValidationProxy();
            }
        }
        return prox_inst;
    }

    /**
     * Returns <code>FvChallengeBean</code>.
     *
     * @return <code>FvChallengeBean</code>.
     */
    public FvChallengeBean getChallenge() {
        FvChallengeBean challenge = new FvChallengeBean();
        ChallengeSet cset = port.issueChallenge();
        challenge.setFvChallenge(cset.getChallenge());
        challenge.setFvChallengeId(cset.getChallengeId());
        return challenge;
    }
    
    /**
     * Returns the authentication result (0:success, 1:auth failed, -1: server 
     * error).
     *
     * @param accountName user account name.
     * @param challengeId ID of the challenge.
     * @param authData data sent from FV device.
     * @param operationType always 0.
     * @return <code>int</code> based on verification.
     */
    public int verify(String accountName, long challengeId,
            String authData, int operationType) {
        return port.verify(domainName, accountName, challengeId, authData,
                operationType);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        FvValidationProxy vproxy;
        FvChallengeBean cbean;
        int authResult;

        System.out.println("##################################");
        try {
            vproxy = FvValidationProxy.getInstance();
            cbean = vproxy.getChallenge();
            System.out.println(cbean.getFvChallenge());
            System.out.println(cbean.getFvChallengeId());
            authResult = vproxy.verify("test002", cbean.getFvChallengeId(), "1212123", 0);
            System.out.println("authResult:" + authResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
