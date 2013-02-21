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
    This class Encapsulates a DataDesignator from the XACML policy.
    In this case, we have To Fetch the data from PIP

 */


public class DataDesignator extends FunctionArgument {
    private String category;
    private String attributeID;
    private boolean presence;

    public DataDesignator(String type, String category, String attributeID) {
        setType(type);
        this.category = category;
        this.attributeID = attributeID;
    };

    public Object getValue(XACMLPIPObject pip) {
        return pip.resolve(category,attributeID).getValue(pip);
    }
}
