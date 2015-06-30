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
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.console.sts.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

/**
 * This class is a bit of a 'tricky move': the STSHomeViewBean needs a single model, but needs to represent state from
 * two services - the rest and soap sts. So the STSInstanceModel interface provides functionality to satisfy the STSHomeViewBean,
 * and the rest and soap create and add view beans. Ultimately, both the RestSTSInstanceModel and the SoapSTSInstanceModel will get the bulk
 * of their functionality from the STSInstanceModelBase, which extends the AMServiceProfileModelImpl class, thereby providing the
 * functionality to harvest property-sheet state, and turn it into the {@code Map<String, Set<String>>} format which can
 * be POSTed or PUT to sts-publish/rest or sts-publish/soap to create/update rest/soap instances. However, the page in
 * the STS pane which displays the table of both rest and soap sts instances, which is handled by the STSHomeViewBean,
 * also needs a AMModel implementation, one which can satisfy referencing two services. This class satisfies this need,
 * but delegates the work to either the restSTSInstanceModel or the soapSTSInstanceModel.
 *
 * Note also that the Rest- and Soap- specific edit and add ViewBean classes do not return an instance of this class
 * from getModelInternal, but rather an instance of the RestSTSInstanceModel or the SoapSTSInstanceModel, as these extend the
 * AMServiceProfileModelImpl class, which is necessary to represent a service instance.
 *
 */
public class STSHomeViewBeanModelImpl extends AMModelBase implements STSHomeViewBeanModel {
    private final STSInstanceModel restSTSInstanceModel;
    private final STSInstanceModel soapSTSInstanceModel;

    public STSHomeViewBeanModelImpl(HttpServletRequest req, Map map) throws AMConsoleException {
        super(req, map);
        restSTSInstanceModel = new RestSTSInstanceModel(req, map);
        soapSTSInstanceModel = new SoapSTSInstanceModel(req, map);
    }

    @Override
    public Set<String> getPublishedInstances(STSType stsType, String realm) throws AMConsoleException {
        if (stsType.isRestSTS()) {
            return restSTSInstanceModel.getPublishedInstances(stsType, realm);
        } else {
            return soapSTSInstanceModel.getPublishedInstances(stsType, realm);
        }
    }

    @Override
    public void deleteInstances(STSType stsType, Set<String> instanceNames) throws AMConsoleException {
        if (stsType.isRestSTS()) {
            restSTSInstanceModel.deleteInstances(stsType, instanceNames);
        } else {
            soapSTSInstanceModel.deleteInstances(stsType, instanceNames);
        }
    }
}
