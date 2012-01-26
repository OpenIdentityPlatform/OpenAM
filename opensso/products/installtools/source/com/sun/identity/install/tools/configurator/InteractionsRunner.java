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
 * $Id: InteractionsRunner.java,v 1.2 2008/06/25 05:51:21 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.sun.identity.install.tools.util.Debug;

final class InteractionsRunner extends InteractionsRunnerBase {

    InteractionsRunner(InstallRunInfo installRunInfo,
            UserResponseHandler uHandler) throws InstallException {
        super(installRunInfo, uHandler);
    }

    private int getInstanceInteractionsOffset() {
        return instanceInteractionsOffset;
    }

    public IStateAccess getStateAccess() {
        return iStateAccess;
    }

    public void setStateAccessDataType(int index) {
        if (index < getInstanceInteractionsOffset()) { // Common interactions
            ((PersistentStateAccess) getStateAccess()).setCommonDataFlag(true);
        } else { // Instance interactions
            ((PersistentStateAccess) getStateAccess()).setCommonDataFlag(
                    false);
        }
    }

    public void setStateAccess(PersistentStateAccess newStateAccess) {
        if (newStateAccess != null) { // If you are here
            // NOTE: newStateAccess must be having NEW instance finder data and
            // if the previous stateAccess is not null, then it needs to
            // updated with new instance finder data.
            PersistentStateAccess oldStateAccess = 
                (PersistentStateAccess) getStateAccess();
            Debug.log("InteractionsRunner.setStateAccess() - newStateAccess"
                    + " is" + newStateAccess + "\n Old StateAccess is:"
                    + oldStateAccess);

            if (oldStateAccess != null) { // Not first time.
                // StateAccess has already been set. So update it to the
                // new one now. Best way is to use the newStateAccess, add the
                // data corresponding to all the previously executed (common
                // + instance) interactions from oldStateAccess ignoring the
                // specified keys (updated instance Finder keys).

                // NOTE: The getAllInteractionKeys() will return only the
                // common + instance interactions that have been excecuted so
                // far in the (previous attempt).
                newStateAccess.copyMissingData(oldStateAccess);
            }
        }
        // Finally modify iStateAccess to newStateAccess
        iStateAccess = newStateAccess;
        Debug.log("InteractionsRunner.setStateAccess() - finally StateAccess"
                + " is: " + getStateAccess());
    }

    public void createAllInteractions(InstallRunInfo installRunInfo)
            throws InstallException {
        // Note initInteractions will also populate all configured
        // interaction keys defined in the configuration XML file.
        ArrayList cInteractionsInfo = installRunInfo.getCommonInteractions();
        if (InstallState.isFreshInstall()) {
            // Fresh install intialize the common interactions
            initInteractions(cInteractionsInfo);
            setInstanceInteractionsOffset(cInteractionsInfo.size());
        } else {
            // We need to explicitly add the config keys for common 
            // interactions these will be needed when creating the response 
            // file. In the above if clause, they are stored by the init
            // Interactions method.
            storeCommonInteractionKeys(cInteractionsInfo);
            setInstanceInteractionsOffset(0);
        }
        // Initialize the instance interactions
        ArrayList iInteractionsInfo = installRunInfo.getInstanceInteractions();
        initInteractions(iInteractionsInfo);
    }

    private void storeCommonInteractionKeys(ArrayList cInteractionsInfo)
            throws InstallException {
        int count = cInteractionsInfo.size();
        for (int i = 0; i < count; i++) {
            UserDataInteraction interaction = InteractionFactory
                    .createInteraction((InteractionInfo) cInteractionsInfo
                            .get(i));
            getAllConfiguredInteractionKeys().add(interaction.getKey());
        }
    }

    public void initInteractions(ArrayList iInteractionsInfo)
            throws InstallException {
        // Intialize the Set's before their usage methods are invoked by super
        // class
        setNonPersistentCommonKeys(new HashSet());
        setNonPersistentInstanceKeys(new HashSet());
        super.initInteractions(iInteractionsInfo);
    }

    public void storeNonPersistentKeys(String key, int index) {
        if (index < getInstanceInteractionsOffset()) { // Common interactions
            getNonPersistentCommonKeys().add(key);
        } else { // Instance interactions
            getNonPersistentInstanceKeys().add(key);
        }
    }

    public void clear() {
        super.clear();
        setStateAccess(null);
    }

    public Set getNonPersistentCommonKeys() {
        return nonPersistentCommonKeys;
    }

    public Set getNonPersistentInstanceKeys() {
        return nonPersistentInstanceKeys;
    }

    public void setNonPersistentCommonKeys(Set set) {
        nonPersistentCommonKeys = set;
    }

    public void setNonPersistentInstanceKeys(Set set) {
        nonPersistentInstanceKeys = set;
    }

    private void setInstanceInteractionsOffset(int offset) {
        instanceInteractionsOffset = offset;
    }

    private int instanceInteractionsOffset;

    // iStateAccess is initialized to null => as a means to determine, first
    // time run or a re-run of interactions.
    private IStateAccess iStateAccess;

    private Set nonPersistentCommonKeys;

    private Set nonPersistentInstanceKeys;
}
