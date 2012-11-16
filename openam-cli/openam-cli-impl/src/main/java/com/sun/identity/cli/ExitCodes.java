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
 * $Id: ExitCodes.java,v 1.4 2008/07/31 21:54:55 veiming Exp $
 *
 */

package com.sun.identity.cli;


/**
 * This interface defines exit codes.
 */
public interface ExitCodes {
    /**
     * Missing resource bundle.
     */
    int MISSING_RESOURCE_BUNDLE = 1;

    /**
     * Missing definition files. i.e. java -DdefinitionFiles=....
     * is missing.
     */
    int MISSING_DEFINITION_FILES = 2;

    /**
     * Missing commandline name i.e. java -DcommandName=...
     * is missing.
     */
    int MISSING_COMMAND_NAME = 3;

    /**
     * The defined definition class is not found.
     */
    int MISSING_DEFINITION_CLASS = 4;

    /**
     * The incorrect definition class.
     */
    int INCORRECT_DEFINITION_CLASS = 5;

    /**
     * The defined definition class does not implement <code>IDefinition</code>
     * interface.
     */
    int CLASS_CAST_DEFINITION_CLASS = 6;

    /**
     *
     * Definition class cannot be instantiated.
     */
    int INSTANTIATION_DEFINITION_CLASS = 7;

    /**
     * Illegal access to definition class.
     */
    int ILLEGEL_ACCESS_DEFINITION_CLASS = 8;

    /**
     * Definition class is using a reserved argument/option.
     * e.g. -d as this is already reserved for debug.
     */
    int RESERVED_OPTION = 9;

    /**
     * Usage of the commandline cannot formated.
     */
    int USAGE_FORMAT_ERROR = 10;

    /**
     * Invalid argument/option.
     */
    int INCORRECT_OPTION = 11;

    /**
     * Invalid sub command.
     */
    int INVALID_SUBCOMMAND = 12;

    /**
     * Sub command implementation class not found.
     */
    int SUBCOMMAND_IMPLEMENT_CLASS_NOTFOUND = 13;

    /**
     * Sub command implementation class cannot be instantiated.
     */
    int SUBCOMMAND_IMPLEMENT_CLASS_CANNOT_INSTANTIATE = 14;

    /**
     * Illegal access to sub command implementation class.
     */
    int SUBCOMMAND_IMPLEMENT_CLASS_ILLEGAL_ACCESS = 15;

    /**
     * Cannot instantiate output writer class.
     */
    int OUTPUT_WRITER_CLASS_CANNOT_INSTANTIATE = 16;

    /**
     * Cannot instantiate debugger class.
     */
    int DEBUGGER_CLASS_CANNOT_INSTANTIATE = 17;

    /**
     * Cannot read a file.
     */
    int CANNOT_READ_FILE = 18;

    /**
     * LDAP Login Fails.
     */
    int LDAP_LOGIN_FAILED = 19;

    /**
     * Session Based Login Fails.
     */
    int SESSION_BASED_LOGIN_FAILED = 20;

    /**
     * Definition class duplicated argument/options.
     */
    int DUPLICATED_OPTION = 21;

    /**
     * Session Based Logout failed..
     */
    int SESSION_BASED_LOGOUT_FAILED = 22;

    /**
     * Invalid option value.
     */
    int INVALID_OPTION_VALUE = 23;

    /**
     * Input/Output Exception
     */
    int IO_EXCEPTION = 24;

    /**
     * Cannot write to log.
     */
    int CANNOT_WRITE_LOG = 25;

    /**
     * Incorrect data format.
     */
    int INCORRECT_DATA_FORMAT = 26;

    /**
     * Session has expired.
     */
    int SESSION_EXPIRED = 27;
    
    /**
     * Request cannot be processed.
     */
    int REQUEST_CANNOT_BE_PROCESSED = 127;
}
