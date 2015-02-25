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
 * $Id: IConfigurationListener.java,v 1.2 2008/06/25 05:51:36 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

/**
 * <p>Allows for receiving an updated message when a change in system
 * configuration settings is detected. Once a configuration change is detected,
 * the new configuration is loaded and all the registered listeners are 
 * notified using the method {@link #configurationChanged()}. It is upto
 * the respective listeners to ensure that the subsystem that they represent
 * is able to absorb the configuration changes correctly. </p>
 * <p>
 * The method {@link #getName()} is used by the <code>AgentConfiguration</code>
 * to record which all listeners were notified during a configuration change
 * event dispatch.</p>
 * 
 * @see com.sun.identity.agents.arch.AgentConfiguration
 */
public interface IConfigurationListener {
    
   /**
    * Notifies the listener instance that a configuration change has occurred
    * in the system. When such a change occurs, it is upto the listener to 
    * ensure that the subsystem they represent is able to absorb the 
    * configuration changes correctly.
    */
    public void configurationChanged();
    
   /**
    * Returns a String indicating the descriptive name of the subsystem that 
    * this listener instance represents. All implementing classes are expected
    * to uniquely identify their respective subsystems using the return value
    * as provided by this method.
    * 
    * @return a descriptive name of the subsystem that this listener caters to.
    */
    public String getName();

}
