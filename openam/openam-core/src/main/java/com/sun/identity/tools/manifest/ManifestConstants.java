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
* $Id: ManifestConstants.java,v 1.3 2008/09/22 20:49:27 kevinserwin Exp $
*/

package com.sun.identity.tools.manifest;

public interface ManifestConstants {
    public static final String MANIFEST_CREATE_FILE = "file.dest.path";
    public static final String DRYRUN_OPTION = "option.dryrun";
    public static final String VERBOSE_OPTION = "option.verbose";
    public static final String DEFAULT_VERBOSE = "false";

    public static final String LATEST_WAR_FILE = "file.war.latest";
    public static final String PROPERTIES_FILE = "file.properties";   
    public static final String IDENTIFIER_ENTRY = "entry.identifier";
    public static final String DEFAULT_IDENTIFIER_ENTRY = "identifier";
    public static final String MANIFEST_PATTERN = "pattern.manifest";
    public static final String MANIFEST_FILE_NAME = "filename.manifest";
    public static final String DEFAULT_MANIFEST_FILE_NAME = "manifest.manifest";
    public static final String WILDCARD_CHAR = "pattern.wildcard";
    public static final String DEFAULT_MANIFEST_PATTERN = "*.manifest";
    public static final String DEFAULT_WILDCARD_CHAR ="*";
    public static final String DELETE_SUFFIX = "delete";
    public static final String ALL_SUFFIX = "all";
    public static final String IDENTIFIER_SEPARATOR = ".";
    public static final String VERSION_FILE = "file.version";    
    int BUFFER_SIZE = 8192;
    String SHA1 = "SHA1";
    String DEFAULT_RECURSIVE = "true";
    String EQUAL = "=";
    String FILE_SEPARATOR = "/";
    char DEFAULT_WILD_CARD = '*';
    String PATTERN_SEPARATOR = ",";
    String HEADER_FILE_PATH = "file.header.path";
    String SRC_FILE_PATH = "file.src.path";
    String DEST_FILE_PATH = "file.dest.path";
    String RECURSIVE = "file.recursive";
    String INCLUDE_PATTERN = "file.include";
    String EXCLUDE_PATTERN = "file.exclude";
    String MANIFEST_NAME = "name.manifest";
    String DIGEST_ALG = "digest.alg";
    String DIGEST_HANDLEJAR = "digest.handlejar";
    String DEFAULT_DIGEST_HANDLEJAR = "true";
    String DIGEST_HANDLEWAR = "digest.handlewar";
    String DEFAULT_DIGEST_HANDLEWAR = "true";
    String JAR_FILE_EXT = ".jar";
    String WAR_FILE_EXT = ".war";
    String OVERWRITE = "file.overwrite";
    String DEFAULT_OVERWRITE = "true";
    
}
