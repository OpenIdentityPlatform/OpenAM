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
 * $Id: Handler.java,v 1.2 2008/06/25 05:43:54 qcheng Exp $
 *
 */

package com.sun.identity.protocol.https;

import com.sun.identity.shared.debug.Debug;

/*
 *	HTTP stream opener
 *      open an https input stream given a URL 
 */
public class Handler extends sun.net.www.protocol.https.Handler {
    static private Debug debug = Debug.getInstance("amJSSE");
    
    /**
     * Constructor
     */
    public Handler () {
        super();
        Https.init();
    }
	
    /**
     * Constructor
     * @param proxy
     * @param port
     */
    public Handler (String proxy, int port) {
        super(proxy, port);
        Https.init();
    }
	
    /**
     * Constructor
     * @param alias  certificate alias for client certificate used in the
     *     https connection if client auth is required
     */
    public Handler (String alias) {
        super();
        Https.init(alias);
        if (debug.messageEnabled()) {
            debug.message("certAlias --> " + alias);
        }
    }
}
