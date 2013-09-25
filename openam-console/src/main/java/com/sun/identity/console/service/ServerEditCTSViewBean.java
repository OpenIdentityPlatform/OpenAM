/**
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
 * Copyright 2013 ForgeRock AS.
 */
package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;

/**
 * Server Configuration, CTS Tab.
 */
public class ServerEditCTSViewBean extends ServerEditViewBeanBase {

    private static final String DEFAULT_DISPLAY_URL = "/console/service/ServerEditCTS.jsp";

    /**
     * Creates a modify server view bean.
     */
    public ServerEditCTSViewBean() {
        super("ServerEditCTS", DEFAULT_DISPLAY_URL);
    }

    protected String getPropertyXML() {
        return "com/sun/identity/console/propertyServerEditCTS.xml";
    }

    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        super.beginDisplay(event);

        // TODO: There is an opportunity here to pre-populated the fields with default values that perhaps
        // can only be determined at run time, like the default root suffix.
    }

    /**
     * Handles modify server request.
     *
     * @param event
     *         Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) throws ModelControlException {

        // TODO: Provide any pre-commit validation of submitted fields.

        submitCycle = true;
        modifyProperties();
        forwardTo();
    }

}
