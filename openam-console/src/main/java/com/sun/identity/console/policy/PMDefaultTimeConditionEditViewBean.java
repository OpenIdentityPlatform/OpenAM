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
 * $Id: PMDefaultTimeConditionEditViewBean.java,v 1.2 2008/06/25 05:43:03 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.console.policy.model.TimePolicyModelImpl;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.plugins.SimpleTimeCondition;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class PMDefaultTimeConditionEditViewBean
    extends ConditionEditViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/PMDefaultTimeConditionEdit.jsp";
    public static TimeConditionHelper helper =
        TimeConditionHelper.getInstance();

    public PMDefaultTimeConditionEditViewBean() {
        super("PMDefaultTimeConditionEdit", DEFAULT_DISPLAY_URL);
    }

    protected String getConditionXML(
        String curRealm,
        String condType,
        boolean readonly
    ) {
        return helper.getConditionXML(false, readonly);
    }

    protected String getMissingValuesMessage() {
        return helper.getMissingValuesMessage();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new TimePolicyModelImpl(req, getPageSessionAttributes());
    }

    protected void createTableModel() {
        helper.setTimeZoneOptions(canModify, this, getModel());
    }

    protected Map getValues(String conditionType)
        throws ModelControlException {
        PolicyModel model = (PolicyModel)getModel();
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        Map values = getConditionValues(model, realmName, conditionType);
        return helper.getConditionValues(this, values);
    }

    protected Map getDefaultValues() {
        Map values = null;

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            String conditionName = (String)getPageSessionAttribute(
                ConditionOpViewBeanBase.PG_SESSION_CONDITION_NAME);
            Condition condition = policy.getCondition(conditionName);
            Map map = condition.getProperties();
            for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ){
                String propName = (String)iter.next();
                Set val = (Set)map.get(propName);

                if (propName.equals(SimpleTimeCondition.START_DATE)) {
                    String strDate = (String)val.iterator().next();
                    helper.setDate(this, true, strDate, getModel());
                } else if (propName.equals(SimpleTimeCondition.END_DATE)) {
                    String strDate = (String)val.iterator().next();
                    helper.setDate(this, false, strDate, getModel());
                } else if (propName.equals(SimpleTimeCondition.START_TIME)){
                    String strTime = (String)val.iterator().next();
                    helper.setTime(this, true, strTime);
                } else if (propName.equals(SimpleTimeCondition.END_TIME)) {
                    String strTime = (String)val.iterator().next();
                    helper.setTime(this, false, strTime);
                } else if (propName.equals(SimpleTimeCondition.START_DAY)) {
                    String strDay = (String)val.iterator().next();
                    helper.setDay(this, true, strDay);
                } else if (propName.equals(SimpleTimeCondition.END_DAY)) {
                    String strDay = (String)val.iterator().next();
                    helper.setDay(this, false, strDay);
                } else if (propName.equals(
                    SimpleTimeCondition.ENFORCEMENT_TIME_ZONE)
                ) {
                    String strTz = (String)val.iterator().next();
                    helper.setTimeZone(this, canModify, strTz);
                }
            }
        } catch (NameNotFoundException e) {
            debug.warning("ConditionEditViewBean.getDefaultValues", e);
        } catch (AMConsoleException e) {
            debug.warning("ConditionEditViewBean.getDefaultValues", e);
        }

        //Yes, we are returning null;
        return values;
    }
}
