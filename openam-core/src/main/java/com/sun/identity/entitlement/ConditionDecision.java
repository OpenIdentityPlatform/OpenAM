/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConditionDecision.java,v 1.3 2009/09/05 00:24:04 veiming Exp $
 *
 * Portions copyright 2010-2015 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class to represent {@link EntitlementCondition} evaluation match result and - if applicable - its advices.
 *
 * @supported.all.api
 */
public class ConditionDecision {

    private final Map<String, Set<String>> responseAttributes;
    private final Map<String, Set<String>> advices;
    private final long timeToLive;
    private final boolean satisfied;


    /**
     * Constructs an instance of <code>ConditionDecision</code>.
     * <p/>
     * Deprecated, favour the factory methods.
     *
     * @param satisfied
     *         result of this <code>ConditionDecision</code>.
     * @param advices
     *         Advice map of this <code>ConditionDecision</code>.
     */
    @Deprecated
    public ConditionDecision(boolean satisfied, Map<String, Set<String>> advices) {
        this(satisfied, advices, Long.MAX_VALUE);
    }

    /**
     * Constructs an instance of <code>ConditionDecision</code>.
     * <p/>
     * Deprecated, favour the factory methods.
     *
     * @param satisfied
     *         Result of this <code>ConditionDecision</code>.
     * @param advices
     *         Advice map of this <code>ConditionDecision</code>.
     * @param ttl
     *         The TTL of this <code>ConditionDecision</code>.
     */
    @Deprecated
    public ConditionDecision(boolean satisfied, Map<String, Set<String>> advices, long ttl) {
        this.satisfied = satisfied;
        this.advices = new HashMap<>(advices);
        this.responseAttributes = new HashMap<>();
        this.timeToLive = ttl;
    }

    private ConditionDecision(Builder builder) {
        satisfied = builder.satisfied;
        advices = builder.advices;
        responseAttributes = builder.responseAttributes;
        timeToLive = builder.timeToLive;
    }

    /**
     * Whether this <code>ConditionDecision</code> is satisfied.
     *
     * @return <code>true</code> if <code>ConditionDecision</code> is fulfilled.
     */
    public boolean isSatisfied() {
        return satisfied;
    }

    /**
     * Advices associated with this <code>ConditionDecision</code>.
     *
     * @return advices of <code>ConditionDecision</code>.
     */
    public Map<String, Set<String>> getAdvices() {
        return Collections.unmodifiableMap(advices);
    }

    /**
     * Retrieves the response attributes.
     *
     * @return the response attributes
     */
    public Map<String, Set<String>> getResponseAttributes() {
        return Collections.unmodifiableMap(responseAttributes);
    }

    /**
     * Returns the time to live (TTL) of this <code>ConditionDecision</code>.
     *
     * @return The TTL time in ms.
     */
    public long getTimeToLive() {
        return timeToLive;
    }

    /**
     * Clears the current advices associated with this <code>ConditionDecision</code>.
     * <p/>
     * Deprecated method as a given instance should be immutable.
     */
    @Deprecated
    public void clearAdvices() {
        advices.clear();
    }

    /**
     * Adds an advice (from another <code>ConditionDecision</code>) to this <code>ConditionDecision</code>.
     * <p/>
     * Deprecated method as a given instance should be immutable.
     *
     * @param decision
     *         The <code>ConditionDecision</code> whose advices should be added to this
     *         <code>ConditionDecision</code>.
     */
    @Deprecated
    public void addAdvices(ConditionDecision decision) {
        if (decision != null) {
            Map<String, Set<String>> otherAdvices = decision.getAdvices();
            if (otherAdvices != null && !otherAdvices.isEmpty()) {
                advices.putAll(otherAdvices);
            }
        }
    }

    /**
     * New decision builder.
     *
     * @param satisfied
     *         whether the decision represents a successful evaluation or not
     *
     * @return new builder instance
     */
    public static Builder newBuilder(boolean satisfied) {
        return new Builder(satisfied);
    }

    /**
     * New builder representing a satisfied.
     *
     * @return new builder instance
     */
    public static Builder newSuccessBuilder() {
        return new Builder(true);
    }

    /**
     * New builder representing a failure.
     *
     * @return new builder instance
     */
    public static Builder newFailureBuilder() {
        return new Builder(false);
    }

    /**
     * Builder to help construct decisions.
     */
    public static final class Builder {

        private final boolean satisfied;
        private Map<String, Set<String>> advices;
        private Map<String, Set<String>> responseAttributes;
        private long timeToLive;

        private Builder(boolean success) {
            this.satisfied = success;
            advices = new HashMap<>();
            responseAttributes = new HashMap<>();
            timeToLive = Long.MAX_VALUE;
        }

        /**
         * Sets the advices.
         *
         * @param advices
         *         the advices
         *
         * @return this builder instance
         */
        public Builder setAdvices(Map<String, Set<String>> advices) {
            Reject.ifNull(advices);
            this.advices = advices;
            return this;
        }

        /**
         * Sets the response attributes.
         *
         * @param responseAttributes
         *         the response attributes
         *
         * @return this builder instance
         */
        public Builder setResponseAttributes(Map<String, Set<String>> responseAttributes) {
            Reject.ifNull(responseAttributes);
            this.responseAttributes = responseAttributes;
            return this;
        }

        /**
         * Sets the time to live.
         *
         * @param timeToLive
         *         the time to live
         *
         * @return this builder instance
         */
        public Builder setTimeToLive(long timeToLive) {
            Reject.ifTrue(timeToLive < 0);
            this.timeToLive = timeToLive;
            return this;
        }

        /**
         * Builds the decision.
         *
         * @return the decision instance
         */
        public ConditionDecision build() {
            return new ConditionDecision(this);
        }

    }

}
