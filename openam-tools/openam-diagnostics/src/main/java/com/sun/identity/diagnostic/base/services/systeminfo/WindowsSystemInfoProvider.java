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
 * $Id: WindowsSystemInfoProvider.java,v 1.1 2008/11/22 02:24:32 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.services.systeminfo;

import java.util.HashMap;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.diagnostic.base.core.utils.ProcessExecutor;

public class WindowsSystemInfoProvider extends SystemInfoProvider {
    
    public WindowsSystemInfoProvider() {
    }
    
    /**
     * This method gets the Windows system information
     *
     * @return sysDetailsHM Hashmap containing the Window system information.
     */
    public HashMap getSystemInformation() {
        HashMap sysDetailsHM = new HashMap();

        sysDetailsHM.put(HOSTNAME, getHostName());
        sysDetailsHM.put(DOMAINNAME, getDomainName());
        sysDetailsHM.put(OSNAME, getOSName());
        sysDetailsHM.put(OSVERSIONINFO, getOSReleaseNo());
        sysDetailsHM.put(OSCPUARCH, getOSCPUArch());
        sysDetailsHM.put(MEMORYINFO, checkRAMAvailability());
        sysDetailsHM.put(SWAPINFO, checkSwapAvailability());
        sysDetailsHM.put(IPADDRESS, getIPAddress());
        return sysDetailsHM;
    }
    
    
    /**
     * This method is called to get the available RAM space
     *
     * @return  RAMdetected_MB the available RAM space
     */
    private long checkRAMAvailability() {
        String cmdOp = "";
        String RAMdetected = "";
        long RAMdetected_MB = 0;
        try {
            //TODO: Get RAM information
        } catch (Exception ex) {
        }
        return RAMdetected_MB;
    }
    
    /**
     * This method gets the required swap space and queries the system
     * for the installed swap space.
     *
     * @return the Available Swap Memory
     */
    private long checkSwapAvailability() {
        String cmdOp = "";
        String swapDetected = "";
        long swapDetected_MB = 0;
        try {
            //TODO: Get Swap information
        } catch (Exception ex) {
        }
        return swapDetected_MB;
    }
    
    /**
     * This method is used to get the architecture of the system
     *
     * @return cpuArch returns the architecture of the system
     */
    public String getOSCPUArch() {
        String cpuArch = null;
        ProcessExecutor archexecutor =
            new ProcessExecutor(new String[]{"systeminfo"});
        try {
            String[] cpuArchCmd = archexecutor.execute(true);
            cpuArch = cpuArchCmd[0];
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "WindowsSystemInfoProvider.getOSCPUArch : " +
                "Exception in executing systeminfo command", ex);
        }
        return ((cpuArch != null) && (cpuArch.length() > 0)) ? cpuArch :
            System.getProperty("os.arch");
    }
    
    /**
     * This method is called to get the domain name.
     *
     * @return string domain Name string
     */
    public String getDomainNameFromCommand() {
        String domainName = null;
        String[] cmdOut = null;
        ProcessExecutor executor;
        String searchStr = "connection-specific dns";
        boolean found = false;
        try {
            executor = new ProcessExecutor(new String[]{"ipconfig"});
            cmdOut = executor.execute(true);
            for (int i = 0; i < cmdOut.length && !found; i++) {
                if (((cmdOut[i].toLowerCase()).trim()).startsWith(searchStr)) {
                    int idx  = cmdOut[i].indexOf(":");
                    if (idx != -1) {
                        domainName = cmdOut[i].substring(idx);
                        found = true;
                    }
                }
            }
            if ((domainName == null) || (domainName.length() == 0)) {
                found = false;
                executor = new ProcessExecutor(new String[]{"systeminfo"});
                cmdOut = executor.execute(true);
                searchStr = "domain";
                for (int i=0; i < cmdOut.length && !found; i++) {
                    if ((cmdOut[i].toLowerCase()).startsWith(searchStr)) {
                        int idx  = cmdOut[i].indexOf(":");
                        if (idx != -1) {
                            domainName = cmdOut[i].substring(idx);
                            found = true;
                        }
                    }
                }
            }
        } catch (Exception cmdExp) {
            Debug.getInstance(DEBUG_NAME).error(
                "WindowsSystemInfoProvider.getDomainNameFromCommand : " +
                "Exception in executing ipconfig command", cmdExp);
            domainName = "";
        }
        return domainName;
    }
}
