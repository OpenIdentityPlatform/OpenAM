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

package org.forgerock.openam.session.ha.amsessionstore.common;

/**
 *
 * Specific Constants for use within the OpenDJPersistentStore as our
 * Session Store.
 *
 * @author peter.major
 * @author steve
 */
public interface Constants extends com.sun.identity.shared.Constants {

    static final String PROPERTIES_FILE = "amsessiondb.properties";
    
    static final String PERSISTER_KEY = 
        "amsessiondb.amrecordpersister"; 
    
    static final String STATS_ENABLED = 
        "amsessiondb.enabled";


    static final String PORT = "amsessiondb.port";
    
    static final String SHUTDOWN_PORT = "amsessiondb.shutdown.port";
    
    static final String SHUTDOWN_ADDR = "amsessiondb.shutdown.addr";
    
    static final String URI = "amsessiondb.uri";
    
    static final String MIN_THREADS = "amsessiondb.min.threads";
    
    static final String MAX_THREADS = "amsessiondb.max.threads";
    
    static final String LOCAL = "local";
    
    static final String OPENDJ_ROOT = "amsessiondb.opendj.root";
    
    static final String OPENDJ_ADMIN_PORT = "amsessiondb.opendj.admin.port";
            
    static final String OPENDJ_LDAP_PORT = "amsessiondb.opendj.ldap.port";
    
    static final String OPENDJ_JMX_PORT = "amsessiondb.opendj.jmx.port";

    static final String OPENDJ_SUFFIX = "amsessiondb.opendj.suffix";
    
    static final String OPENDJ_REPL_PORT = "amsessiondb.opendj.repl.port";
    
    static final String OPENDJ_SUFFIX_TAG = "AMSESSIONDB_BASEDN";
    
    static final String OPENDJ_RDN_TAG = "AMSESSIONDB_RDN";

    
    static final String OPENDJ_DATASTORE = "opendj";
    
    static final String OPENDJ_DS_MGR_DN = "amsessiondb.opendj.ds_mgr_dn";
    
    static final String OPENDJ_DS_MGR_PASSWD = "amsessiondb.opendj.ds_mgr_passwd";
    
    static final String USERNAME = "amsessiondb.auth.username";
    
    static final String PASSWORD = "amsessiondb.auth.password";
    
    static final String EXISTING_SERVER_URL = "amsessiondb.exising.server.url";
    
    static final String HOST_URL = "amsessiondb.host.url";
    
    static final String HOST_PROTOCOL = "amsessiondb.host.protocol";
    
    static final String HOST_FQDN = "amsessiondb.host.fqdn";
    
    static final String HOST_PORT = "amsessiondb.host.port";
    
    static final String HOST_URI = "amsessiondb.host.uri";



    
    static final String BASE_DN = "ou=famrecords";
    
    static final String HOSTS_BASE_DN = "ou=amsessiondb";
    
    static final String HOST_NAMING_ATTR = "cn";
    
    static final String AMRECORD_NAMING_ATTR = "pKey";
    
    public final static String TOP = "top";
        
    public final static String FR_FAMRECORD = "frFamRecord";
    
    public final static String OBJECTCLASS = "objectClass";
    
    public final static String FR_AMSESSIONDB = "frAmSessionDb";
    
    public final static String FAMRECORD_FILTER = "(objectclass=*)";
    
    static final String COMMA = ",";
    
    static final String EQUALS = "=";




    // exit codes
    static final int EXIT_INVALID_URL = 1;
    
    static final int EXIT_INSTALL_FAILED = 2;
    
    static final int EXIT_REMOVE_FAILED = 3;

}
