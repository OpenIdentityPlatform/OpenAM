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
 * $Id: DummyCommand.java,v 1.2 2008/06/25 05:44:16 qcheng Exp $
 *
 */

package com.sun.identity.cli;

import com.sun.identity.cli.schema.SchemaCommand;

/**
 * Dummy command for testing purposes.
 */
public class DummyCommand extends SchemaCommand {
    
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throw CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {   
        super.handleRequest(rc);
        if (isOptionSet("testmatch")) {
            if (!getStringOptionValue("mandatory").equals("mandatory") ||
                !getStringOptionValue("optional").equals("optional") ) {
                throw new CLIException("option values does not matched.", 
                    ExitCodes.INVALID_OPTION_VALUE);
            }
        }
    }
}
