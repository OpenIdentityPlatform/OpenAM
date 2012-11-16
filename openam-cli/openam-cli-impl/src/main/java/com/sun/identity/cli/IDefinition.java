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
 * $Id: IDefinition.java,v 1.6 2008/10/09 04:28:57 veiming Exp $
 *
 */

package com.sun.identity.cli;


import java.util.List;
import java.util.Locale;

/**
 * This interface defines methods for a CLI definition class.
 */
public interface IDefinition {
    
    /**
     * Initializes the definition class.
     * 
     * @param locale Locale of the request.
     * @throws CLIException if command definition cannot initialized.
     */
    void init(Locale locale)
        throws CLIException;
    
    /**
     * Returns product name.
     *
     * @return product name.
     */
    String getProductName();

    /**
     * Returns log name.
     *
     * @return log name.
     */
    String getLogName();
    
    /**
     * Returns a list of sub commands.
     *
     * @return a list of sub commands.
     */
    List getSubCommands();

    /**
     * Returns sub command object.
     *
     * @param name Name of sub command.
     * @return sub command object.
     */
    SubCommand getSubCommand(String name);
    
    /**
     * Returns <code>true</code> if the option is an authentication related
     * option such as user ID and password.
     *
     * @param opt Name of option.
     * @return <code>true</code> if the option is an authentication related
     *         option such as user ID and password.
     */
    boolean isAuthOption(String opt);
}
