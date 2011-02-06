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
 * $Id: InteractionConstants.java,v 1.4 2008/07/15 21:19:50 leiming Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

/**
 * @author krishc
 * 
 * Interface to store Interaction related constants
 * 
 */
public interface InteractionConstants {

    /**
     * Localized keys required for interaction
     */
    public static final String LOC_IN_ERR_INTERACTION_RUN = 
        "IN_ERR_INTERACTION_RUN";

    public static final String LOC_IN_MESS_SUMMARY_DESC_FORMAT = 
        "IN_MESS_SUMMARY_DESC_FORMAT";

    public static final String LOC_IN_WRN_INVALID_USER_INPUT = 
        "IN_WRN_INVALID_USER_INPUT";

    public static final String LOC_IN_ERR_SILENT_INST_FAILED = 
        "IN_ERR_SILENT_INST_FAILED";

    public static String LOC_IN_ERR_FAILED_TO_CREATE_INTER = 
        "IN_ERR_FAILED_TO_CREATE_INTER";

    public static final String LOC_IN_MSG_OPTION_HELP = 
        "IN_MSG_OPTION_HELP";

    public static final String LOC_IN_MSG_OPTION_BACK = "IN_MSG_OPTION_BACK";

    public static final String LOC_IN_MSG_OPTION_EXIT = "IN_MSG_OPTION_EXIT";

    public static final String LOC_IN_ERR_INVALID_USER_INPUT = 
        "IN_ERR_INVALID_USER_INPUT";

    public static final String LOC_IN_MSG_OPTION_CLR_DEF = 
        "IN_MSG_OPTION_CLR_DEF";
    
    public static final String LOC_VA_WRN_VAL_MESSAGE = 
        "VA_WRN_VAL_MESSAGE";

    public static final String LOC_VA_WRN_VAL_INSTALL_LOG = 
        "VA_WRN_VAL_INSTALL_LOG";
    
    /**
     * Interaction element,field constants
     */
    public static final String STR_IN_DESCRIPTION_SUFFIX = "_DESC";

    public static final String STR_IN_PROMPT_SUFFIX = "_PROMPT";

    public static final String STR_IN_SUMMARY_DESCRIPTION_SUFFIX = 
        "_SUMMARY_DESC";

    public static final String STR_IN_HELP_SUFFIX = "_HELP";

    public static final String STR_IN_ERROR_SUFFIX = "_ERROR";

    public static final String STR_IN_WARNING_SUFFIX = "_WARNING";

    /**
     * public static strings
     */
    public static final String STR_IN_EMPTY_STRING = "";

    public static final String STR_IN_MSG_OPTION_HELP = "?";

    public static final String STR_IN_MSG_OPTION_BACK = "<";

    public static final String STR_IN_MSG_OPTION_EXIT = "!";

    public static final String STR_IN_MSG_OPTION_CLR_DEF = "^";

    public static final String STR_IN_COLON = ":";

    public static final String STR_IN_INSTALL_INTER_TYPE = "install";

}
