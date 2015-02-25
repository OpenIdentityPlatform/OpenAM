/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.agents.jetty.v7;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.plus.jaas.spi.UserInfo;
import org.eclipse.jetty.util.security.Credential;

/**
 *
 * @author Peter Major
 */
public class AgentUserInfo extends UserInfo {

    public AgentUserInfo(String userName, Credential credential, List<String> roleNames) {
        super(userName, new AgentCredential(userName), null);
    }

    @Override
    public List<String> getRoleNames() {
        return new ArrayList<String>(((AgentCredential)getCredential()).getRoles());
    }
}
