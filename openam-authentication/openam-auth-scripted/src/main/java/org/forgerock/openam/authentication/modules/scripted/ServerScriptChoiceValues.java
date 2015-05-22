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
package org.forgerock.openam.authentication.modules.scripted;

import org.forgerock.openam.scripting.ScriptConstants;
import org.forgerock.openam.scripting.service.ScriptChoiceValues;

/**
 * This class is used to retrieve the server side authentication script names from the scripting
 * service for display in a drop down UI component.
 *
 * @since 13.0.0
 */
public class ServerScriptChoiceValues extends ScriptChoiceValues {

    @Override
    protected String getContextName() {
        return ScriptConstants.ScriptContext.AUTHENTICATION_SERVER_SIDE.name();
    }
}