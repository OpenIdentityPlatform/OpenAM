/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.identity.openam.xacml.v3.Entitlements;



/*
    This class is the parent class for Function Arguments.
    It has three subclasses.
        DataValue for known values
        DataDesignator for values to fetch at runtime
        XACMLFunction as a parent class for all functions

 */
public abstract class FunctionArgument {
    public static FunctionArgument trueObject = new DataValue(DataType.XACMLBOOLEAN,"true");
    public static FunctionArgument falseObject = new DataValue(DataType.XACMLBOOLEAN,"false");
    public static FunctionArgument indeterminateObject = new DataValue(DataType.XACMLBOOLEAN,"indeterminate");
    private String dataType;

    public void setType(String type) {
        this.dataType = type;
    }
    public String getType() {
        return dataType;
    }
    public abstract Object getValue(XACMLPIPObject pip);
}
