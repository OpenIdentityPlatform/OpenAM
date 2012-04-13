/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CacheBlock.java,v 1.4 2008/06/25 05:41:23 qcheng Exp $
 *
 */

package com.iplanet.am.sdk.common;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;

/**
 * This class represents the value part stored in the AMCacheManager's cache.
 * Each CacheBlock object would represent a Directory entry. It caches the
 * attributes corresponding to that entry. It also keeps track of red other
 * details such as the Organization DN for the entry.
 * 
 * <p>
 * Also, this cache block can be used to serve as dummy block representing a
 * non-existent directory entry (negative caching). This prevents making
 * un-necessary directory calls for non-existent directory entries.
 * 
 * <p>
 * Since the attributes that can be retrieved depends on the principal
 * performing the operation (ACI's set), the result set would vary. The
 * attributes that are returned are the ones that are readable by the principal.
 * Each cache block keeps account of these differences in result sets by storing
 * all the attributes readable (and writable) on a per principal basis. This
 * information is stored in a PrincipalAccess object. In order to avoid
 * duplicate copy of the values, the all attribute values are not cached per
 * principal. A single copy of the attributes is stored in the CacheBlock
 * object. Also this copy of attributes stored in the cache block keeps track of
 * non-existent directory attributes (invalid attributes). This would also
 * prevent un-necessary directory calls for non-existent entry attributes.
 * 
 * The attribute copy is dirtied by removing the entries which get modified.
 */
public class CacheBlock extends CacheBlockBase {

    // CONSTANTS
    protected static final String ENTRY_EXPIRATION_ENABLED_KEY = 
        "com.iplanet.am.sdk.cache.entry.expire.enabled";

    protected static final String ENTRY_USER_EXPIRE_TIME_KEY = 
        "com.iplanet.am.sdk.cache.entry.user.expire.time";

    protected static final String ENTRY_DEFAULT_EXPIRE_TIME_KEY = 
        "com.iplanet.am.sdk.cache.entry.default.expire.time";

    protected static boolean ENTRY_EXPIRATION_ENABLED_FLAG = false;

    protected static long ENTRY_USER_EXPIRE_TIME;

    protected static long ENTRY_DEFAULT_EXPIRE_TIME;

    private Debug debug = Debug.getInstance("amProfile_ldap");

    static {
        initializeExpirationParms();
    }

    private static void initializeExpirationParms() {
        setEntryExpirationEnabledFlag();
        if (ENTRY_EXPIRATION_ENABLED_FLAG) {
            // Read the expiration times for user and non user entries
            setUserEntryExpirationTime();
            setDefaultEntryExpirationTime();
        }
    }

    private static void setEntryExpirationEnabledFlag() {
        String userEntryExpireTimeStr = SystemProperties.get(
                ENTRY_EXPIRATION_ENABLED_KEY, "false");
        ENTRY_EXPIRATION_ENABLED_FLAG = Boolean.valueOf(userEntryExpireTimeStr)
                .booleanValue();
    }

    private static void setUserEntryExpirationTime() {
        ENTRY_USER_EXPIRE_TIME = getPropertyIntValue(
                ENTRY_USER_EXPIRE_TIME_KEY, 15) * 60000; // Convert to
                                                            // milliseconds
    }

    private static void setDefaultEntryExpirationTime() {
        ENTRY_DEFAULT_EXPIRE_TIME = getPropertyIntValue(
                ENTRY_DEFAULT_EXPIRE_TIME_KEY, 30) * 60000; // Convert to
                                                            // milli-scds
    }

    private static int getPropertyIntValue(String key, int defaultValue) {
        int value = defaultValue;
        String valueStr = SystemProperties.get(key);
        if (valueStr != null && valueStr.trim().length() > 0) {
            try {
                value = Integer.parseInt(valueStr);
            } catch (NumberFormatException e) {
                value = defaultValue;
            }
        }
        return value;
    }

    public Debug getDebug() {
        return debug;
    }

    public boolean isEntryExpirationEnabled() {
        return ENTRY_EXPIRATION_ENABLED_FLAG;
    }

    public long getUserEntryExpirationTime() {
        return ENTRY_USER_EXPIRE_TIME;
    }

    public long getDefaultEntryExpirationTime() {
        return ENTRY_DEFAULT_EXPIRE_TIME;
    }

    public CacheBlock(String entryDN, boolean validEntry) {
        super(entryDN, validEntry);
    }

    public CacheBlock(String entryDN, String orgDN, boolean validEntry) {
        super(entryDN, orgDN, validEntry);
    }
}
