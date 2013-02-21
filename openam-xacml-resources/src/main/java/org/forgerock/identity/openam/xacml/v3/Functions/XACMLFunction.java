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
import org.forgerock.identity.openam.xacml.v3.Entitlements.FunctionArgument;
import org.forgerock.identity.openam.xacml.v3.Entitlements.XACMLPIPObject;

import java.util.ArrayList;
import java.util.List;


/*
    This interface defines the XACML functions.
    The Syntax,  is to create a function object with a
    ResourceID, and a Value, which will be checked
    when the function is evaluated.
 */
public abstract class XACMLFunction extends FunctionArgument {
    private List<FunctionArgument> arguments;

    public XACMLFunction() {
        arguments = new ArrayList<FunctionArgument>();
    }

    public void addArgument(FunctionArgument arg) {
        arguments.add(arg);
    };
    public void addArgument(List<FunctionArgument> args) {
        arguments.addAll(args);
    };
    public Object getValue(XACMLPIPObject pip) {
        return evaluate(pip).getValue(pip);
    };
    abstract public FunctionArgument evaluate( XACMLPIPObject pip);

    /* Protected methods only for subclasses */

    protected FunctionArgument getArg(int index) {
        return arguments.get(index);
    }
    protected int getArgCount() {
        return arguments.size();
    }
}
