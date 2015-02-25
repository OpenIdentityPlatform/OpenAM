/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock, Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.agents.jboss.install;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.util.Map;
import static org.forgerock.openam.agents.jboss.install.InstallerConstants.*;

/**
 * Performs changes on the JBoss configuration XML file so the agent can properly work.
 *
 * @author Peter Major
 */
public class ConfigureXMLTask extends ConfigXMLBase implements ITask {

    /**
     * @see ConfigXMLBase#performChanges(com.sun.identity.install.tools.configurator.IStateAccess).
     */
    @Override
    public boolean execute(String name, IStateAccess state, Map properties) throws InstallException {
        Debug.log("Updating configuration XML");
        return performChanges(state);
    }

    /**
     * @see ConfigXMLBase#rollbackChanges(com.sun.identity.install.tools.configurator.IStateAccess).
     */
    @Override
    public boolean rollBack(String name, IStateAccess state, Map properties) throws InstallException {
        Debug.log("Rolling back configuration XML changes");
        return rollbackChanges(state);
    }

    @Override
    public LocalizedMessage getExecutionMessage(IStateAccess state, Map properties) {
        return LocalizedMessage.get(LOC_UPDATE_CONFIG_XML_EXECUTE, BUNDLE_NAME, new Object[]{getConfigFilePath(state)});
    }

    @Override
    public LocalizedMessage getRollBackMessage(IStateAccess state, Map properties) {
        return LocalizedMessage.get(LOC_UPDATE_CONFIG_XML_ROLLBACK, BUNDLE_NAME,
                new Object[]{getConfigFilePath(state)});
    }
}
