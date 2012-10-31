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
 * $Id: PMDefaultAuthSchemeConditionAddViewBean.java,v 1.2 2008/06/25 05:43:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.html.Option;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.plugins.AuthSchemeCondition;
import com.sun.web.ui.view.html.CCSelect;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PMDefaultAuthSchemeConditionAddViewBean
    extends ConditionAddViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/PMDefaultAuthSchemeConditionAdd.jsp";

    public PMDefaultAuthSchemeConditionAddViewBean() {
        super("PMDefaultAuthSchemeConditionAdd", DEFAULT_DISPLAY_URL);
    }

    protected String getConditionXML(
        String curRealm,
        String condType,
        boolean readonly
    ) {
        return AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyPMConditionAuthScheme.xml"));
    }

    public void beginDisplay(DisplayEvent event)
            throws ModelControlException {
        super.beginDisplay(event);
        CCSelect sl = (CCSelect)getChild(AuthSchemeCondition.AUTH_SCHEME);
        OptionList optList = sl.getOptions();

        if (optList != null) {
            int size = optList.size();
            String emptyRealm = getModel().getLocalizedString(
                "policy.condition.null.realm");

            for (int i = 0; i < size; i++) {
                Option opt = optList.get(i);
                if (opt.getLabel().startsWith(":")) {
                    opt.setLabel(emptyRealm + opt.getLabel());
                }
            }
        }
    }

    protected String getMissingValuesMessage() {
        return "policy.condition.missing.auth.scheme";
    }

    protected void setPropertiesValues(Map values) {
        if ((values != null) && !values.isEmpty()) {
            for (Iterator i = values.keySet().iterator(); i.hasNext(); ) {
                String propName = (String)i.next();
                if (propName.equals(AuthSchemeCondition.AUTH_SCHEME)) {
                    Set val = (Set)values.get(propName);
                    if ((val != null) && !val.isEmpty()) {
                        AMModel model = getModel();
                        CCSelect sl = (CCSelect)getChild(
                                AuthSchemeCondition.AUTH_SCHEME);
                        sl.setOptions(createOptionList(getLabelValueMap(val)));
                        propertySheetModel.setValues(
                                propName, val.toArray(), model);
                    }
                }
            }
        }
    }

    protected Map getConditionValues(
        PolicyModel model,
        String realmName,
        String conditionType
    ) {
        Map values = super.getConditionValues(model, realmName, conditionType);
        setPropertiesValues(values);
        return values;
    }
}
