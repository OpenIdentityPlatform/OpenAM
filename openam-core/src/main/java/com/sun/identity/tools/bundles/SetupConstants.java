/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SetupConstants.java,v 1.5 2009/10/30 21:10:10 weisun2 Exp $
 *
 */
/**
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.tools.bundles;

public interface SetupConstants {

    char DEFAULT_WILD_CARD = '*';
    String WINDOWS = "windows";
    String SOLARIS = "solaris";
    String X86SOLARIS = "x86solaris";
    String LINUX = "linux";
    String AIX = "aix"; 
    String DEFAULT_PROPERTIES_FILE = "com.sun.identity.tools.bundles." +
        "amadmtoolssetup";
    String SETUP_PROPERTIES_FILE = "file.setup";
    String AMCONFIG_PATH = "path.AMConfig";
    String FILE_SEPARATOR = System.getProperty("file.separator");
    String OS_NAME = System.getProperty("os.name");
    String OS_ARCH = System.getProperty("os.arch");
    String FROM_DIR = ".fromdir";
    String TO_DIR = ".todir";
    String FROM_FILE = ".fromfile";
    String TO_FILE = ".tofile";
    String QUESTION = ".question";
    String TOKEN = ".token";
    String USER_INPUT = "UserInput";
    String BASE_DIR = "BaseDir";
    String YES = "yes";
    String CONFIG_FILE = "file.config";
    String CURRENT_PLATFORM = "CurrentPlatform";
    String VAR_PREFIX = "${";
    String VAR_SUFFIX = "}";
    String REX_VAR_PREFIX = "[$]\\{";
    String REX_VAR_SUFFIX = "\\}";
    String CHECK_VERSION = "version.check";
    String VERSION_FILE = "version.file";
    String AM_VERSION_CURRENT = "am.version.current";
    String AM_VERSION_EXPECTED = "am.version.expected";
    String JAVA_VERSION_EXPECTED = "java.version.expected";
    String JAVA_VERSION_CURRENT = "java.vm.version";
    String XML_CONFIG = "xml.config";
    String SUNOS = "sunos";
    String X86 = "86";
    String X64 = "64";
    String TOOLS_VERSION = "version.tools";
    String AM_VERSION = "com.iplanet.am.version";
    String PRINT_HELP = "help.print";
    String CONFIG_LOAD = "load.config";
    String ALL = "all";
    int BUFFER_SIZE = 8192;
    String GZIP_EXT = ".gz";
    String PATH_DEST = "path.dest";

    /**
     * debug directory system property name.
     */
    String DEBUG_PATH = "path.debug";

    /**
     * log directory system property name.
     */
    String LOG_PATH = "path.log";
}
