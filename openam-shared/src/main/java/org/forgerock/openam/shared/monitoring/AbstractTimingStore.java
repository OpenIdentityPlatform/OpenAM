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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.shared.monitoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An abstract store for holding a set of {@link TimingEntry}s. The maximum size of the store
 * is set on creation. The store internally uses a synchronizedList, which verifies the size of the collection
 * isn't greater than the maximum upon insertion, and removes the oldest entry if it is.
 * Read operations are not synchronized.
 */
public abstract class AbstractTimingStore {

    protected final int maxEntries;

    public AbstractTimingStore(int maxEntries) {
        if (maxEntries < 100) {
            this.maxEntries = 100;
        } else {
            this.maxEntries = maxEntries;
        }
    }

    //the duration store, limited to the number of entries specified by the configuration set by administrator
    protected final List<TimingEntry> durationStore = Collections.synchronizedList(new ArrayList<TimingEntry>() {

        @Override
        public boolean add(TimingEntry entry) {
            while (size() >= maxEntries) {
                remove(0);
            }
            return super.add(entry);
        }

    });

    /**
     * Getter for the average length of time events recorded in the duration store have taken.
     *
     * @return a long representation of the average length of time in ms.
     */
    public long getDurationAverage() {

        if (durationStore.size() == 0) {
            return 0L;
        }

        long current = 0L;

        for(TimingEntry te : durationStore) {
            current += te.getDuration();
        }

        return current / durationStore.size();

    }

    /**
     * Returns a string representation of the slowest performing policy evaluation.
     * If no policy evaluations have yet been run, this will return the empty String.
     *
     * @return representation of the slowest performing policy evaluation.
     */
    public String getSlowestEvaluation() {

        final TimingEntry te = getSlowest();

        if (te == null) {
            return "";
        }

        return te.toString();
    }

    /**
     * Returns the long representation of the slowest performing policy evaluation.
     * If no policy evaluations have yet been run, this will return 0.
     *
     * @return duration of the slowest performing policy evaluation in ms.
     */
    public long getSlowestEvaluationDuration() {

        final TimingEntry te = getSlowest();

        if (te == null) {
            return 0L;
        }

        return te.getDuration();
    }

    /**
     * Finds the slowest (longest duration) timing entry in the durationStore.
     *
     * @return the slowest {@link TimingEntry} in the durationStore, or null.
     */
    private TimingEntry getSlowest() {

        if (durationStore == null || durationStore.size() == 0) {
            return null;
        }

        TimingEntry current = durationStore.get(0);

        for (TimingEntry te : durationStore) {
            if (current.getDuration() < te.getDuration()) {
                current = te;
            }
        }

        return current;
    }

    /**
     * Returns the maximum entries for this store.
     *
     * @return the maximum number of entries this timing store will hold before removing the oldest entry
     */
    public int getMaxEntries() {
        return maxEntries;
    }
}
