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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.soap;

import com.google.inject.Inject;
import org.apache.cxf.sts.operation.TokenIssueOperation;
import org.apache.cxf.sts.operation.TokenRenewOperation;
import org.apache.cxf.sts.operation.TokenValidateOperation;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import org.apache.cxf.ws.security.sts.provider.operation.IssueOperation;
import org.apache.cxf.ws.security.sts.provider.operation.RenewOperation;
import org.apache.cxf.ws.security.sts.provider.operation.ValidateOperation;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenValidationException;

/**
 * An instance of this class is created for each STS instance. An instance of this class is set as the bean published via
 * the JaxWsServerFactoryBean. All of its dependencies are injected via guice.
 *
 */
public class STSEndpoint extends SecurityTokenServiceProvider {
    /*
    The Provider class has a single no-arg ctor, that throws Exception. Because this ctor will be called
    as part of calling the ctor below, we need to propagate the exception.
     */
    @Inject
    public STSEndpoint(IssueOperation issueOperation,
                       ValidateOperation validateOperation,
                       RenewOperation renewOperation) throws Exception {
        /*
        I'm never setting the issueSingle operation. The IssueOperation above also implements the IssueSingle interface -
        perhaps I should inject both IssueOperation and IssueSingleOperation instances. Probably necessary for full
        spec compliance.
        TODO:
         */
        setIssueOperation(issueOperation);
        setValidateOperation(validateOperation);
        setRenewOperation(renewOperation);
    }
}
