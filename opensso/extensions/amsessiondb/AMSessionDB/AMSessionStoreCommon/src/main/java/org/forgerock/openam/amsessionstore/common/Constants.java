/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.amsessionstore.common;

/**
 *
 * @author peter.major
 * @author steve
 */
public final class Constants {
    public static final String PROPERTIES_FILE = "amsessiondb.properties";
    
    public static final String PERSISTER_KEY = 
        "amsessiondb.amrecordpersister"; 
    
    public static final String STATS_ENABLED = 
        "amsessiondb.enabled";
    
    public static final String PORT = "amsessiondb.port";
    
    public static final String SHUTDOWN_PORT = "amsessiondb.shutdown.port";
    
    public static final String SHUTDOWN_ADDR = "amsessiondb.shutdown.addr";
    
    public static final String URI = "amsessiondb.uri";
    
    public static final String MIN_THREADS = "amsessiondb.min.threads";
    
    public static final String MAX_THREADS = "amsessiondb.max.threads";
    
    public static final String LOCAL = "local";
    
    public static final String OPENDJ_ROOT = "amsessiondb.opendj.root";
    
    public static final String OPENDJ_ADMIN_PORT = "amsessiondb.opendj.admin.port";
            
    public static final String OPENDJ_LDAP_PORT = "amsessiondb.opendj.ldap.port";
    
    public static final String OPENDJ_JMX_PORT = "amsessiondb.opendj.jmx.port";

    public static final String OPENDJ_SUFFIX = "amsessiondb.opendj.suffix";
    
    public static final String OPENDJ_REPL_PORT = "amsessiondb.opendj.repl.port";
    
    public static final String OPENDJ_SUFFIX_TAG = "AMSESSIONDB_BASEDN";
    
    public static final String OPENDJ_RDN_TAG = "AMSESSIONDB_RDN";
    
    public static final String OPENDJ_DATASTORE = "opendj";
    
    public static final String OPENDJ_DS_MGR_DN = "amsessiondb.opendj.ds_mgr_dn";
    
    public static final String OPENDJ_DS_MGR_PASSWD = "amsessiondb.opendj.ds_mgr_passwd";
    
    public static final String USERNAME = "amsessiondb.auth.username";
    
    public static final String PASSWORD = "amsessiondb.auth.password";
    
    public static final String EXISTING_SERVER_URL = "amsessiondb.exising.server.url";
    
    public static final String HOST_URL = "amsessiondb.host.url";
    
    public static final String HOST_PROTOCOL = "amsessiondb.host.protocol";
    
    public static final String HOST_FQDN = "amsessiondb.host.fqdn";
    
    public static final String HOST_PORT = "amsessiondb.host.port";
    
    public static final String HOST_URI = "amsessiondb.host.uri";
    
    public static final String BASE_DN = "ou=famrecords";
    
    public static final String HOSTS_BASE_DN = "ou=amsessiondb";
    
    public static final String HOST_NAMING_ATTR = "cn";
    
    public static final String AMRECORD_NAMING_ATTR = "pKey";
    
    public final static String TOP = "top";
        
    public final static String FR_FAMRECORD = "frFamRecord";
    
    public final static String OBJECTCLASS = "objectClass";
    
    public final static String FR_AMSESSIONDB = "frAmSessionDb";
    
    public final static String FAMRECORD_FILTER = "(objectclass=*)";
    
    public static final String COMMA = ",";
    
    public static final String EQUALS = "=";
    
    // exit codes
    public static final int EXIT_INVALID_URL = 1;
    
    public static final int EXIT_INSTALL_FAILED = 2;
    
    public static final int EXIT_REMOVE_FAILED = 3;
}
