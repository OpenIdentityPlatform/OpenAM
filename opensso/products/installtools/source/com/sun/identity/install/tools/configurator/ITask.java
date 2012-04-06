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
 * $Id: ITask.java,v 1.2 2008/06/25 05:51:19 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.Map;

import com.sun.identity.install.tools.util.LocalizedMessage;

public interface ITask {

    public boolean execute(String name, IStateAccess stateAccess, 
            Map properties) throws InstallException;

    public boolean rollBack(String name, IStateAccess state, Map properties)
            throws InstallException;

    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties);

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties);
}
