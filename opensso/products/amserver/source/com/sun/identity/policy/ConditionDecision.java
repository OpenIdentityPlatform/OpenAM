/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConditionDecision.java,v 1.2 2008/06/25 05:43:43 qcheng Exp $
 *
 */



package com.sun.identity.policy;

import java.util.Map;
import java.util.HashMap;

/**
 * The <code>ConditionDecision</code> class represents the result of  
 * the evaluation of a condition. 
 *
 * @supported.all.api
 */
public class ConditionDecision {

    private boolean allowed;
    private Map advices = new HashMap();
    private long timeToLive = Long.MAX_VALUE;

    /** No argument constructor */
    public ConditionDecision() {
    }

    /** Constructs <code>ConditionDecision</code> given the boolean result of 
     *  a condition evaluation
     *
     * @param allowed boolean result of a condition evaluation
     */
    public ConditionDecision(boolean allowed) {
        this.allowed = allowed;
    }

    /** Constructs <code>ConditionDecision</code> given the boolean result of
     *  a condition evaluation and  advices 
     *
     * @param allowed boolean result of a condition evaluation
     * @param advices A <code>Map</code> representing advices associated with 
     *     this <code>ConditionDecision</code>.
     *     The advice name is the key to the Map. The
     *     value is a <code>Set</code> of advice message Strings corresponding 
     *     to the advice name.
     *     The advice name  examples are 
     *     SessionCondition.SESSION_CONDITION_ADVICE
     *     AuthSchemeCondition.AUTH_SCHEME_CONDITION_ADVICE
     */
    public ConditionDecision(boolean allowed, Map advices) {
        this.allowed = allowed;
        this.advices = advices;
    }

    /** Constructs <code>ConditionDecision</code> given the boolean result of 
     *  a condition evaluation and time to live
     *
     * @param allowed boolean result of a condition evaluation
     * @param timeToLive GMT time in milliseconds since epoch when 
     *     this object is to be treated as expired. 
     */
    public ConditionDecision(boolean allowed, long timeToLive) {
        this.allowed = allowed;
        this.timeToLive = timeToLive;
    }

    /** Constructs <code>ConditionDecision</code> given the boolean result of 
     *  a condition evaluation, time to live and advices
     *
     * @param allowed boolean result of a condition evaluation
     * @param timeToLive GMT time in milliseconds since epoch when 
     *     this object is to be treated as expired. 
     * @param advices advices associated with this action decision. 
     *     The advice name is the key to the Map. The
     *     value is a set of advice message Strings corresponding to the 
     *     advice name.
     *     The advice name  examples are 
     *     SessionCondition.SESSION_CONDITION_ADVICE
     *     AuthSchemeCondition.AUTH_SCHEME_CONDITION_ADVICE
     */
    public ConditionDecision(boolean allowed, long timeToLive, Map advices) {
        this.allowed = allowed;
        this.timeToLive = timeToLive;
        this.advices = advices;
    }

    /** Sets boolean result of condition evaluation
     *
     * @param allowed boolean result of condition evaluation
     */
    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    /** Gets boolean result of condition evaluation
     *
     * @return result of condition evaluation
     */
    public boolean isAllowed() {
        return allowed;
    }

    /** Sets advices associated with this object 
     *
     * @param advices A <code>Map</code> representing advices associated with 
     *     this object.
     *     The advice name is the key to the Map. The
     *     value is a <code>Set</code> of advice message Strings corresponding 
     *     to the  advice name.
     *     The advice name  examples are 
     *     SessionCondition.SESSION_CONDITION_ADVICE
     *     AuthSchemeCondition.AUTH_SCHEME_CONDITION_ADVICE
     */
    public void setAdvices(Map advices) {
        this.advices = advices;
    }

    /** Gets advices associated with this object 
     *
     * @return advices associated with this object.
     *     The advice name is the key to the <code>Map</code>. The
     *     value is a <code>Set</code> of advice message Strings corresponding 
     *     to the advice name.
     */
    public Map getAdvices() {
        return advices;
    }

    /** Sets <code>timeToLive</code> associated with this object 
     *
     * @param timeToLive GMT time in milliseconds since epoch when 
     *     this object is to be treated as expired. 
     */
    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    /** Gets <code>timeToLive</code> associated with this object 
     *
     * @return GMT time in milliseconds since epoch when 
     *     this object is to be treated as expired. 
     */
    public long getTimeToLive() {
        return timeToLive;
    }
}
