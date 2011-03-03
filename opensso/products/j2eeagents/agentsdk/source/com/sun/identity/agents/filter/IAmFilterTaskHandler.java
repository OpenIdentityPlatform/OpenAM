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
 * $Id: IAmFilterTaskHandler.java,v 1.2 2008/06/25 05:51:45 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import com.sun.identity.agents.arch.AgentException;

/**
 * <p>
 * The <code>AmFilter</code> uses a set of task handlers that together
 * provide the necessary functionality depending upon the filter mode to cater
 * to the incoming requests. Each such task handler used by the filter caters
 * to an independent unit of the overall processing task that may be necessary
 * for the operation of the filter in the given mode. The intent of this
 * interface is to isolate such independent tasks needed in the overall
 * processing of a request by the filter and thus decouple them in a manner
 * that they can invoked by the filter as necessary depending upon its mode
 * of operation.
 * </p>
 * <p>
 * All implementations of this interfaces are required to be stateless at best
 * and thread-safe at the least since the same instance of the task handler
 * may be required to process various requests on concurrent threads in the
 * given web container.
 * </p>
 * <p>
 * During its course of operation, the <code>AmFilter</code> uses an ordered
 * collection of such task handlers and passes the request to them till the
 * point where the entire set of task handlers have been exhausted, or till the
 * point where a certain task handler processing resulted in an action that
 * must be taken on the request. In the later case, the request is no longer
 * passed to the subsequent task handlers and instead the action suggested by
 * the last task handler is taken immediately, thereby short-circuiting the
 * overall processing of the request.
 * </p>
 */
public interface IAmFilterTaskHandler extends IAmFilterHandler {

    /**
     * This method is invoked by the <code>AmFilter</code> when the task
     * handled by this particular handler must be done for a given filter
     * request.
     *
     * @param ctx the filter request context which provides access to the
     * underlying <code>HttpServletRequest</code>,
     * <code>HttpServletResponse</code> and other data that
     * may be needed by this handler to process its particular task
     *
     * @return <code>AmFilterResult</code> if the processing of this task
     * resulted in a particular action to be taken for the incoming request.
     * The return could be <code>null</code> if no action is necessary for
     * this request.
     *
     * @throws AgentException if the processing resulted in an unrecoverable
     * error condition
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
        throws AgentException;
}
