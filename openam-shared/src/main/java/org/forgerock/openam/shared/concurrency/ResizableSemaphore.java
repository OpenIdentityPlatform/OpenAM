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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.shared.concurrency;

import java.util.concurrent.Semaphore;

/**
 * A {@link Semaphore} implementation which performs exactly as a standard {@link Semaphore}, but supports 'resizing'
 * of the {@link Semaphore} too.
 *
 * @since 13.0.0
 */
public class ResizableSemaphore extends Semaphore {

    /**
     * {@inheritdoc}
     */
    public ResizableSemaphore(int permits, boolean fair) {
        super(permits, fair);
    }

    /**
     * {@inheritdoc}
     */
    public ResizableSemaphore(int permits) {
        super(permits);
    }

    /**
     * Overridden method to stop it being protected.
     * Decrease the number of available permits by the amount in the supplied argument.
     *
     * {@inheritdoc}
     */
    @Override
    protected void reducePermits(int reduction) {
        super.reducePermits(reduction);
    }

    /**
     * Increase the number of available permits by the amount in the supplied argument.
     *
     * @param increase The number of permits to add.
     */
    public void increasePermits(int increase) {
        this.release(increase);
    }
}
