/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Create.java,v 1.3 2008/06/25 05:42:32 qcheng Exp $
 *
 */
package com.sun.identity.config.agent;

import com.sun.identity.config.util.TemplatedPage;

import java.util.Collections;
import java.util.List;

/**
 * @author Les Hazlewood
 */
public class Create extends TemplatedPage {

    private List groups;
    private List profiles;

    protected String getTitle() {
        return "agent.create.title";
    }

    public void doInit() {
        this.groups = getConfigurator().getAgentGroups();
        if ( this.groups == null ) {
            this.groups = Collections.EMPTY_LIST;
        }
        this.profiles = getConfigurator().getAgentProfiles();
        if ( this.profiles == null ) {
            this.profiles = Collections.EMPTY_LIST;
        }
        
        addModel("groups", this.groups );
        addModel("profiles", this.profiles );
    }

}
