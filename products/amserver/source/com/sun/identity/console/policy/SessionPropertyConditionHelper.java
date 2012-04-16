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
 * $Id: SessionPropertyConditionHelper.java,v 1.3 2008/07/07 20:39:22 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.iplanet.jato.util.HtmlUtil;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.policy.plugins.SessionPropertyCondition;
import com.sun.web.ui.model.CCActionTableModel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SessionPropertyConditionHelper {
    static String PG_SESSION_PROPERTY_VALUES = "pgSessionPropertyValues";
    static String PG_SESSION_PROPERTY_NAME = "pgSessionPropertyName";
    static String ATTR_VALUES = "Values";
    static String CB_CASE = "cbCaseInsensitive";
    static String TBL_DATA_NAME = "tblPolicySessionDataName";
    static String TBL_DATA_ACTION = "tblPolicySessionHrefAction";
    static String TBL_DATA_VALUES = "tblPolicySessionValues";
    private static SessionPropertyConditionHelper instance =
        new SessionPropertyConditionHelper();


    private SessionPropertyConditionHelper() {
    }

    public static SessionPropertyConditionHelper getInstance() {
        return instance;
    }

    public String getConditionXML(boolean bCreate, boolean readonly) {
        String xml = null;

        if (bCreate) {
            xml =
            "com/sun/identity/console/propertyPMConditionSessionProperty.xml";
        } else {
            xml = (readonly) ?
            "com/sun/identity/console/propertyPMConditionSessionProperty_Readonly.xml" :
            "com/sun/identity/console/propertyPMConditionSessionProperty.xml";
        }
        return AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(xml));
    }

    public String getMissingValuesMessage() {
        return "policy.condition.missing.session.property.message";
    }

    public void populateTable(
        Map values,
        AMPropertySheetModel propertySheetModel
    ) {
        if ((values != null) && !values.isEmpty()) {
            CCActionTableModel tblModel = (CCActionTableModel)
                propertySheetModel.getModel(ATTR_VALUES);
            tblModel.clear();
            boolean first = true;
            for (Iterator i = values.keySet().iterator(); i.hasNext(); ) {
                String propName = (String)i.next();

                if (!propName.equals(
                    SessionPropertyCondition.VALUE_CASE_INSENSITIVE)
                ) {
                    Set val = (Set)values.get(propName);

                    if (first) {
                        first = false;
                    } else {
                        tblModel.appendRow();
                    }
                    tblModel.setValue(TBL_DATA_NAME, propName);
                    tblModel.setValue(TBL_DATA_ACTION, 
                        AMViewBeanBase.stringToHex(propName));
                    tblModel.setValue(TBL_DATA_VALUES, formatValues(val));
                }
            }
        }
    }

    private String formatValues(Set values) {
        StringBuilder buff = new StringBuilder();
        for (Iterator i = values.iterator(); i.hasNext(); ) {
            String val = (String)i.next();
            buff.append(HtmlUtil.escape(val));
            buff.append("<br />");
        }
        return buff.toString();
    }
}
