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
 * $Id: ToolOutputManager.java,v 1.1 2008/11/22 02:19:53 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.log;

import com.sun.identity.diagnostic.base.core.ToolContext;
import com.sun.identity.diagnostic.base.core.log.impl.ToolOutputFactory;

/**
 * This class provides the output writer based on the application run mode.
 */

public class ToolOutputManager {
    
    private ToolContext tContext;
    private static ToolOutputManager toolOutputMgr;
    private IToolOutput toolOutput;
    private static Object instanceLock = new Object();
    
    private ToolOutputManager() {
    }

    public static ToolOutputManager getInstance() {
        if (toolOutputMgr != null) {
            return toolOutputMgr;
        }
        synchronized (instanceLock) {
            if (toolOutputMgr == null) {
                toolOutputMgr = new ToolOutputManager();
            }
        }
        return toolOutputMgr;
    }

    
    /**
     * Instatiates the actual <code>ToolOutput</code> based on the 
     * tool run mode.
     *
     * @param tContext Context of the tool application. 
     */
    public void init(ToolContext tContext) {
        toolOutput = ToolOutputFactory.getInstance(tContext.getMode());
    }

    /**
     * Returns a output writer based on the tool run mode 
     *
     * @return toolOutput Output writer based on tool run mode.
     */
    public IToolOutput getOutputWriter() {
        return toolOutput;
    }
}
