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
 * $Id: SystemInfoService.java,v 1.2 2009/11/21 02:26:32 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.services.systeminfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.ResourceBundle;

import com.sun.identity.diagnostic.base.core.ToolContext;
import com.sun.identity.diagnostic.base.core.ToolLogWriter;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.base.core.service.ServiceRequest;
import com.sun.identity.diagnostic.base.core.service.ServiceResponse;
import com.sun.identity.diagnostic.base.core.service.ToolService;
import com.sun.identity.diagnostic.base.services.systeminfo.utils.PatchData;
import com.sun.identity.diagnostic.base.services.systeminfo.utils.SystemInfoData;
import com.sun.identity.diagnostic.base.services.systeminfo.utils.ZoneData;

public class SystemInfoService implements SystemInfoConstants, ToolService {
    private SystemInfoProvider sysInfoProvider = null;
    private static IToolOutput toolOutWriter;
    private ResourceBundle rb;

    /**
     * Creates a new instance of SystemInfoService
     */
    public SystemInfoService() {
    }
    
    /**
     * Initialize the service with the tool context. This is called once 
     * during service activation.
     *
     * @param tContext ToolContext in which this service runs.
     */
    public void init(ToolContext tContext) {
        sysInfoProvider = getSystemInfoProvider();
        toolOutWriter = tContext.getOutputWriter();
        rb = ResourceBundle.getBundle(SYSTEM_RESOURCE_BUNDLE);
    }
    
    public void start() {
    }
    
    /**
     * This is method that processes the given request.
     *
     * @param sReq ServiceRequest object containing input params
     * @param sRes ServiceResponse object containg output results
     * @throws Exception if the exception occurs.
     */
    public void processRequest(ServiceRequest sReq, ServiceResponse sRes)
        throws Exception 
    {
        toolOutWriter.init(sRes, rb);
        toolOutWriter.printlnResult("service-start-msg");
        ToolLogWriter.log(rb,Level.INFO,"service-start-msg", null);
        HashMap systemInfoHM = null;
        HashSet commandSet = (HashSet)sReq.getCommandSet();
        for (Iterator j = commandSet.iterator(); j.hasNext();) {
            String command = (String)j.next();
            if (command!=null && command.equalsIgnoreCase("get-sys-info")) {
                systemInfoHM = sysInfoProvider.getSystemInformation();
                SystemInfoData sysInfoData = setToDB(systemInfoHM);             
                sysInfoData.toString(toolOutWriter);
            } 
	}
        toolOutWriter.printResult(sRes.getStatus());
        toolOutWriter.printResult("service-done-msg");
    }
    
