/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
* $Id: PatchGeneratorConstants.java,v 1.6 2009/03/10 23:54:14 veiming Exp $
*/

package com.sun.identity.tools.patch;

public interface PatchGeneratorConstants {
    String RESOURCE_BUNDLE_NAME = "ssoPatch";
    
    String MANIFEST_CREATE_FILE = "file.create.manifest";
    String DEFAULT_MANIFEST_FILE = "META-INF/OpenSSO.manifest";
    String PROPERTIES_FILE = "file.properties";
    String IDENTIFIER_ENTRY = "entry.identifier";
    String DEFAULT_IDENTIFIER_ENTRY = "identifier";
    String MANIFEST_PATTERN = "pattern.manifest";
    String MANIFEST_FILE_NAME = "filename.manifest";
    String DEFAULT_MANIFEST_FILE_NAME = ".manifest";
    String WILDCARD_CHAR = "pattern.wildcard";
    String DEFAULT_MANIFEST_PATTERN = "*.manifest";
    String DEFAULT_WILDCARD_CHAR ="*";

    String OPTION_HELP = "--help";
    String OPTION_SRC_FILE_PATH = "--war-file";
    String OPTION_DEST_FILE_PATH = "--manifest";
    String OPTION_SRC2_FILE_PATH = "--war-file-compare";
    String OPTION_STAGING_FILE_PATH = "--staging";
    String OPTION_LOCALE = "--locale";
    String OPTION_OVERRIDE = "--override";
    String OPTION_OVERWRITE = "--overwrite";
}
