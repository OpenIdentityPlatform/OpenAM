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
 * FvChallengeBean.java
 *
 * Created on 2007/09/20, 21:11 
 * @author yasushi.iwakata@sun.com
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.authentication.modules.fvauth;
/**
 * Stores ChallengeId/Challenge pair.
 */
public class FvChallengeBean {
    private long FvChallengeId = 0;
    private long FvChallenge = 0;
    
    /**
     * Returns <code>FvChallenge</code>.
     *
     * @return <code>FvChallenge</code>
     */
    public long getFvChallenge() {
        return FvChallenge;
    }
    /**
     * Returns <code>FvChallengeId</code>.
     *
     * @return <code>FvChallengeId</code>
     */    
    public long getFvChallengeId() {
        return FvChallengeId;
    }
    /**
     * @param challenge to be sent to auth client
     */    
    public void setFvChallenge(long challenge) {
        this.FvChallenge = challenge;
    }
     /**
     * @param challengeId to be sent to auth client
     */   
    public void setFvChallengeId(long challengeId) {
        this.FvChallengeId = challengeId;
    }
}