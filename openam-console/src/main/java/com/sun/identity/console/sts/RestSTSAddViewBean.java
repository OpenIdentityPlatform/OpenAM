/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

/*
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */
package com.sun.identity.console.sts;

import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMPropertySheetModel;

import static com.sun.identity.console.sts.model.STSInstanceModel.STSType;


/**
 * The ViewBean used to create new Rest STS instances. Extends the AMServiceProfileViewBeanBase class as this class
 * provides for automatic constitution of propertySheet values based on model state.
 */
public class RestSTSAddViewBean extends STSAddViewBeanBase {
    public static final String DEFAULT_DISPLAY_URL = "/console/sts/RestSTSAdd.jsp";
    private static final String PROPERTY_MODEL_XML_FILE_LOCATION = "com/sun/identity/console/propertyRestSecurityTokenService.xml";

    public RestSTSAddViewBean() {
        super("RestSTSAdd", DEFAULT_DISPLAY_URL, AMAdminConstants.REST_STS_SERVICE, STSType.REST);
    }

    /*
    This method is called from the AMServiceProfileViewBeanBase ctor, so the xml file location cannot be passed to the
    super ctor, as this field is not initialized until after the super ctor is called. So this method must be over-ridden
    here.
    */
    @Override
    protected void createPropertyModel() {
        String xml = AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(PROPERTY_MODEL_XML_FILE_LOCATION));

        propertySheetModel = new AMPropertySheetModel(xml);
        propertySheetModel.clear();
    }
}
