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
package org.forgerock.identity.openam.xacml.v3.Functions;

/*
urn:oasis:names:tc:xacml:1.0:function:double-equal
This function SHALL take two arguments of data-type
“http://www.w3.org/2001/XMLSchema#double” and SHALL return
an “http://www.w3.org/2001/XMLSchema#boolean”.
It SHALL perform its evaluation on doubles according to IEEE 754 [IEEE754].
*/

import org.forgerock.identity.openam.xacml.v3.Entitlements.FunctionArgument;
import org.forgerock.identity.openam.xacml.v3.Entitlements.XACMLPIPObject;

public class DoubleEqual extends XACMLFunction {

    public DoubleEqual()  {
    }
    public FunctionArgument evaluate( XACMLPIPObject pip){
        FunctionArgument retVal =  FunctionArgument.falseObject;

        if ( getArgCount() != 2) {
            return retVal;
        }
        String s = (String)getArg(0).getValue(pip);
        Double arg0 = Double.parseDouble(s);
        String s1 = (String)getArg(1).getValue(pip);
        Double arg1 = Double.parseDouble(s1);

        if (arg0.equals(arg1)) {
            retVal = FunctionArgument.trueObject;
        } else {
            retVal = FunctionArgument.falseObject;
        }
        return retVal;
    }

}
