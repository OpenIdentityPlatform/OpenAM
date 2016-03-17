/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MastheadViewBean.java,v 1.1 2009/08/05 20:15:51 veiming Exp $
 *
 * Portions copyright 2016 ForgeRock AS.
 */

package com.sun.identity.console.version;

import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;

public class MastheadViewBean extends
    com.sun.web.ui.servlet.version.MastheadViewBean {

    public MastheadViewBean() {
        super();
    }

    /**
     * Gets the path for masthead logo.
     *
     * @return the path for masthead logo.
     */
    public String getMastheadLogo() {
        String logo = null;
        AMModel model = new AMModelBase(getRequestContext().getRequest(), getPageSessionAttributes());
        String consoleDirectory = model.getConsoleDirectory();
        logo = "../" + consoleDirectory + AMAdminConstants.IMAGES_PRIMARY_PRODUCT_NAME_PNG;
        return logo;
    }
}
