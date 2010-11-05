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
 * $Id: ExistConf.java,v 1.3 2008/06/25 05:42:31 qcheng Exp $
 *
 */
package com.sun.identity.config;

import com.sun.identity.config.util.TemplatedPage;
import net.sf.click.control.ActionLink;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Les Hazlewood
 */
public class ExistConf extends TemplatedPage {

    public static final String INDEX_PARAM_NAME = "indices";
    public static final String INDEX_DELIMITER = ",";

    public ActionLink writeConfigLink = new ActionLink("writeConfig", this, "writeConfig" );

    private List existingConfigs = null;

    protected String getTitle() {
        return "existConf.title";
    }

    public void doInit() {
        existingConfigs = getConfigurator().getExistingConfigurations();
        if ( existingConfigs == null ) {
            existingConfigs = new ArrayList();
        }
        addModel( "existingConfigs", existingConfigs );
    }

    private Integer integer( String sval ) {
        try {
            return Integer.valueOf( sval );
        } catch ( NumberFormatException e ) {
            return null;
        }
    }

    public boolean writeConfig() {
        //comma delimited values of the indices of the original list (easier than encoding/decoding urls):
        String commaDelimited = toString( INDEX_PARAM_NAME );
        if ( commaDelimited == null ) {
            writeInvalid("Invalid access.");
            setPath(null);
            return false;
        }

        String[] indicesArray = commaDelimited.split( INDEX_DELIMITER );
        List indices = new ArrayList(indicesArray.length);
        for( int i = 0; i < indicesArray.length; i++ ) {
            Integer index = integer(indicesArray[i]);
            if ( index == null ) {
                writeInvalid("Invalid access." );
                setPath(null);
                return false;
            } else {
                indices.add( index );
            }
        }

        List configs = new ArrayList(indices.size());
        for( int i = 0; i < indices.size(); i++ ) {
            configs.add( existingConfigs.get(i) );
        }

        try {
            getConfigurator().writeConfiguration( configs );
            writeValid("Configuration complete.");
        } catch ( Exception e ) {
            writeInvalid(e.getMessage());
        }
        setPath(null);
        return false;
    }
}
