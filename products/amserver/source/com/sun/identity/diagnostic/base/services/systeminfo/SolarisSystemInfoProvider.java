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
 * $Id: SolarisSystemInfoProvider.java,v 1.1 2008/11/22 02:24:32 ak138937 Exp $
 *
 */
package com.sun.identity.diagnostic.base.services.systeminfo;

import java.util.ArrayList;
import java.util.HashMap;

import com.sun.identity.diagnostic.base.core.utils.GenUtils;
import com.sun.identity.diagnostic.base.core.utils.ProcessExecutor;
import com.sun.identity.shared.debug.Debug;

public class SolarisSystemInfoProvider extends SystemInfoProvider {
    
    public SolarisSystemInfoProvider() {
    }
    
    /**
     * This method is called to get the domain name.
     *
     * @return string The domainname string
     */
    public String getDomainNameFromCommand() {
        int retValue;
        ProcessExecutor executor = new ProcessExecutor(
            new String[]{"/bin/domainname"});
        try {
            String[] domainName = executor.execute(true);
            return domainName[0];
        } catch(Exception ex) {
            return "";
        }
    }
    
    /**
     * This method gets the Solaris system information
     *
     * @return sysDetailsHM Hashmap containing the Solaris system 
     *                      information.
     */
    public HashMap getSystemInformation() {
        HashMap sysDetailsHM = new HashMap();
        
        sysDetailsHM.put(HOSTNAME, getHostName());
        sysDetailsHM.put(DOMAINNAME, getDomainName());
        sysDetailsHM.put(OSNAME, getOSName());
        sysDetailsHM.put(OSVERSIONINFO, getOSReleaseNo());
        sysDetailsHM.put(OSCPUARCH, getOSCPUArch());
        sysDetailsHM.put(OSPATCHLIST, getPatchDetectionResults());
        sysDetailsHM.put(ZONELIST, getAllZones());
        sysDetailsHM.put(MEMORYINFO, checkRAMAvailability());
        sysDetailsHM.put(SWAPINFO, checkSwapAvailability());
        sysDetailsHM.put(IPADDRESS, getIPAddress());
        return sysDetailsHM;
    }

    /**
     * This method is used to get the Patch detection results
     *
     * @return patchList containing Patch Present,Obsoletes,
     *                   Requires,Incompatibles and Packages
     *                   list.
     */
    private ArrayList getPatchDetectionResults() {
        PatchCommandExecutor pce = new PatchCommandExecutor();
        ArrayList patchList =  pce.createGeneralPatchList();
        ArrayList patchListInfo = new ArrayList();
        HashMap patchHM;
        if (patchList == null) {
            return patchList;
        }
        if (patchList.size() > 1) {
            for (int i=1; i<patchList.size(); i++) {
                patchHM = new HashMap();
                String pID = pce.getPatchFromToken(
                   (String)patchList.get(i), PATCHKEY);
                if (pID.length() > 0) {
                    if (pID != null) {
                        patchHM.put(PATCH, pID);
                    }
                    pID = pce.getPatchFromToken(
                       (String)patchList.get(i), OBSOLETESKEY);
                    if (pID != null) {
                        patchHM.put(OBSOLETES, pID);
                    }
                    pID = pce.getPatchFromToken(
                        (String)patchList.get(i), REQUIRESKEY);
                    if (pID != null) {
                        patchHM.put(REQUIRES, pID);
                    }
                    pID = pce.getPatchFromToken(
                       (String)patchList.get(i), INCOMPATIBLESKEY);
                    if (pID != null) {
                        patchHM.put(INCOMPATIBLES, pID);
                    }
                    pID = pce.getPatchFromToken(
                        (String)patchList.get(i), PACKAGESKEY);
                    if (pID != null) {
                        patchHM.put(PACKAGES, pID);
                    }
                    patchListInfo.add(patchHM);
                }
            }
        } else {
            return patchList;
        }
        return patchListInfo;
    }

