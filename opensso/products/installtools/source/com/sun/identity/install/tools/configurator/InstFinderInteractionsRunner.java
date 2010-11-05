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
 * $Id: InstFinderInteractionsRunner.java,v 1.2 2008/06/25 05:51:19 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.sun.identity.install.tools.admin.ToolsConfiguration;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

final class InstFinderInteractionsRunner extends InteractionsRunnerBase
        implements InstallConstants {

    InstFinderInteractionsRunner(InstallRunInfo installRunInfo,
            UserResponseHandler uHandler, ServerLocatorHandler handler)
            throws InstallException {
        super(installRunInfo, uHandler);
        setNonPersistentKeys(new HashSet());
        // At this time there is no state. So create a TransientStateAccess
        setStateAccess(new TransientStateAccess());
        setServerHomeConfigHandler(handler);
    }

    public void runInteractions(boolean startFromLast) throws InstallException 
    {
        // Run the interactions defined in the base class
        super.runInteractions(startFromLast);

        // Check the status of the final InteractionResultStatus
        InteractionResultStatus status = getFinalStatus();

        // Determine the Server Home directory & Create InstallState only when
        // the status is continue
        if (status != null && status.getIntValue() == 
            InteractionResultStatus.INT_STATUS_CONTINUE) {
            // If a previous installation is found verify the integrity of
            // Product related information in the server locator file and the
            // state files in product home's data directory
            String productHome = getServerHomeConfigHandler().getProductHome(
                    getStateAccess());
            if ((productHome != null) && 
                    (InstallState.getInstanceCount() == 0)) 
            {
                // Some thing wrong with the installation
                Debug.log("InstanceInteractionsRunner : Error - A valid "
                        + ToolsConfiguration.getProductShortName()
                        + " home was found in locator file '"
                        + getServerHomeConfigHandler().getProductLocatorFile()
                        + "'" + ". But no state information is present at '"
                        + productHome + "'.");
                throw new InstallException(LocalizedMessage
                        .get(LOC_DR_ERR_CORRUPT_PRODUCT_INSTALL));
            } else if ((productHome == null)
                    && (!InstallState.isFreshInstall())) {
                // No product home found in Server's Product locator file -
                // while
                // some instance state data is found
                Debug.log("InstanceInteractionsRunner : Error - Product "
                        + "Locator file was not found at '"
                        + getServerHomeConfigHandler().getProductLocatorFile()
                        + ". But Install State information found!");

                throw new InstallException(LocalizedMessage
                        .get(LOC_DR_ERR_CORRUPT_PRODUCT_INSTALL));
            }
        }
    }

    public IStateAccess getStateAccess() {
        return iStateAccess;
    }

    public void setStateAccessDataType(int index) {
        // We are always dealing with TransientStateAccess here. So, nothing
        // to do here.
    }

    public void createAllInteractions(InstallRunInfo installRunInfo)
            throws InstallException {
        ArrayList iFinderInteractionsInfo = installRunInfo
                .getInstanceFinderInteractions();
        initInteractions(iFinderInteractionsInfo);
        // Mark the first interaction
        UserDataInteraction firstInteraction = (UserDataInteraction) 
            getAllInteractions().get(0);
        firstInteraction.setIsFirstFlag(true);
    }

    public void storeNonPersistentKeys(String key, int index) {
        // index is not used here
        getNonPersistentKeys().add(key);
    }

    public Set getNonPersistentKeys() {
        return nonPersistentKeys;
    }

    public void clear() {
        super.clear();
        ((TransientStateAccess) getStateAccess()).clear();
    }

    private ServerLocatorHandler getServerHomeConfigHandler() {
        return serverHomeHandler;
    }

    public void setNonPersistentKeys(Set set) {
        nonPersistentKeys = set;
    }

    private void setServerHomeConfigHandler(ServerLocatorHandler handler) {
        serverHomeHandler = handler;
    }

    public void setStateAccess(IStateAccess iStateAccess) {
        this.iStateAccess = iStateAccess;
    }

    // Determines if it is a first time install
    private ServerLocatorHandler serverHomeHandler;

    private IStateAccess iStateAccess;

    private Set nonPersistentKeys;
}
