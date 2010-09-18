/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Options.java,v 1.7 2009/01/05 23:17:09 veiming Exp $
 *
 */
package com.sun.identity.config;

import com.sun.identity.config.util.TemplatedPage;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.EmbeddedOpenDS;
import net.sf.click.control.ActionLink;

public class Options extends TemplatedPage {

    public ActionLink createConfigLink = new ActionLink("upgradeLink", this, "upgrade" );
    public ActionLink testUrlLink = new ActionLink("coexistLink", this, "coexist" );
    public ActionLink pushConfigLink = new ActionLink("olderUpgradeLink", this, "olderUpgrade" );

    protected boolean passwordUpdateRequired = true;
    protected boolean upgrade = false;
    protected boolean isOpenDS1x = false;
    
    private java.util.Locale configLocale = null;
    
    protected String getTitle() {
        return "configOptions.title";
    }

    public void doInit() {
        passwordUpdateRequired = getConfigurator().isPasswordUpdateRequired();
        addModel("passwordUpdateRequired",
            Boolean.valueOf( passwordUpdateRequired ) );
        upgrade = !getConfigurator().isNewInstall();
        addModel( "upgrade", Boolean.valueOf( upgrade ) );

        isOpenDS1x = EmbeddedOpenDS.isOpenDSVer1Installed();
        addModel("isOpenDS1x", Boolean.valueOf(isOpenDS1x));

        if (isOpenDS1x) {
            addModel("odsdir", AMSetupServlet.getBaseDir());
        }
    }

    public boolean upgrade() {
        try {
            getConfigurator().upgrade();
            writeToResponse("true");
        } catch ( Exception e ) {
            writeToResponse( e.getMessage());
        }
        setPath(null);
        return false;
    }

    public boolean coexist() {
        try {
            getConfigurator().coexist();
            writeToResponse("true");
        } catch ( Exception e ) {
            writeToResponse(e.getMessage());
        }
        setPath(null);
        return false;
    }

    public boolean olderUpgrade() {
        try {
            getConfigurator().olderUpgrade();
            writeToResponse("true");
        } catch ( Exception e ) {
            writeToResponse(e.getMessage());
        }
        setPath(null);
        return false;
    }
}
