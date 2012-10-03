/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
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
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */

package org.forgerock.openam.session.ha.amsessionstore.store.plugin;

import com.iplanet.dpro.session.service.AMSessionRepository;
import com.sun.identity.common.GeneralTaskRunnable;
import org.forgerock.openam.session.model.*;

/**
 * Provide easy Plug-In Implementation of AMSessionRepository using a custom
 * driven external or experimental solution.
 *
 * This allows for the Session Management and Replication Aspects to be
 * in a secondary instance.
 *
 * @author steve
 * @author jeff.schenk@forgerock.com
 */
public abstract class PlugInPersistentStore extends GeneralTaskRunnable implements AMSessionRepository {



    /**
     * Shut down the store
     */
    @Override
    public void shutdown() {
        // TODO -- Implement
    }

    /**
     * Returns the current set of store statistics.
     *
     * @return DBStatistics
     */
    @Override
    public DBStatistics getDBStatistics() {
        return null;
    }

    /**
     * Adds an element to this TaskRunnable.
     *
     * @param key Element to be added to this TaskRunnable
     * @return a boolean to indicate whether the add success
     */
    @Override
    public boolean addElement(Object key) {
        return false;
    }

    /**
     * Removes an element from this TaskRunnable.
     *
     * @param key Element to be removed from this TaskRunnable
     * @return A boolean to indicate whether the remove success
     */
    @Override
    public boolean removeElement(Object key) {
        return false;
    }

    /**
     * Indicates whether this TaskRunnable is empty.
     *
     * @return A boolean to indicate whether this TaskRunnable is empty
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * Returns the run period of this TaskRunnable.
     *
     * @return A long value to indicate the run period
     */
    @Override
    public long getRunPeriod() {
        return 0;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
    }


}
