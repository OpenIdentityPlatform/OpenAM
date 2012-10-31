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
 * $Id: SMG11NModelImpl.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.identity.console.property.PropertyXMLBuilder;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/* - NEED NOT LOG - */

public class SMG11NModelImpl
    extends AMServiceProfileModelImpl
    implements AMServiceProfileModel
{
    public static final String SERVICE_NAME =
        "iPlanetG11NSettings";
    public static final String ATTRIBUTE_NAME_SUPPORTED_CHARSETS =
        "sun-identity-g11n-settings-locale-charset-mapping";
    public static final String ATTRIBUTE_NAME_CHARSET_ALIAS =
        "sun-identity-g11n-settings-charset-alias-mapping";
    public static final String LOCALE_PREFIX = "locale=";
    public static final String CHARSETS_PREFIX = "charset=";
    public static final String MIMENAME_PREFIX = "mimeName=";
    public static final String JAVANAME_PREFIX = "javaName=";

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public SMG11NModelImpl(HttpServletRequest req, Map map
        ) throws AMConsoleException {
        super(req, SERVICE_NAME, map);
    }

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param serviceName Name of Service.
     * @param map of user information
     */
    public SMG11NModelImpl(HttpServletRequest req, String serviceName, Map map
        ) throws AMConsoleException {
        super(req, serviceName, map);
    }

    /**
     * Returns the XML for property sheet view component.
     *
     * @param realmName Name of Realm.
     * @param viewbeanClassName Class Name of View Bean.
     * @param serviceName Name of Service.
     * @return the XML for property sheet view component.
     * @throws AMConsoleException if XML cannot be created.
     */
    public String getPropertySheetXML(
        String realmName,
        String viewbeanClassName,
        String serviceName
    ) throws AMConsoleException {
        String xml = super.getPropertySheetXML(
            realmName, viewbeanClassName, serviceName);
        xml = PropertyXMLBuilder.removeSubSection(
            xml, ATTRIBUTE_NAME_SUPPORTED_CHARSETS,
            TBL_SUPPORTED_CHARSETS_XML);
        return PropertyXMLBuilder.removeSubSection(
            xml, ATTRIBUTE_NAME_CHARSET_ALIAS, TBL_CHARSET_ALIAS_XML);
    }

    private static String TBL_SUPPORTED_CHARSETS_XML =
        "<property span=\"true\"><cc name=\"sun-identity-g11n-settings-locale-charset-mapping\" tagclass=\"com.sun.web.ui.taglib.table.CCActionTableTag\" > <attribute name=\"title\" value=\"globalization.service.table.SupportedCharsets.name\" /><attribute name=\"empty\" value=\"globalization.service.table.SupportedCharsets.noentries\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"selectionType\" value=\"multiple\" /><attribute name=\"selectionJavascript\" value=\"toggleTblButtonState('SMG11N', 'SMG11N.sun-identity-g11n-settings-locale-charset-mapping', 'supportedCharsetsCount', 'SMG11N.tblSupportedCharsetsButtonDelete', this)\" /><attribute name=\"showAdvancedSortingIcon\" value=\"false\" /><attribute name=\"showLowerActions\" value=\"false\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"showPaginationIcon\" value=\"false\" /><attribute name=\"showSelectionIcons\" value=\"true\" /><attribute name=\"showSelectionSortIcon\" value=\"false\" /><attribute name=\"showSortingRow\" value=\"true\" /></cc><fieldhelp name=\"tblSupportedCharsetsColLocaleHelp\" defaultValue=\"a100.help\" /></property>";

    private static String TBL_CHARSET_ALIAS_XML =
        "<property span=\"true\"><cc name=\"sun-identity-g11n-settings-charset-alias-mapping\" tagclass=\"com.sun.web.ui.taglib.table.CCActionTableTag\" > <attribute name=\"title\" value=\"globalization.service.table.CharsetAlias.name\" /><attribute name=\"empty\" value=\"globalization.service.table.CharsetAlias.noentries\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"selectionType\" value=\"multiple\" /><attribute name=\"selectionJavascript\" value=\"toggleTblButtonState('SMG11N', 'SMG11N.sun-identity-g11n-settings-charset-alias-mapping', 'charsetAliasCount', 'SMG11N.tblCharsetAliasButtonDelete', this)\" /><attribute name=\"showAdvancedSortingIcon\" value=\"false\" /><attribute name=\"showLowerActions\" value=\"false\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"showPaginationIcon\" value=\"false\" /><attribute name=\"showSelectionIcons\" value=\"true\" /><attribute name=\"showSelectionSortIcon\" value=\"false\" /><attribute name=\"showSortingRow\" value=\"true\" /></cc><fieldhelp name=\"help-pap-config\" defaultValue=\"a101.help\" /></property>";
}
