/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.authentication.callbacks;

import javax.security.auth.callback.Callback;

/**
 * Class representative of a PollingWaitCallback Callback Object which instructs a client to wait for the given period
 * and then resubmit their request.
 */
public class PollingWaitCallback implements Callback {

    /** The public name of this callback object. */
    public static final String NODE_NAME = "PollingWaitCallback";

    /** The period of time in milliseconds that the client should wait before replying to this callback. */
    private final String waitTime;

    /**
     * Constructor for creating this Callback
     * @param waitTime the wait time for this PollingWaitCallback
     */
    public PollingWaitCallback(String waitTime) {
        this.waitTime = waitTime;
    }

    /**
     * Private Constructor for use with the builder.
     * @param builder the PollingWaitCallbackBuilder to use
     */
    private PollingWaitCallback(PollingWaitCallbackBuilder builder) {
        this.waitTime = builder.waitTime;
    }

    /**
     * Gets the wait time associated with this callback.
     * @return the wait time in milliseconds to use as a String.
     */
    public String getWaitTime() {
        return waitTime;
    }

    /**
     * Gets a new PollingWaitCallbackBuilder to use for construction of a PollingWaitCallback.
     * @returna new PollingWaitCallbackBuilder object.
     */
    public static PollingWaitCallbackBuilder makeCallback() {
        return new PollingWaitCallbackBuilder();
    }

    /**
     * Builder object for the PollingWaitCallback
     */
    public static class PollingWaitCallbackBuilder {

        private String waitTime;

        /**
         * Set the wait time for the new PollingWaitCallback Object.
         * @param waitTime the waitTime to use
         * @return this builder
         */
        public PollingWaitCallbackBuilder withWaitTime(String waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        /**
         * Sets the default values to use for the new PollingWaitCallback from an existing PollingWaitCallback Object.
         * @param callback the callback to copy
         * @return this builder
         */
        public PollingWaitCallbackBuilder asCopyOf(PollingWaitCallback callback) {
            this.waitTime = callback.getWaitTime();
            return this;
        }

        /**
         * finalise the construction process and return the completed PollingWaitCallback Object.
         * @return a new PollingWaitCallback object
         */
        public PollingWaitCallback build() {
            return new PollingWaitCallback(this);
        }
    }
}