    public SystemInfoData setToDB(HashMap systemInfoHM) {
        SystemInfoData sysInfoData = new SystemInfoData(); 
        if (systemInfoHM!=null) {
            if (systemInfoHM.get(sysInfoProvider.HOSTNAME)!= null) {
                sysInfoData.setHostName((String) 
                    systemInfoHM.get(sysInfoProvider.HOSTNAME));
            }
            if (systemInfoHM.get(sysInfoProvider.OSNAME)!=null) {
                sysInfoData.setOsName(((String)systemInfoHM.get(
                    sysInfoProvider.OSNAME)).replaceAll("SunOS", "Solaris"));
            }
            if (systemInfoHM.get(sysInfoProvider.OSCPUARCH)!=null) {
                sysInfoData.setOsArch(((String) systemInfoHM.get(
                    sysInfoProvider.OSCPUARCH)).replaceAll("i386", "x86"));
            }
            if (systemInfoHM.get(sysInfoProvider.DOMAINNAME)!=null) {
                sysInfoData.setDomainName((String) systemInfoHM.get(
                    sysInfoProvider.DOMAINNAME));
            }
            if (systemInfoHM.get(sysInfoProvider.IPADDRESS)!=null) {
                sysInfoData.setIpAddress((String) systemInfoHM.get(
                    sysInfoProvider.IPADDRESS));
            }
            if (systemInfoHM.get(sysInfoProvider.MEMORYINFO) != null) {
                sysInfoData.setRam(String.valueOf(systemInfoHM.get(
                    sysInfoProvider.MEMORYINFO)));
            }
            if (systemInfoHM.get(sysInfoProvider.SWAPINFO) != null) {
                sysInfoData.setSwap(String.valueOf(systemInfoHM.get(
                    sysInfoProvider.SWAPINFO)));
            }
            if (systemInfoHM.get(sysInfoProvider.OSVERSIONINFO) != null) {
                sysInfoData.setOsVersionNo((String)systemInfoHM.get(
                    sysInfoProvider.OSVERSIONINFO));
            }
            List patchList =
                (List)systemInfoHM.get(sysInfoProvider.OSPATCHLIST);
            List<PatchData> patches = new ArrayList();
            PatchData patch = null;
            HashMap patchMap = null;
            if (patchList != null && patchList.size() > 0) {
                for (int i = 0; i < patchList.size(); i++) {
                    patch = new PatchData();
                    patchMap = (HashMap) patchList.get(i);
                    patch.setPatchNumber((String) patchMap.get(PATCH));
                    patch.setObsoletes((String) patchMap.get(OBSOLETES));
                    patch.setRequires((String) patchMap.get(REQUIRES));
                    patch.setIncompatibles((String) patchMap.get(INCOMPATIBLES));
                    patch.setPackages((String) patchMap.get(PACKAGES));
                    patches.add(patch);
                }
            }
            sysInfoData.setPatches(patches);
            
            List zoneList = (List) systemInfoHM.get(sysInfoProvider.ZONELIST);
            List<ZoneData> zones = new ArrayList();
            ZoneData zone = null;
            HashMap zoneMap = null;
            if (zoneList != null && zoneList.size() > 0) {
                for (int i = 0; i < zoneList.size(); i++) {
                    zone = new ZoneData();
                    zoneMap = (HashMap) zoneList.get(i);
                    zone.setZoneId((String) zoneMap.get(ID));
                    zone.setZoneName((String) zoneMap.get(ZONE_NAME));
                    zone.setZonePath((String) zoneMap.get(ZONE_PATH));
                    zone.setStatus((String) zoneMap.get(ZONE_STATUS));
                    zones.add(zone);
                }
            }
            sysInfoData.setZones(zones);
        }
        return sysInfoData;
    }
    
    public void stop() {
    }
    
    /**
     * This method returns the instance of the appriopriate systeminfo provider
     *
     * @return instance of the appriopriate systeminfo provider.
     */
    private SystemInfoProvider getSystemInfoProvider() {
        if (isSolaris()) {
            return new SolarisSystemInfoProvider();  
        } else if (isLinux()) {
            return new LinuxSystemInfoProvider();
        } else if (isWindows()) {
            return new WindowsSystemInfoProvider();
        } else {
            return null;
        }
    }
    
    /**
     * This method checks whether the OS is Solaris
     *
     * @return <code>true</code> if OS is solaris
     *         false otherwise
     */
    private  static boolean isSolaris() {
        String OSName = System.getProperty("os.name");
        if ((OSName.indexOf("SunOS")) != -1) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * This method checks whether the OS is HPUX
     *
     * @return <code>true</code> if OS is HPUX
     *         false otherwise
     */
    private static boolean isHPUX() {
        String OSName = System.getProperty("os.name");
        if ((OSName.indexOf("HP-UX")) != -1) {
            return true;
        } else {
            return false;
        }
    } 
    
    /**
     * This method checks whether the OS is WINDOWS
     *
     * @return <code>true</code> if OS is WINDOWS
     *         false otherwise
     */
    private  static boolean isWindows() {
        String OSName = System.getProperty("os.name");
        if ((OSName.indexOf("Windows")) != -1) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * This method checks whether the OS is LINUX
     *
     * @return <code>true</code> if OS is LINUX
     *         false otherwise
     */
    private  static boolean isLinux() {
        String OSName = System.getProperty("os.name");
        if ((OSName.indexOf("Linux")) != -1) {
            return true;
        } else {
            return false;
        }
    }
}
