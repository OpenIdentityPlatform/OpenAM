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
 * $Id: LinuxSystemInfoProvider.java,v 1.1 2008/11/22 02:24:32 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.services.systeminfo;

import java.util.ArrayList;
import java.util.HashMap;

import com.sun.identity.diagnostic.base.core.utils.GenUtils;
import com.sun.identity.diagnostic.base.core.utils.ProcessExecutor;
import com.sun.identity.shared.debug.Debug;

public class LinuxSystemInfoProvider extends SystemInfoProvider {
    
    public LinuxSystemInfoProvider() {
    }
    
    /**
     * This method gets the Linux system information
     *
     * @return sysDetailsHM Hashmap containing the Linux system information.
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
    public long checkRAMAvailability() {
        String cmdOp = "";
        String RAMdetected = "";
        long RAMdetected_MB = 0;
        try {
            cmdOp = getMemoryInfo();
            RAMdetected = parseMemoryInfo(cmdOp);
            RAMdetected_MB = convertRAMdetectedToMB(RAMdetected);
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "LinuxSystemInfoProvider.checkRAMAvailability : " +
                "Exception in RAM information", ex);
        }
        return RAMdetected_MB;
    }
    
    /**
     * This method places a system (OS) call to read the RAM space.
     *
     * @return memory information on the system
     * @throws Exception when executing the RAM script
     */
    private String getMemoryInfo() throws Exception{
        String[] meminfo_cmd = new String[]{"/bin/cat","/proc/meminfo"};
        ProcessExecutor executor = new ProcessExecutor(meminfo_cmd);
        try {
            String[] cmdOp = executor.execute(true);
            String retString = "";
            for (int i = 0; i < cmdOp.length; i++) {
                if (cmdOp[i].indexOf(MEMINFO_PREFIX) != -1) {
                    retString=cmdOp[i];
                    break;
                }
            }
            return retString;
        } catch(Exception ex) {
            throw new Exception("Cannot get the memory information");
        }
    }
    
    /**
     * This method is called to get the total configured memory size from
     * the RAM information
     *
     * @param cmdOp String containing the RAM information
     * @return the Configured Memory size
     * @throws java.lang.Exception
     */
    private String parseMemoryInfo(String memInfo){
        return memInfo.substring(memInfo.indexOf(MEMINFO_PREFIX)+
            MEMINFO_PREFIX.length()).trim();
    }
    
    /**
     * This method gets the required swap space and queries the system
     * for the installed swap space.
     *
     * @return the Available Swap Memory
     */
    public long checkSwapAvailability(){
        String cmdOp = "";
        String swapDetected = "";
        long swapDetected_MB = 0;
        try {
            cmdOp = getSwapInfo();
            swapDetected = parseSwapInfo(cmdOp);
            swapDetected_MB = convertSwapDetectedToMB(swapDetected);
        } catch(Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "LinuxSystemInfoProvider.checkSwapAvailability: " +
                "Exception in getting system swap space", ex);
        }
        return swapDetected_MB;
    }
    
    /**
     * This is the method that places a system (OS) call to read
     * the swap space.
     *
     * @return retString Swapinformation
     * @throws Exception error while processing the command
     */
    private String getSwapInfo() throws Exception{
        String[] SWAPINFO_CMD = new String[]{"/usr/bin/free"};
        ProcessExecutor executor = new ProcessExecutor(SWAPINFO_CMD);
        try {
            String[] cmdOp = executor.execute(true);
            String retString = "";
            for (int i = 0; i < cmdOp.length; i++) {
                if (cmdOp[i].indexOf(SWAPINFO_PREFIX) != -1) {
                    retString=cmdOp[i];
                    break;
                }
            }
            return retString;
        } catch (Exception ex) {
            throw new Exception("Cannot obtain the system swap space.");
        }
    }
    
    private String parseSwapInfo(String swapInfo) throws Exception {
        String[] SWAPINFO_CMD = new String[]{"/usr/bin/free"};
        try {
            ArrayList firstSplitList =
                GenUtils.seperateStringComponents(swapInfo, " ");
            return firstSplitList.get(1).toString().trim();
        } catch (IndexOutOfBoundsException e) {
            throw new Exception("Exception in parsing swap info");
        }
    }
    
    /**
     * This method is used to get the architecture of the system
     *
     * @return cpuArch returns the architecture of the system
     */
    public String getOSCPUArch() {
        String cpuArch="";
        ProcessExecutor archexecutor =
            new ProcessExecutor(new String[]{"/bin/uname", "-m"});
        try {
            String[] cpuArchCmd = archexecutor.execute(true);
            cpuArch = cpuArchCmd[0];
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "LinuxSystemInfoProvider.getOSCPUArch: " +
                "Exception in getting cpu architecture", ex);
        }
        return cpuArch;
    }
    
    /**
     * This method is called to get the domain name.
     *
     * @return string The domainname string
     */
    public String getDomainNameFromCommand() {
        int retValue;
        ProcessExecutor executor =
            new ProcessExecutor(new String[]{"/bin/domainname"});
        try {
            String[] domainName = executor.execute(true);
            return domainName[0];
        } catch (Exception ex) {
            return "";
        }
    }
}
