/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: InstallRunInfo.java,v 1.3 2008/08/29 20:23:39 leiming Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;

import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * @author ap74890
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class InstallRunInfo implements InstallConstants {

    public String toString() {
        StringBuffer buff = new StringBuffer("InstallRunInfo For: ");
        buff.append(getType()).append(LINE_SEP);
        buff.append("Welcome message: ").append(LINE_SEP);
        LocalizedMessage welcomeMesssage = LocalizedMessage.get(
                getWelcomeMessageInfo().getKey(), getWelcomeMessageInfo()
                        .getGroup());
        buff.append(welcomeMesssage.toString()).append(LINE_SEP);
        buff.append("Exit message: ").append(LINE_SEP);
        LocalizedMessage exitMesssage = LocalizedMessage.get(
                getExitMessageInfo().getKey(), 
                getExitMessageInfo().getGroup());
        buff.append(exitMesssage.toString()).append(LINE_SEP);
        buff.append("HomeDirLocator: ");
        buff.append(getHomeDirLocator()).append(LINE_SEP);
        buff.append("Instance Finder Interactions: ").append(LINE_SEP);
        ArrayList iiInteractions = getInstanceFinderInteractions();
        for (int i = 0; i < iiInteractions.size(); i++) {
            buff.append(iiInteractions.get(i)).append(LINE_SEP);
        }
        buff.append("Common Interactions: ").append(LINE_SEP);
        ArrayList commonInteractions = this.getCommonInteractions();
        for (int i = 0; i < commonInteractions.size(); i++) {
            buff.append(commonInteractions.get(i)).append(LINE_SEP);
        }
        buff.append("Instance Interactions: ").append(LINE_SEP);
        ArrayList instanceInteractions = this.getInstanceInteractions();
        for (int i = 0; i < instanceInteractions.size(); i++) {
            buff.append(instanceInteractions.get(i)).append(LINE_SEP);
        }
        buff.append("Common Tasks: ").append(LINE_SEP);
        ArrayList commonTasks = this.getCommonTasks();
        for (int i = 0; i < commonTasks.size(); i++) {
            buff.append("  ");
            buff.append(commonTasks.get(i)).append(LINE_SEP);
        }
        buff.append("Instance Tasks: ").append(LINE_SEP);
        ArrayList instanceTasks = this.getInstanceTasks();
        for (int i = 0; i < instanceTasks.size(); i++) {
            buff.append("  ");
            buff.append(instanceTasks.get(i)).append(LINE_SEP);
        }
        buff.append("Check optional-display attribute:" + isCheckDisplay()).
                append(LINE_SEP);
        buff.append("--------------------------------------------------");
        buff.append(LINE_SEP);
        return buff.toString();
    }

    public InstallRunInfo(boolean isInstall, String homeDirLocator,
            ArrayList instanceFinderInteractions, ArrayList commonInteractions,
            ArrayList instanceInteractions, ArrayList commonTasks,
            ArrayList instanceTasks, I18NInfo welcomeMess, I18NInfo exitMess,
            boolean checkDisplay) {
        setInstallFlag(isInstall);
        setHomeDirLocator(homeDirLocator);
        setInstanceFinderInteractions(instanceFinderInteractions);
        setCommonInteractions(commonInteractions);
        setInstanceInteractions(instanceInteractions);
        setCommonTasks(commonTasks);
        setInstanceTasks(instanceTasks);
        setWelcomeMessageInfo(welcomeMess);
        setExitMessageInfo(exitMess);
        setCheckDisplay(checkDisplay);
        
    }

    public String getHomeDirLocator() {
        return homeDirLocator;
    }

    public ArrayList getInstanceFinderInteractions() {
        return instanceFinderInteractions;
    }

    public ArrayList getCommonInteractions() {
        return commonInteractions;
    }

    public ArrayList getInstanceInteractions() {
        return instanceInteractions;
    }

    public ArrayList getCommonTasks() {
        return commonTasks;
    }

    public ArrayList getInstanceTasks() {
        return instanceTasks;
    }

    public boolean isInstall() {
        return isInstall;
    }

    public I18NInfo getWelcomeMessageInfo() {
        return welcomeMessageInfo;
    }

    public I18NInfo getExitMessageInfo() {
        return exitMessageInfo;
    }

    
    public boolean isCheckDisplay() {
        return checkDisplay;
    }
    
    private void setInstanceTasks(ArrayList tasks) {
        instanceTasks = tasks;
    }

    private void setCommonTasks(ArrayList tasks) {
        commonTasks = tasks;
    }

    private void setInstanceInteractions(ArrayList interactions) {
        instanceInteractions = interactions;
    }

    private void setCommonInteractions(ArrayList interactions) {
        commonInteractions = interactions;
    }

    private void setInstanceFinderInteractions(ArrayList interactions) {
        instanceFinderInteractions = interactions;
    }

    private void setHomeDirLocator(String homeDirLocator) {
        this.homeDirLocator = homeDirLocator;
    }

    private void setInstallFlag(boolean flag) {
        isInstall = flag;
    }

    private String getType() {
        return isInstall() ? "Install " : "Uninstall ";
    }

    private void setWelcomeMessageInfo(I18NInfo welcome) {
        welcomeMessageInfo = welcome;
    }

    private void setExitMessageInfo(I18NInfo exitMess) {
        exitMessageInfo = exitMess;
    }

    private void setCheckDisplay(boolean checkDisplay) {
        this.checkDisplay = checkDisplay;
    }

    private String homeDirLocator;

    private ArrayList instanceFinderInteractions;

    private ArrayList commonInteractions;

    private ArrayList instanceInteractions;

    private ArrayList commonTasks;

    private ArrayList instanceTasks;

    private boolean isInstall;

    private I18NInfo welcomeMessageInfo;

    private I18NInfo exitMessageInfo;
    
    private boolean checkDisplay;

}
