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
 * $Id: LoanProcessor.java,v 1.1 2008/07/12 18:33:41 mallas Exp $
 *
 */

package com.sun.samples.loanprocessor;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.ejb.Stateless;

/**
 *
 * @author Administrator
 */
@WebService()
@Stateless()
public class LoanProcessor {
    
    
   int MINIMUM_AGE_LIMIT = 18;
   int MAXIMUM_AGE_LIMIT = 65;
   double MINIMUM_SALARY = 20000;
   int AVERAGE_LIFE_EXPECTANCY = 70;
  
/**
     * Web service operation
     */
    @WebMethod(operationName = "processApplication")
    public String processApplication(@WebParam(name = "socialSecurityNumber")
    String socialSecurityNumber, @WebParam(name = "applicantName")
    String applicantName, @WebParam(name = "applicantAddress")
    String applicantAddress, @WebParam(name = "applicantEmailAddress")
    String applicantEmailAddress, @WebParam(name = "applicantAge")
    int applicantAge, @WebParam(name = "applicantGender")
    String applicantGender, @WebParam(name = "annualSalary")
    double annualSalary, @WebParam(name = "amountRequested")
    double amountRequested) {
       String result = "Loan Application APPROVED.";
        // Check age of applicant
        // If less than min age limit, rejected
        if(applicantAge < MINIMUM_AGE_LIMIT) {
            result = "Loan Application REJECTED - Reason: Under-aged " +
                    applicantAge +
                    ". Age needs to be over " +
                    MINIMUM_AGE_LIMIT +
                    " years to qualify.";
            System.out.println(result);
            return result;
        }

        // Check age of applicant
        // If more than max age limit, rejected
        if(applicantAge > MAXIMUM_AGE_LIMIT) {
            result = "Loan Application REJECTED - Reason: Over-aged " +
                    applicantAge +
                    ". Age needs to be under " +
                    MAXIMUM_AGE_LIMIT +
                    " years to qualify.";
            System.out.println(result);
            return result;
        }

        // Check annual salary
        // If less than min salary, rejected
        if(annualSalary < MINIMUM_SALARY) {
            result = "Loan Application REJECTED - Reason: Annual Salary $" +
                    annualSalary +
                    " too low. Annual Salary needs to be over $" +
                    MINIMUM_SALARY +
                    " to qualify.";
            System.out.println(result);
            return result;
        }

        // Calculate the years to pay off loan based on applicantAge
        int yearsToRepay = AVERAGE_LIFE_EXPECTANCY - applicantAge;

        // Calculate the max amount of loan based on years to pay off loan
        double limit = annualSalary * yearsToRepay * 0.5;

        // Check amount requested, if higher than limit, rejected
        if(amountRequested > limit) {
            result = "Loan Application REJECTED - Reason: You are asking for too much $" +
                    amountRequested +
                    ". Annual Salary $" +
                    annualSalary +
                    ", Age " +
                    applicantAge +
                    " years. Your limit is $" +
                    limit;
            System.out.println(result);
            return result;
        }
        System.out.println(result);
        return result;
    }

}
