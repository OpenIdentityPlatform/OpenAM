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
 * $Id: SystemInfoProvider.java,v 1.1 2008/11/22 02:24:32 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.services.systeminfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.StringTokenizer;
 
import com.sun.identity.shared.debug.Debug;

/**
 * This is an abstract base class for system information
 */

public abstract class SystemInfoProvider implements SystemInfoConstants {
    
    public SystemInfoProvider() {
    }
    
    /**
     * Gets the system information based on platform
     */
    public abstract HashMap getSystemInformation();
    
    /**
     * Returns the domain name of the system 
     *
     * @return name of the domain.
     */
    public String getDomainName() {
        String myName = getHostName();
        String DName = "";
        DName = getDomainNameFromCommand();
        if ((!DName.equalsIgnoreCase("")) && 
            canHostBeReached(myName + '.' + DName)) {
            return DName;
        }
        return DName;
    }
    
    /**
     * Returns the host name of the system 
     *
     * @return name of the host.
     */
    public String getHostName() {
        String hostname = "";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            Debug.getInstance(DEBUG_NAME).error(
                "SystemInfoProvider.getHostName : " +
                    "Exception in getting hostname", uhe);
        } catch (SecurityException se) {
            Debug.getInstance(DEBUG_NAME).error(
                "SystemInfoProvider.getHostName: " +
                    "Security exception in getting hostname", se);
        }
        if (hostname.indexOf('.') != -1) {
            hostname = hostname.substring(0, hostname.indexOf('.'));
        }
        return hostname;
    }

    /**
     * Returns <code>true</code> if host is reachable, false otherwise
     *
     * @param fqHostName fully qualified host name
     * @return <code>true</code> if host is reachable, false otherwise
     */
    public boolean canHostBeReached(String fqHostName) {
        String ipDetected= "";
        try {
            ipDetected = InetAddress.getByName(fqHostName).getHostAddress();
        } catch (UnknownHostException uhe) {
            Debug.getInstance(DEBUG_NAME).error(
                "SystemInfoProvider.canHostBeReached: " +
                    "Exception in getting hostname", uhe);
            return false;
        } catch (SecurityException se) {
            Debug.getInstance(DEBUG_NAME).error(
                "SystemInfoProvider.canHostBeReached: " +
                    "Security exception in getting hostname", se);
            return false;
        }
        return true;
    }
    
    /**
     * Place holder method
     */
    public String getDomainNameFromCommand() {
        return null;
    }
    
    /**
     * Returns the IP address of the system.
     *
     * @return ip address as strig of the host.
     */
    public String getIPAddress() {
        String ipAddress = "";
        try {
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException uhe) {
            Debug.getInstance(DEBUG_NAME).error(
                "SystemInfoProvider.getIPAddress: " +
                    "Exception in getting hostname", uhe);
        } catch (SecurityException se) {
            Debug.getInstance(DEBUG_NAME).error(
                "SystemInfoProvider.getIPAddress: " +
                    "Security exception in getting hostname", se);
        }
        return ipAddress;
    }

    /**
     * Returns the operating system of the host system. 
     *
     * @return operating system of the host.
     */
    public String getOSName() {
        String OSName;
        OSName = System.getProperty("os.name");
        return OSName;
    }
    
    /**
     * Returns the operating system version. 
     *
     * @return operating system verion.
     */
    public String getOSReleaseNo() {
        String releaseNumberString;
        releaseNumberString = System.getProperty("os.version");
        if (releaseNumberString != null) {
            releaseNumberString = releaseNumberString.trim();
        }
        return releaseNumberString;
    }
    
    protected long convertSwapDetectedToMB(String swapDetected) 
        throws Exception
    {
        long swapDetected_no = 0;
        try {
            swapDetected_no = Long.parseLong(swapDetected);
        } catch (NumberFormatException nfe) {
            throw new Exception(
                "Number Format error in reading SWAP specification");
        }
        long swap_in_MB=0;
        swap_in_MB =  swapDetected_no / 1024; // free outputs data in kb
        return swap_in_MB ;
    }
    
    protected long convertRAMdetectedToMB(String RAMdetected) 
        throws Exception
    {
        StringTokenizer st = new StringTokenizer(RAMdetected," ");
        String RAMdetected_val = st.nextToken();
        String unit = st.nextToken();
        long RAMdetected_no = 0;
        try {
            RAMdetected_no = Long.parseLong(RAMdetected_val);
        } catch (NumberFormatException nfe) {
            throw new Exception(
                "Number Format error in reading RAM specification");
        }
        long RAM_in_MB=0;
        if (unit.startsWith("M") || unit.startsWith("m")) {
            RAM_in_MB = RAMdetected_no;
        } else if (unit.startsWith("K") || unit.startsWith("k")) {
            RAM_in_MB =  Math.round(RAMdetected_no/1024.00);
        } else if (unit.startsWith("T") || unit.startsWith("t")) {
            RAM_in_MB =  RAMdetected_no*1024;
        }
        return RAM_in_MB ;
    }
}
