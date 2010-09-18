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
 * $Id: IAmFilterHandler.java,v 1.2 2008/06/25 05:51:45 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import com.sun.identity.agents.arch.AgentException;


/**
 * <p>
 * The <code>AmFilter</code> uses a set of handlers that together
 * provide the necessary functionality depending upon the filter mode to cater
 * to the incoming requests. Each such handler used by the filter caters
 * to an independent unit of the overall processing task that may be necessary
 * for the operation of the filter in the given mode.
 *
 * The intent of this interface is to define a generic handler type which
 * can be used to identify common methods that all such handlers are obligated
 * to implement.
 * </p>
 * <p>
 * All implementations of this interfaces are required to be stateless at best
 * and thread-safe at the least since the same instance of the handler
 * may be required to process various requests on concurrent threads in the
 * given web container.
 * </p>
 */
public interface IAmFilterHandler {
    
    
   /**
    * Allows the handler to be initialized based on the given 
    * <code>ISSOContext</code> and the <code>AmFilterMode</code>.
    *  
    * @param context
    * @param mode
    * @throws AgentException
    */
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException;

    /**
     * This method returns a boolean value indicating if the implementing
     * handler can be considered active for the current mode of filter
     * operation. Certain handlers that may fit in the filter processing
     * chain can still become inactive in cases such as when they are explicity
     * disabled by means of configuration. Other handlers may choose to
     * be inactive if the specified configuration is insufficient for it to
     * make any difference in the filter processing chain or because of certain
     * internal non-fatal error conditions.
     * @return true if the task handler is active, false otherwise
     */
    public boolean isActive();

    /**
     * This method returns a string value identifying the handler. This
     * name may be used by the filter when logging certain debug messages to the
     * subsystem log file.
     * @return a descriptive name for the task handler
     */
    public String getHandlerName();
}