    /**
     * This method is used to get the Patch detection results
     *
     * @param detail Indicates if detailed output is required
     * @return patchMap HashMap containing Patch_Present list,
     *                  Patch_Obseleted List,Patch_Requires 
     *                  List,Patch_Incompatible and Composed_Package
     *                  List.
     */
    private HashMap getPatchDetectionResults(boolean detail) {
        PatchCommandExecutor pce = new PatchCommandExecutor();
        HashMap patchMap=new HashMap();
        ArrayList patchPresentList = pce.createPatchPresentList();
        ArrayList patchObsoletedList = pce.createPatchObsoletedList();
        patchMap.put("PATCH_PRESENT", patchPresentList);
        patchMap.put("PATCH_OBSOLETED", patchObsoletedList);

        if (detail) {
            ArrayList patchRequiresList = pce.createPatchRequiresList();
            ArrayList patchIncompatibleList = pce.createPatchIncompatibleList();
            ArrayList patchPackageList = pce.createPackageList();
            patchMap.put("PATCH_REQUIRED", patchRequiresList);
            patchMap.put("PATCH_INCOMPATIBLE", patchIncompatibleList);
            patchMap.put("PATCH_PACKAGE", patchPackageList);
        } else {
            HashMap patchIncompatiblesMap = pce.createPatchIncompatiblesMap();
            patchMap.put("PATCH_INCOMPATIBLE", patchIncompatiblesMap);
        }
        return patchMap;
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
            cmdOp = executePrtConf();
            RAMdetected = parsePrtConfOpForMemory(cmdOp);
            RAMdetected_MB = convertRAMdetectedToMB(RAMdetected);
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "SolarisSystemInfoProvider.checkRAMAvailability: " +
                "Exception in getting RAM information", ex);
        }
        return RAMdetected_MB;
    }
    
   /**
    * This method places a system (OS) call to read the RAM space.
    *
    * @return the RAM space information
    * @throws Exception  when executing the RAM acript
    */
    private String executePrtConf() throws Exception {
        int result = -1;
        ProcessExecutor executor = new ProcessExecutor(
            new String[]{"/usr/sbin/prtconf"});
        try {
            String[] cmdOp = executor.execute(true);
            String retString = "";
            for (int i = 0; i < cmdOp.length; i++) {
                retString=retString+cmdOp[i];
            }
            return retString;
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "SolarisSystemInfoProvider.executePrtConf: " +
                "Exception in system call execution", ex);
            throw new Exception("Error in executing system call");
        }
    }
    
   /**
    * This method is called to get the total configured memory 
    * size from the RAM information.
    *
    * @param cmdOp String containing the RAM information
    * @return the Configured Memory size
    * @throws java.lang.Exception
    */
    private String parsePrtConfOpForMemory(String cmdOp) 
        throws Exception
    {
        String parsedRAM = GenUtils.getStringDelimited(
            cmdOp, "Memory size:", "System Peripherals");
        return parsedRAM.trim();
    }
    
    /**
     * This method gets the required swap space and queries the 
     * system for the installed swap space.
     *
     * @return the Available Swap Memory
     */
    public long checkSwapAvailability() {
        String cmdOp = "";
        String swapDetected = "";
        long swapDetected_MB = 0;
        try {
            cmdOp = executeSwapS();
            swapDetected_MB = parseSwapSOpForTotalSwap(cmdOp);
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "SolarisSystemInfoProvider.checkSwapAvailability: " +
                "Exception in getting swap information", ex);
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
    private String executeSwapS() throws Exception {
        String swapSCommand[] = new String[]{"/usr/sbin/swap", "-s"};
        int result = -1;
        ProcessExecutor executor = new ProcessExecutor(swapSCommand);
        try {
            String[] cmdOp = executor.execute(true);
            String retString = "";
            for (int i = 0; i < cmdOp.length; i++) {
                retString = retString + cmdOp[i];
            }
            return retString;
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "SolarisSystemInfoProvider.executeSwapS: " +
                "Swap command execution error", ex);
            throw new Exception("Swap command execution error.");
        }
    }
    
    private long parseSwapSOpForTotalSwap(String cmdOp) 
        throws Exception
    {
        try {
            ArrayList firstSplitList = GenUtils.seperateStringComponents(
                cmdOp, ",");
            ArrayList secondSplitList = GenUtils.seperateStringComponents(
                firstSplitList.get(0).toString(), "=");
            ArrayList thirdSplitList = GenUtils.seperateStringComponents(
                firstSplitList.get(1).toString(), " ");
            ArrayList fourthSplitList = GenUtils.seperateStringComponents(
                secondSplitList.get(1).toString().trim(), " ");
            String totalUsedSwapSpace = 
                fourthSplitList.get(0).toString().trim();
            String totalAvailableSwapSpace = 
                thirdSplitList.get(0).toString().trim();
            long totalUsedSwapSpace_no = 
                convertSwapDetectedToMB(totalUsedSwapSpace);
            long totalAvailableSwapSpace_no = 
                convertSwapDetectedToMB(totalAvailableSwapSpace);      
            return totalAvailableSwapSpace_no + totalUsedSwapSpace_no;
        } catch (IndexOutOfBoundsException e) {
            Debug.getInstance(DEBUG_NAME).error(
                "SolarisSystemInfoProvider.parseSwapSOpForTotalSwap: " +
                "Swap command output parsing  error", e);
            throw new Exception("Swap command output parsing error.");
        }
    }
    
    public long convertSwapDetectedToMB(String swapDetected) 
        throws Exception
    {
        /*
         * The passed value 'swapDetected' can be in one of the 
         * two forms: 641436k or 64143633.
         */
        long swapDetected_no = 0;
        long swap_in_MB=0;
        
        String swapDetected_val = 
            swapDetected.substring(0, swapDetected.length()-1);
        String unit = swapDetected.substring(
            swapDetected.length()-1, swapDetected.length());
        if (Character.isDigit(unit.charAt(0))) {
            try {
                swapDetected_no = Long.parseLong(swapDetected);
                //convert to MB as the supplied data is in bytes.
                swap_in_MB = swapDetected_no / (1024 * 1024); 
                return swap_in_MB;
            } catch (NumberFormatException nfe) {
                Debug.getInstance(DEBUG_NAME).error(
                    "SolarisSystemInfoProvider.convertSwapDetectedToMB: " +
                    "Number format error", nfe);
                throw new Exception(
                    "Number Format error in reading Swap specification");
            }
        }
        try {
            swapDetected_no = Long.parseLong(swapDetected_val);
        } catch (NumberFormatException nfe) {
            throw new Exception(
                "Number Format error in reading Swap specification");
        }
        
        if (unit.startsWith("M") || unit.startsWith("m")) {
            swap_in_MB = swapDetected_no;
        } else if (unit.startsWith("K") || unit.startsWith("k")) {
            swap_in_MB =  Math.round(swapDetected_no / 1024.00);
        } else if (unit.startsWith("T") || unit.startsWith("t")) {
            swap_in_MB =  swapDetected_no * 1024;
        }
        return swap_in_MB;
    }

    /**
     * This method is used to get the architecture of the system
     *
     * @return cpuArch returns the architecture of the system
     */    
    private String getOSCPUArch() {
        String cpuArch = "";
        ProcessExecutor archexecutor = new ProcessExecutor(
           new String[]{"/usr/bin/uname", "-p"});
        try {
            String[] cpuArchCmd = archexecutor.execute(true);
            cpuArch = cpuArchCmd[0];
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
               "SolarisSystemInfoProvider.getOSCPUArch: " +
                  "Exception in getting OS arch info", ex);
        }
        return cpuArch;
    }
    
    /**
     * This method is used to get the platform of the system
     *
     * @return platform returns the OS platform of the system
     */    
    private String getOSPlatform() {
        String platform = "";
        ProcessExecutor platformexecutor = new ProcessExecutor(
            new String[]{"/usr/bin/uname", "-i"});
        try {
            String[] platformCmd = platformexecutor.execute(true);
            platform = platformCmd[0];
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
               "SolarisSystemInfoProvider.getOSPlatform: " +
                  "Exception in getting system OS info", ex);
        }
        return platform;
    }
    
    /**
     * Get the zones of the Solaris system
     *
     * @return list of zones on the system 
     */
    private ArrayList getAllZones() {
        ArrayList zonesList = new ArrayList();
        ProcessExecutor zonesListexecutor = new ProcessExecutor(
            new String[]{"/usr/sbin/zoneadm" , "list", "-v"});
        try {
            String[] zones = zonesListexecutor.execute(true);
            for (int i = 0; i < zones.length; ++i) {
                zonesList.add(zones[i]);
            }
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
               "SolarisSystemInfoProvider.getAllZones: " +
                  "Exception in getting zone info", ex);
        }
        String zoneId = "";
        String zoneName = "";
        String zoneStatus = "";
        String zonePath = "";
        ArrayList zoneListInfo = new ArrayList();
        ArrayList zoneInfo = new ArrayList();
        HashMap zoneHM = new HashMap();
        if (zonesList == null) {
            return zonesList;
        }
        if (zonesList.size() > 1) {
            for (int i = 1; i < zonesList.size(); i++) {
                zoneHM = new HashMap();
                zoneInfo = GenUtils.seperateStringComponents(
                    (String)zonesList.get(i), " ");
                if (zoneInfo.size() == 4) {
                    if ((zoneInfo.get(0)) != null) {
                        zoneHM.put(ID, zoneInfo.get(0));
                    }
                    if ((zoneInfo.get(1)) != null) {
                        zoneHM.put(ZONE_NAME, zoneInfo.get(1));
                    }
                    if ((zoneInfo.get(2)) != null) {
                        zoneHM.put(ZONE_STATUS, zoneInfo.get(2));
                    }
                    if ((zoneInfo.get(3)) != null) {
                        zoneHM.put(ZONE_PATH, zoneInfo.get(3));
                    } 
                    zoneListInfo.add(zoneHM);
                } 
            }
        } else{
            return zonesList;
        }
        return zoneListInfo;
    }
}
