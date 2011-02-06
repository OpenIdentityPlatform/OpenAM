/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ToolOutputFactory.java,v 1.1 2008/11/22 02:19:54 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.log.impl;

import com.sun.identity.diagnostic.base.core.common.ToolConstants;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;

 /*
  * Returns an instance of IToolOutput.
  */
public class ToolOutputFactory {
    
    public static IToolOutput getInstance(String runMode) {
        IToolOutput opWriter = null;
        if (runMode.equalsIgnoreCase(ToolConstants.GUI_MODE)) {
            opWriter = new ToolGUIWriter();
        } else {
            opWriter = new ToolCLIWriter();
        }   
        return opWriter;
    }
}
