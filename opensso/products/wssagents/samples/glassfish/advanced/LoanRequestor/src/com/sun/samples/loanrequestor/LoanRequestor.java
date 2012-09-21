/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LoanRequestor.java,v 1.1 2008/07/12 18:33:41 mallas Exp $
 *
 */

package com.sun.samples.loanrequestor;

import com.sun.samples.loanprocessor.LoanProcessorService;
import javax.jws.WebService;
import javax.xml.ws.WebServiceRef;
import com.sun.samples.loanrequestor.LoanRequestorPortType;
import com.sun.samples.loanrequestor.ProcessApplicationResponseType;


@WebService(serviceName = "LoanRequestorService", portName = "LoanRequestorPort", endpointInterface = "com.sun.samples.loanrequestor.LoanRequestorPortType", targetNamespace = "http://j2ee.netbeans.org/wsdl/LoanRequestor", wsdlLocation = "WEB-INF/wsdl/LoanRequestor/LoanRequestor.wsdl")
public class LoanRequestor implements LoanRequestorPortType {
    @WebServiceRef(wsdlLocation = "http://localhost:8080/LoanProcessorService/LoanProcessor?wsdl")
    private LoanProcessorService service;

    public com.sun.samples.loanrequestor.ProcessApplicationResponseType loanRequestorOperation(com.sun.samples.loanrequestor.ProcessApplicationType requestLoanMessage) {
        //TODO implement this method
        
        try { // Call Web Service Operation
            com.sun.samples.loanprocessor.LoanProcessor port = service.getLoanProcessorPort();
            // TODO initialize WS operation arguments here
            java.lang.String socialSecurityNumber = requestLoanMessage.getSocialSecurityNumber();
            java.lang.String applicantName = requestLoanMessage.getApplicantName();
            java.lang.String applicantAddress = requestLoanMessage.getApplicantAddress();
            java.lang.String applicantEmailAddress = requestLoanMessage.getApplicantEmailAddress();
            int applicantAge = requestLoanMessage.getApplicantAge();
            java.lang.String applicantGender = requestLoanMessage.getApplicantGender();
            double annualSalary = requestLoanMessage.getAnnualSalary();
            double amountRequested = requestLoanMessage.getAmountRequested();
            // TODO process result here
            java.lang.String result = port.processApplication(socialSecurityNumber, applicantName, applicantAddress, applicantEmailAddress, applicantAge, applicantGender, annualSalary, amountRequested);
            System.out.println("Loan Processor Result = "+result);
            ProcessApplicationResponseType responseType =
                    new ProcessApplicationResponseType();
            responseType.setReturn(result);
            return responseType;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        
    }

}
