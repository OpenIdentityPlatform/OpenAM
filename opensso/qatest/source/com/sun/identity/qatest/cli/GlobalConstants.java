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
 * $Id: GlobalConstants.java,v 1.2 2008/01/08 17:59:41 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.cli;

/**
 * <code>GlobalConstants</code> is an interface which contains the argument
 * prefixes and the long and short option names for the global options.
 */
public interface GlobalConstants {
    /**
     * Prefix for a long argument
     */
    static final String PREFIX_ARGUMENT_LONG = "--";
    
    /**
     * Prefix for a short argument
     */
    static final String PREFIX_ARGUMENT_SHORT = "-";
    
    /**
     * Locale argument/option 
     */
    static final String LOCALE_ARGUMENT = "locale";
    
    /**
     * Short locale argument/option
     */
    static final String SHORT_LOCALE_ARGUMENT = "l";
    
    /**
     * Debug argument/option
     */
    static final String DEBUG_ARGUMENT = "debug";
    
    /**
     * Short debug argument/option
     */
    static final String SHORT_DEBUG_ARGUMENT = "d";
    
    /**
     * Verbose argument/option
     */
    static final String VERBOSE_ARGUMENT = "verbose";
    
    /**
     * Short verbose argument/option
     */
    static final String SHORT_VERBOSE_ARGUMENT = "v";
    
    /**
     * Version argument/option
     */
    static final String VERSION_ARGUMENT = "version";
    
    /**
     * Short version argument/option
     */
    static final String SHORT_VERSION_ARGUMENT = "V";
    
    /**
     * Help argument/option
     */
    static final String HELP_ARGUMENT = "help";
    
    /**
     * Short help argument/option
     */
    static final String SHORT_HELP_ARGUMENT = "?";
}
