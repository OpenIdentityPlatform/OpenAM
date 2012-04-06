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
 * $Id: ToolsOptionsInfo.java,v 1.2 2008/06/25 05:51:17 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.admin;

import com.sun.identity.install.tools.util.LocalizedMessage;

public class ToolsOptionsInfo {

    public ToolsOptionsInfo(String handlerClass, String option,
            LocalizedMessage description) {
        this(handlerClass, option, description, true);
    }

    public ToolsOptionsInfo(String handlerClass, String option,
            LocalizedMessage description, boolean licenseRequired) {
        setHandlerClass(handlerClass);
        setOption(option);
        setDescription(description);
        setLicenseRequiredFlag(licenseRequired);
    }

    public String getOption() {
        return option;
    }

    public LocalizedMessage getDescription() {
        return description;
    }

    public String getHandlerClass() {
        return handlerClass;
    }

    public boolean isLicenseCheckRequired() {
        return licenseRequired;
    }

    private void setOption(String option) {
        this.option = option;
    }

    private void setDescription(LocalizedMessage description) {
        this.description = description;
    }

    private void setHandlerClass(String handlerClass) {
        this.handlerClass = handlerClass;
    }

    private void setLicenseRequiredFlag(boolean licenseRequired) {
        this.licenseRequired = licenseRequired;
    }

    private String option;

    private LocalizedMessage description;

    private String handlerClass;

    private boolean licenseRequired;
}
