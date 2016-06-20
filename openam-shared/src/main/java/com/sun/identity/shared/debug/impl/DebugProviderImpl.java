/*
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
 * $Id: DebugProviderImpl.java,v 1.4 2009/03/07 08:01:53 veiming Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.shared.debug.impl;

import com.sun.identity.shared.debug.IDebug;
import com.sun.identity.shared.debug.IDebugProvider;
import com.sun.identity.shared.debug.file.DebugFileProvider;
import com.sun.identity.shared.debug.file.impl.DebugFileProviderImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Default debug provider implementation.
 */
public class DebugProviderImpl implements IDebugProvider {
    private DebugFileProvider debugFileProvider;

    private Map<String, IDebug> debugMap = new HashMap<String, IDebug>();

    /**
     * Default constructor
     * {@link com.sun.identity.shared.debug.file.impl.DebugFileProviderImpl} would be debug file provider used by
     * every debug logs
     */
    public DebugProviderImpl() {
        this(new DebugFileProviderImpl());
    }

    /**
     * Constructor with a debug file provider
     *
     * @param debugFileProvider debug file provider used by every debug logs
     */
    public DebugProviderImpl(DebugFileProvider debugFileProvider) {
        this.debugFileProvider = debugFileProvider;
    }

    /**
     * Get the debugger associated with the debug name
     *
     * @param debugName name of the debug instance which will be returned.
     * @return a debug instance
     */
    public synchronized IDebug getInstance(String debugName) {

        IDebug debug = debugMap.get(debugName);
        if (debug == null) {
            debug = new DebugImpl(debugName, debugFileProvider);
            debugMap.put(debugName, debug);
        }
        return debug;
    }

}
