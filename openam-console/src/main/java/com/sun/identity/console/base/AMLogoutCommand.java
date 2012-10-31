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
 * $Id: AMLogoutCommand.java,v 1.2 2008/06/25 05:42:47 qcheng Exp $
 *
 */

package com.sun.identity.console.base;

import com.iplanet.jato.command.Command;
import com.iplanet.jato.command.CommandEvent;
import com.iplanet.jato.command.CommandException;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import com.sun.identity.console.base.model.AMModelBase;

/**
 * Handles the event when the logout button is clicked. All that
 * needs to happen is get the logout URL and redirect to that
 * value.
 */
public class AMLogoutCommand implements Command {
    public AMLogoutCommand() {
        super();
    }

    public void execute(CommandEvent event) throws CommandException {
        HttpServletResponse response = event.getRequestContext().getResponse();
        try {
            response.sendRedirect(AMModelBase.getLogoutURL());
        } catch(IOException e) {
            throw new CommandException(e.getMessage());
        }
    }
}
