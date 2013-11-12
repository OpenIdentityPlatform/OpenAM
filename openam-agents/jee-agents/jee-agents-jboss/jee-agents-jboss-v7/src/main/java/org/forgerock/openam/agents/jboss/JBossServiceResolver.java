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
package org.forgerock.openam.agents.jboss;

import com.sun.identity.agents.arch.ServiceResolver;
import com.sun.identity.agents.filter.GenericJ2EELogoutHandler;
import com.sun.identity.agents.jboss.v40.AmJBossCDSSOTaskHandler;
import com.sun.identity.agents.jboss.v40.AmJBossSSOTaskHandler;
import com.sun.identity.agents.realm.GenericExternalVerificationHandler;

/**
 * This Service Resolver class makes sure that the JBoss v7 specific hooks are correctly registered.
 *
 * @author Peter Major
 */
public class JBossServiceResolver extends ServiceResolver {

    @Override
    public String getGlobalJ2EEAuthHandlerImpl() {
        return JBossJ2EEAuthHandler.class.getName();
    }

    @Override
    public String getGlobalJ2EELogoutHandlerImpl() {
        return GenericJ2EELogoutHandler.class.getName();
    }

    @Override
    public String getGlobalVerificationHandlerImpl() {
        return GenericExternalVerificationHandler.class.getName();
    }

    @Override
    public String getSSOTaskHandlerImpl() {
        return AmJBossSSOTaskHandler.class.getName();
    }

    @Override
    public String getCDSSOTaskHandlerImpl() {
        return AmJBossCDSSOTaskHandler.class.getName();
    }
}
