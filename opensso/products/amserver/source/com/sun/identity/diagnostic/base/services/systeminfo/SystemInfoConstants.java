/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SystemInfoConstants.java,v 1.2 2009/08/03 23:57:59 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.services.systeminfo;

import com.sun.identity.diagnostic.base.core.common.ToolConstants;

/**
 * This interface contains the property names used by the
 * System configuration service
 */
public interface SystemInfoConstants extends ToolConstants {

    /**
     * Resource file name used by System service
     */
    final String SYSTEM_RESOURCE_BUNDLE = 
        "com.sun.identity.diagnostic.base.resources.locale.SystemInfo";

    /**
     * Property strings for system information.
     */
     public static final String HOSTNAME = "HOSTNAME";
     public static final String DOMAINNAME = "DOMAINNAME";
     public static final String OSNAME = "OSNAME";
     public static final String OSVERSIONINFO = "OSVERSIONINFO";
     public static final String OSCPUARCH = "OSCPUARCH";
     public static final String OSPATCHLIST = "OSPATCHLIST";
     public static final String ZONELIST = "ZONELIST";
     public static final String MEMORYINFO = "RAMMEMORYINFO";
     public static final String SWAPINFO = "SWAPINFO";
     public static final String IPADDRESS = "IPADDRESS";

     /**
      * Property string for swap information prefix.
      */
     public static final String SWAPINFO_PREFIX = "Swap:";

     /**
      * Property strings for memory information prefix.
      */
     public static final String MEMINFO_PREFIX = "MemTotal:";

     /**
      * Property strings for patch command output.
      */
      public static final String PATCHKEY = "Patch:";
      public static final String OBSOLETESKEY = "Obsoletes:";
      public static final String REQUIRESKEY = "Requires:";
      public static final String INCOMPATIBLESKEY = "Incompatibles:";
      public static final String PACKAGESKEY = "Packages:";

     /**
      * Property strings for patch command 
      */
      public static final String PATCH = "Patch";
      public static final String OBSOLETES = "Obsoletes";
      public static final String REQUIRES = "Requires";
      public static final String INCOMPATIBLES = "Incompatibles";
      public static final String PACKAGES = "Packages";

     /**
      * Property strings for zones 
      */
      public static final String ID = "id";
      public static final String ZONE_NAME = "name";
      public static final String ZONE_PATH = "zonepath";
      public static final String ZONE_STATUS = "status";

     /**
      * Property strings for formatting 
      */
      public static final String SMALL_LINE = 
          "----------------------------------";
      public static final String BIG_LINE = 
          "------------------------------------------------------";
      public static final String DOUBLE_LINE = 
          "==================================";
}
