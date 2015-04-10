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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.bootstrap;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;

/**
 * This interface defines the concerns of accessing the configuration state corresponding to the soap STS agent from
 * OpenAM
 */
public interface SoapSTSAgentConfigAccess {
    /**
     * @return the configuration state, in json format, corresponding to this soap-sts agent.
     * @throws ResourceException If an exception is encountered obtaining this configuration state.
     */
    JsonValue getConfigurationState() throws ResourceException;
}
