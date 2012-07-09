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
 * $Id: NumericAttributeCondition.java,v 1.3 2009/08/31 19:48:13 veiming Exp $
 */

package com.sun.identity.entitlement;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Condition for evaluating attribute value of numeric type.
 */
public class NumericAttributeCondition extends EntitlementConditionAdaptor {
    public static final String ATTR_NAME_ATTRIBUTE_NAME = "attributeName";
    public static final String ATTR_NAME_OPERATOR = "caseSensitive";
    public static final String ATTR_NAME_VALUE = "value";

    private String attributeName;
    private Operator operator = Operator.EQUAL;
    private float value;


    @Override
    public void init(Map<String, Set<String>> parameters) {
        for (String key : parameters.keySet()) {
            if (key.equalsIgnoreCase(ATTR_NAME_ATTRIBUTE_NAME)) {
                attributeName = getInitStringValue(parameters.get(key));
            } else if (key.equals(ATTR_NAME_OPERATOR)) {
                operator = getInitOperatorValue(parameters.get(key));
            } else if (key.equals(ATTR_NAME_VALUE)) {
                value = getInitFloatValue(parameters.get(key));
            }
        }
    }

    private static String getInitStringValue(Set<String> set) {
        return ((set == null) || set.isEmpty()) ? null : set.iterator().next();
    }

    private static Operator getInitOperatorValue(Set<String> set) {
        String value = ((set == null) || set.isEmpty()) ? null :
            set.iterator().next();
        if (value == null) {
            return Operator.EQUAL;
        }
        for (Operator o : Operator.values()) {
            if (o.toString().equals(value)) {
                return o;
            }
        }
        return Operator.EQUAL;
    }

    private static float getInitFloatValue(Set<String> set) {
        String value = ((set == null) || set.isEmpty()) ? null :
            set.iterator().next();
        if (value == null) {
            return 0f;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    public void setState(String state) {
          try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            if (jo.has("attributeName")) {
                attributeName = jo.optString("attributeName");
            }
            if (jo.has("operator")) {
                String strOp = jo.getString("operator");
                for (Operator o : Operator.values()) {
                    if (o.toString().equals(strOp)) {
                        operator = o;
                        break;
                    }
                }
                if (operator == null) {
                    operator = Operator.EQUAL;
                }
            }

            String strValue = jo.getString("value");
            try {
                value = Float.parseFloat(strValue);
            } catch (NumberFormatException e) {
                PrivilegeManager.debug.error(
                    "NumericAttributeCondition.setState",
                    e);
            }
        } catch (JSONException e) {
            PrivilegeManager.debug.error("NumericAttributeCondition.setState",
                e);
        }
    }

    public String getState() {
        try {
            JSONObject jo = new JSONObject();
            toJSONObject(jo);
            if (attributeName != null) {
                jo.put("attributeName", attributeName);
            }
            if (operator != null) {
                jo.put("operator", operator);
            }
            jo.put("value", Float.toString(value));
            return jo.toString();
        } catch (JSONException e) {
            PrivilegeManager.debug.error("NumericAttributeCondition.getState",
                e);
            return "";
        }
    }

    public ConditionDecision evaluate(String realm, Subject subject,
        String resourceName,
        Map<String, Set<String>> environment) throws EntitlementException {
        boolean allowed = false;
        if ((attributeName != null) && (attributeName.length() > 0)) {
            Set<String> values = environment.get(attributeName);
            if ((values != null) && !values.isEmpty()) {
                for (String v : values) {
                    allowed = match(v);
                    if (allowed) {
                        break;
                    }
                }
            }
        } else {
            PrivilegeManager.debug.error(
                "NumericAttributeCondition cannot be evaluated because attribute name is null",
                null);
        }

        if (!allowed && (attributeName != null) && (attributeName.length() > 0)
        ) {
            Map<String, Set<String>> advices =
                new HashMap<String, Set<String>>();
            Set<String> set = new HashSet<String>();
            set.add(attributeName + operator.symbol + value);
            advices.put(getClass().getName(), set);
            return new ConditionDecision(false, advices);
        }
        return new ConditionDecision(allowed, Collections.EMPTY_MAP);
    }

    private boolean match(String str) {
        boolean match = false;
        try {
            Float v = Float.parseFloat(str);
            switch (operator) {
                case LESS_THAN:
                    match = (v < value);
                    break;
                case LESS_THAN_OR_EQUAL:
                    match = (v <= value);
                    break;
                case EQUAL:
                    match = (v == value);
                    break;
                case GREATER_THAN_OR_EQUAL:
                    match = (v >= value);
                    break;
                case GREATER_THAN:
                    match = (v > value);
                    break;
            }
        } catch (NumberFormatException e) {
            PrivilegeManager.debug.warning(
                "NumericAttributeCondition.match",
                e);
        }
        return match;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public Operator getOperator() {
        return operator;
    }

    public float getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        NumericAttributeCondition other = (NumericAttributeCondition)obj;
        if (!compareString(this.attributeName, other.attributeName)) {
            return false;
        }
        if (this.operator != other.operator) {
            return false;
        }
        return (this.value == other.value);
    }

    @Override
    public int hashCode() {
        int hc = super.hashCode();
        if (attributeName != null) {
            hc += attributeName.hashCode();
        }
        hc += operator.hashCode();
        hc += value;
        return hc;
    }

    public enum Operator {
        LESS_THAN("<"),
        LESS_THAN_OR_EQUAL("<="),
        EQUAL("="),
        GREATER_THAN_OR_EQUAL(">="),
        GREATER_THAN(">");

        private final String symbol;
        Operator(String symbol) {
            this.symbol = symbol;
        }
    }
}
