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
 * $Id: SystemInfoData.java,v 1.1 2008/11/22 02:24:33 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.services.systeminfo.utils;

import java.io.Serializable;
import java.util.List;

import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.base.services.systeminfo.SystemInfoConstants;

public class SystemInfoData extends Object implements SystemInfoConstants,
    Serializable {
    
    private String hostName;
    private String osName;
    private String osArch;
    private String domainName;
    private String ipAddress;
    private String ram;
    private String swap;
    private List<PatchData> patches;
    private List<ZoneData> zones;
    private String osVersionNo;
    
    public SystemInfoData() {
    }
    
    /**
     * Getter for property hostName.
     *
     * @return Value of property hostName.
     */
    public String getHostName() {
        return this.hostName;
    }
    
    /**
     * Setter for property hostName.
     *
     * @param hostName New value of property hostName.
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    
    /**
     * Getter for property osName.
     *
     * @return Value of property osName.
     */
    public String getOsName() {
        return this.osName;
    }
    
    /**
     * Setter for property osName.
     *
     * @param osName New value of property osName.
     */
    public void setOsName(String osName) {
        this.osName = osName;
    }
    
    /**
     * Getter for property osArch.
     *
     * @return Value of property osArch.
     */
    public String getOsArch() {
        return this.osArch;
    }
    
    /**
     * Setter for property osArch.
     *
     * @param osArch New value of property osArch.
     */
    public void setOsArch(String osArch) {
        this.osArch = osArch;
    }
    
    /**
     * Getter for property domainName.
     *
     * @return Value of property domainName.
     */
    public String getDomainName() {
        return this.domainName;
    }
    
    /**
     * Setter for property domainName.
     *
     * @param domainName New value of property domainName.
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
    
    /**
     * Getter for property ipAddress.
     *
     * @return Value of property ipAddress.
     */
    public String getIpAddress() {
        return this.ipAddress;
    }
    
    /**
     * Setter for property ipAddress.
     *
     * @param ipAddress New value of property ipAddress.
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    /**
     * Getter for property ram.
     *
     * @return Value of property ram.
     */
    public String getRam() {
        return this.ram;
    }
    
    /**
     * Setter for property ram.
     *
     * @param ram New value of property ram.
     */
    public void setRam(String ram) {
        this.ram = ram;
    }
    
    /**
     * Getter for property swap.
     *
     * @return Value of property swap.
     */
    public String getSwap() {
        return this.swap;
    }
    
    /**
     * Setter for property swap.
     *
     * @param swap New value of property swap.
     */
    public void setSwap(String swap) {
        this.swap = swap;
    }
    
    /**
     * Getter for property zones.
     *
     * @return Value of property zones.
     */
    public List<ZoneData> getZones() {
        return this.zones;
    }
    
    /**
     * Setter for property zones.
     *
     * @param zones values of host zones.
     */
    public void setZones(List<ZoneData> zones) {
        this.zones = zones;
    }
    
    /**
     * Getter for property patches.
     *
     * @return Value of property patches.
     */
    public List<PatchData> getPatches() {
        return this.patches;
    }
    
    /**
     * Setter for property zones.
     *
     * @param patches values of patches on host.
     */
    public void setPatches(List<PatchData> patches) {
        this.patches = patches;
    }
    
    /**
     * Getter for property for OS version.
     *
     * @return Value of OS version.
     */
    public String getOsVersionNo() {
        return osVersionNo;
    }
    
    /**
     * Setter for property for OS version.
     *
     * @param osVersionNo version of the OS.
     */
    public void setOsVersionNo(String osVersionNo) {
        this.osVersionNo = osVersionNo;
    }
    
    
    /**
     * Helper method to convert as string representation
     *
     * @return string representation of this object
     */
    public String toString(){
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        result.append(newLine);
        result.append("\n**** System Information Begin ****");
        result.append("\n----------------------------------");
        result.append("\nHost Machine Details");
        result.append("\n----------------------------------");
        result.append("\nHostName   : ").append(hostName);
        result.append("\nOS Name    : ").append(osName);
        result.append("\nOS Arch    : ").append(osArch);
        result.append("\nIP Address : ").append(ipAddress);
        result.append("\nDomain Name: ").append(domainName);
        result.append("\nRAM        : ").append(ram);
        result.append("\nSWAP       : ").append(swap);
        result.append("\nOS VERSION : ").append(osVersionNo);
        result.append("\nZONE(s)    : ");
        for (int i = 0; i < zones.size(); i++) {
            result.append("\n------------------------------------------------------\n");
            result.append(zones.get(i).toString());
            result.append("\n------------------------------------------------------");
        }
        result.append("\nPATCHES    : \n");
        result.append("==================================");
        for (int i = 0; i < patches.size(); i++) {
            result.append(patches.get(i).toString());
        }
        result.append("\n********System Information End ***********\n");
        return result.toString();
    }
    
    
    /**
     * Helper method to convert as string representation
     *
     * @param toolOutWriter the output writer for the tool
     */
    public void toString(IToolOutput toolOutWriter){
        String[] params = {SMALL_LINE, SMALL_LINE};
        toolOutWriter.printMessage("sys-info-title", params);
        toolOutWriter.printMessage("sys-info-host-name",
            new String[] {hostName});
        toolOutWriter.printMessage("sys-info-os-name",
            new String[] {osName});
        toolOutWriter.printMessage("sys-info-os-arch", new String[] {osArch});
        toolOutWriter.printMessage("sys-info-ip-addr",
            new String[] {ipAddress});
        toolOutWriter.printMessage("sys-info-domain",
            new String[] {domainName});
        toolOutWriter.printMessage("sys-info-ram", new String[] {ram});
        toolOutWriter.printMessage("sys-info-swap", new String[] {swap});
        toolOutWriter.printMessage("sys-info-os-ver",
            new String[] {osVersionNo});
        toolOutWriter.printMessage("sys-info-zone");
        for (int i = 0; i < zones.size(); i++) {
            toolOutWriter.printMessage(SMALL_LINE);
            toolOutWriter.printMessage(zones.get(i).toString());
            toolOutWriter.printMessage(SMALL_LINE);
        }
        toolOutWriter.printMessage("sys-info-patches");
        toolOutWriter.printMessage(DOUBLE_LINE);
        for (int i = 0; i < patches.size(); i++) {
            toolOutWriter.printMessage(patches.get(i).toString());
        }
        toolOutWriter.printMessage("sys-info-end");
    }
    
}
