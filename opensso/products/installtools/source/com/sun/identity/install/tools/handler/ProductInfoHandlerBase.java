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
 * $Id: ProductInfoHandlerBase.java,v 1.2 2008/06/25 05:51:27 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.install.tools.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.sun.identity.install.tools.configurator.ConfigurationLoader;
import com.sun.identity.install.tools.configurator.I18NInfo;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.InstallRunInfo;
import com.sun.identity.install.tools.configurator.InteractionConstants;
import com.sun.identity.install.tools.configurator.InteractionInfo;
import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.util.List;

public class ProductInfoHandlerBase {

    public ProductInfoHandlerBase() {
        setI18NInfoMap(new HashMap());
    }

    public ArrayList getIFinderSummaryKeys() throws InstallException {

        ConfigurationLoader cl = new ConfigurationLoader();
        InstallRunInfo installRunInfo = cl.getInstallRunInfo();
        ArrayList iFInteractionsInfoList = installRunInfo
                .getInstanceFinderInteractions();
        int count = iFInteractionsInfoList.size();
        ArrayList summaryKeys = new ArrayList(count);

        for (int i = 0; i < count; i++) {
            InteractionInfo interactionInfo = (InteractionInfo) 
                iFInteractionsInfoList.get(i);
            I18NInfo i18NInfo = interactionInfo.getI18NInfo();
            String summaryKey = i18NInfo.getKey()
                    + InteractionConstants.STR_IN_SUMMARY_DESCRIPTION_SUFFIX;
            summaryKeys.add(summaryKey);
            getI18NInfoMap().put(summaryKey, i18NInfo);
        }

        return summaryKeys;
    }

    public void printProductDetails(Map productDetails, String instanceName) {
        Console.println();
        Console.println(LocalizedMessage.get(
                LOC_HR_MSG_PRODUCTINFO_DISP_HEADER,
                new Object[] { instanceName }));

        Iterator iter = productDetails.keySet().iterator();
        while (iter.hasNext()) {
            String summaryKey = (String) iter.next();
            String value = (String) productDetails.get(summaryKey);
            I18NInfo i18NInfo = (I18NInfo) getI18NInfoMap().get(summaryKey);
            String i18NGroup = i18NInfo.getGroup();
            String displayMessage = LocalizedMessage.get(summaryKey, i18NGroup)
                    .getMessage()
                    + STR_DISPLAY_SEPARATOR + value;
            Console.println(displayMessage);
        }
        Console.println();
    }

    public void printConsoleMessage(String message) {
        printConsoleMessage(message, null);
    }

    public void printConsoleMessage(String message, Object[] args) {
        LocalizedMessage lMessage;
        if (args != null) {
            lMessage = LocalizedMessage.get(message, args);
        } else {
            lMessage = LocalizedMessage.get(message);
        }
        Console.println();
        Console.println(lMessage);
        Console.println();
    }

    public String formatArgs(List arguments) {
        int count = arguments.size();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < count; i++) {
            String arg = (String) arguments.get(i);
            sb.append(arg).append(" ");
        }
        return sb.toString();
    }

    public Map getI18NInfoMap() {
        return i18NInfoMap;
    }

    private void setI18NInfoMap(Map map) {
        i18NInfoMap = map;
    }

    private Map i18NInfoMap;

    public static final String LOC_HR_MSG_PRODUCTINFO_DISP_HEADER = 
        "HR_MSG_PRODUCTINFO_DISP_HEADER";

    private static final String STR_DISPLAY_SEPARATOR = ": ";
}
