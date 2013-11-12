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
 * $Id: CLIConstants.java,v 1.12 2009/04/02 01:16:07 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.cli;


/**
 * This interface defines constants used in the package.
 */
public interface CLIConstants {
    /**
     * Name of definition files that drive the behavior of CLI.
     * These files are passed in as a property in the java interface.
     * i.e. <code>java -DdefinitionFiles=...</code>.
     */
    String SYS_PROPERTY_DEFINITION_FILES = "definitionFiles";

    /**
     * Name of the commandline interface. This name is passed in as a property
     * in the java interface. i.e. <code>java -DcommandName=...</code>.
     */
    String SYS_PROPERTY_COMMAND_NAME = "commandName";

    /**
     * Class name of the output writer. This name is passed in as a property
     * in the java interface. i.e. <code>java -DoutputWriter=...</code>.
     */
    String SYS_PROPERTY_OUTPUT_WRITER = "outputWriter";

    /**
     * Name of web enabled URL.
     */
    String WEB_ENABLED_URL = "webEnabledURL";

    /**
     * Name of debugger.
     */
    String NAME_DEBUGGER = "amcli";
    
    /**
     * Name of product name field in definition class.
     */
    String FLD_PRODUCT_NAME = "product";

    /**
     * Name of product name annotation in definition class.
     */
    String ANNOT_PRODUCTNAME = "productName";

    /**
     * Name of product version annotation in definition class.
     */
    String ANNOT_VERSION = "version";

    /**
     * Name of product resource bundle name annotation in definition class.
     */
    String ANNOT_RESOURCE_BUNDLE_NAME = "resourceBundle";
    
    /**
     * Prefix for argument/option defined in this interface.
     */
    String PREFIX_ARGUMENT = "ARGUMENT_";

    /**
     * Prefix for short argument/option defined in this interface.
     */
    String PREFIX_SHORT_ARGUMENT = "SHORT_ARGUMENT_";

    /**
     * Prefix of long argument/option passed into the CLI.
     * e.g. --version
     */
    String PREFIX_ARGUMENT_LONG= "--";

    /**
     * Prefix of short argument/option passed into the CLI.
     * e.g. --n
     */
    String PREFIX_ARGUMENT_SHORT= "-";

    /**
     * Prefix of resource key.
     * e.g. subcmd-version=Print Version of this tool.
     */
    String PREFIX_SUBCMD_RES = "subcmd-";

    /**
     * Flag to indicate that an argument/option is to be displayed as
     * textarea in web based CLI.
     */
    String FLAG_WEB_UI_TEXTAREA = "t";

    /**
     * Flag to indicate that an argument/option is to be displayed as
     * text box in web based CLI.
     */
    String FLAG_WEB_UI_TEXT = "i";

    /**
     * Flag to indicate that an argument/option is to be displayed as
     * checkbox in web based CLI.
     */
    String FLAG_WEB_UI_CHECKBOX = "c";

    /**
     * Flag to indicate that an argument/option is unary.
     * e.g. --help
     */
    String FLAG_UNARY = "u";

    /**
     * Flag to indicate that an argument/option is single.
     * e.g. --locale en_US
     */
    String FLAG_SINGLE = "s";

    /**
     * Name of version argument/option.
     */
    String ARGUMENT_VERSION = "version";

    /**
     * Short name of version argument/option.
     */
    String SHORT_ARGUMENT_VERSION = "V";

    /**
     * Name of tool information argument/option.
     */
    String ARGUMENT_INFORMATION = "information";

    /**
     * Short name of information argument/option.
     */
    String SHORT_ARGUMENT_INFORMATION = "O";

    /**
     * Name of debug argument/option.
     */
    String ARGUMENT_DEBUG = "debug";

    /**
     * Short name of debug argument/option.
     */
    String SHORT_ARGUMENT_DEBUG = "d";

    /**
     * Name of help argument/option.
     */
    String ARGUMENT_HELP = "help";

    /**
     * Short name of help argument/option.
     */
    String SHORT_ARGUMENT_HELP = "?";

    /**
     * Name of verbose argument/option.
     */
    String ARGUMENT_VERBOSE = "verbose";

    /**
     * Short name of verbose argument/option.
     */
    String SHORT_ARGUMENT_VERBOSE = "v";

    /**
     * Name of locale argument/option.
     */
    String ARGUMENT_LOCALE = "locale";

    /**
     * Short name of locale argument/option.
     */
    String SHORT_ARGUMENT_LOCALE = "l";

    /**
     * Name of disable logging argument/option.
     */
    String ARGUMENT_NOLOG = "nolog";

    /**
     * Short name of disable logging argument/option.
     */
    String SHORT_ARGUMENT_NOLOG = "O";

    /**
     * Template for formating usage.
     */
    String USAGE_FORMAT = "    {0} --{1}, -{2}\n        {3}\n";

    /**
     * Template for formating sub command (short format) in usage text.
     */
    String USAGE_SUBCMD_FORMAT = "    {0} {1} --{2} --{3}\n        {4}\n";

    /**
     * Template for formating sub command (long format) in usage text.
     */
    String USAGE_SUBCMD_LONG_FORMAT = "{0} {1} --{2} [--{3}]\n    {4}\n";

    /**
     * Template for formating sub command (extended format) in usage text.
     */
    String USAGE_SUBCMD_EX_FORMAT = "    {0}\n        {1}\n";

    /**
     * Template for formating sub command help in usage text.
     */
    String USAGE_SUBCMD_HELP_FORMAT = "    {0} {1} --{2}, -{3}\n        {4}\n";

    /**
     * Template for formating mandatory option name in usage text.
     */
    String USAGE_OPTION_NAME_FORMAT = "    --{0}|-{1}";

    /**
     * Template for formating operational option name in usage text.
     */
    String USAGE_OPTIONAL_OPTION_NAME_FORMAT = "    [--{0}|-{1}]";

    /**
     * Template for formating mandatory option in usage text.
     */
    String USAGE_OPTION_FORMAT = "    --{0}, -{1}\n        {2}\n";

    /**
     * Template for formating mandatory option (with alias) in usage text.
     */
    String USAGE_OPTION_WITH_ALIAS_FORMAT = "    --{0}, -{1} |\n        {2}\n";

    /**
     * Template for formating optional option in usage text.
     */
    String USAGE_OPTIONAL_OPTION_FORMAT = "    --{0}, -{1}\n        {2}\n";

    /**
     * Template for formating optional option (with alias) in usage text.
     */
    String USAGE_OPTIONAL_OPTION_WITh_ALIAS_FORMAT =
        "    --{0}, -{1} |\n        {2}\n";

    /**
     * Core authentication service name.
     */
    String AUTH_CORE_SERVICE = "iPlanetAMAuthService";
    
    /**
     * Tag for web input.
     */
    String WEB_INPUT = "web-input";

    /**
     * Comment Tag for exit code in JSP
     */
    String JSP_EXIT_CODE_TAG = "<!-- CLI Exit Code: {0} -->";

    /**
     * Web resource string marker.
     */
    String WEB_RES_MARKER = "__web__";

    /**
     * Agent Type attribute name.
     */
    String ATTR_NAME_AGENT_TYPE = "AgentType";
    
    /**
     * Agent Password Attribute Schema name
     */
    String ATTR_SCHEMA_AGENT_PWD = "userpassword";

    /**
     * Import configuration data sub command name.
     */
    String CMD_NAME_IMPORT_SVC_CONFIG = "import_svc_cfg";
}
