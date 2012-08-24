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

    static final String STATS_ENABLED = 
        "amsessiondb.enabled";

    static final String URI = "amsessiondb.uri";
    
    static final String BASE_DN = "ou=famrecords";
    
    static final String HOSTS_BASE_DN = "ou=amsessiondb";
    
    static final String HOST_NAMING_ATTR = "cn";
    
    static final String AMRECORD_NAMING_ATTR = "pKey";
    
    static final String TOP = "top";

    static final String FR_FAMRECORD = "frFamRecord";

    static final String OBJECTCLASS = "objectClass";

    static final String FR_AMSESSIONDB = "frAmSessionDb";

    static final String FAMRECORD_FILTER = "(objectclass=*)";


}
