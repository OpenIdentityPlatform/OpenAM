/* The contents of this file are subject to the terms
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
 * $Id: Session.java,v 1.2 2009/10/14 08:56:54 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.http;

import java.util.HashMap;

/**
 * Provides a mechanism for the storage of attributes associated with a
 * specific client and principal.
 * <p>
 * Handlers are guaranteed that an incoming request contains a session object
 * that will persist from one request to the next for the same remote client
 * and principal. If the session object is empty upon returning response to the
 * remote client, the container is free to remove any cookies or other session
 * persistence mechanism resources.
 *
 * @author Paul C. Bryan
 */
public class Session extends HashMap<String, Object> {
}

