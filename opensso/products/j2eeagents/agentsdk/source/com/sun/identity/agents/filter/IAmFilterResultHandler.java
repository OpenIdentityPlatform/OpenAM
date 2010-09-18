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
 * $Id: IAmFilterResultHandler.java,v 1.2 2008/06/25 05:51:45 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import com.sun.identity.agents.arch.AgentException;

/**
 * <p>
 * When an incoming request is fully processed by the <code>AmFilter</code>, the
 * resulting <code>AmFilterResult</code> may need to be further processed for
 * purposes such as logging etc. This interface defines such entities which can
 * independently process the generated a result and can optionally override the
 * original result if need be.
 * </p>
 * <p>
 * All implementations of this interfaces are required to be stateless at best
 * and thread-safe at the least since the same instance of the result handler
 * may be required to process various results on concurrent threads in the
 * given web container.
 * </p>
 * <p>
 * The <code>AmFilter</code> maintains a set of result handlers in an ordered
 * collection which get invoked for every request. When the <code>AmFilter</code>
 * has a result that is ready to be processed, it  invokes these
 * <code>IAmFilterResultProcessor</code> instances sequentially and allows
 * them to override the result if necessary. However, unlike the
 * <code>IAmFilterTaskHandler</code> implementations which can potentially
 * short-circuit the handling of the request, these result handlers may not
 * do so in any case. A result produced by a result handler therefore gets
 * passed downstream to the remaining result handlers that may be active in
 * the system. The result returned to the client is the outcome of the final
 * result handler invocation.
 * </p>
 */
public interface IAmFilterResultHandler extends IAmFilterHandler {

    /**
     * This method is invoked by the <code>AmFilter</code> in order to process
     * a result that is obtained by the active task handlers in the system, or
     * which is obtained by the invocation of a previous result handler in
     * the system if applicable.
     *
     * @param ctx the filter request context which provides access to the
     * underlying <code>HttpServletRequest</code>,
     * <code>HttpServletResponse</code> and other data that
     * may be needed by this handler for facilitating its processing.
     *
     * @param result the <code>AmFilterResult</code> obtained by the
     * <code>AmFilter</code> by processing the incoming request.
     *
     * @return <code>AmFilterResult</code> if the processing resulted in a
     * particular action to be taken for the incoming request. <b>If no 
     * processing is applicable to the given result instance, the same instance 
     * is returned by this method.</b>
     *
     * @throws AgentException if the processing resulted in an unrecoverable
     * error condition
     */
    public AmFilterResult process(AmFilterRequestContext ctx,
                                  AmFilterResult result)
        throws AgentException;
}
