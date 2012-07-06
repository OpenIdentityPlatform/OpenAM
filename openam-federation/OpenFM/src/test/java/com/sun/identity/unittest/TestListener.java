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
 * $Id: TestListener.java,v 1.1 2009/08/19 05:41:03 veiming Exp $
 */

package com.sun.identity.unittest;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;


public class TestListener implements ITestListener {
   
    public void onTestStart(ITestResult result) {
        UnittestLog.logMessage("Started " + result.getTestClass().getName() + 
            "." + result.getMethod().getMethodName());
    }
    
    public void onTestSuccess(ITestResult result) {
        long timeTaken = result.getEndMillis() - result.getStartMillis();
        UnittestLog.logMessage("Completed " + 
            result.getTestClass().getName() + "." +
            result.getMethod().getMethodName() + " (" + timeTaken + 
            " millisec)");
    }
   
    public void onTestFailure(ITestResult result) {
        UnittestLog.logError("Failed " +
            result.getTestClass().getName() + "." +
            result.getMethod().getMethodName());
        Throwable t = result.getThrowable();
        if (t != null) {
            UnittestLog.logError("Exception STACKTRACE, message:" + t.getMessage());
            UnittestLog.logError("Exception Class:" + t.getClass().getName());
            StackTraceElement[] elems = t.getStackTrace();
            for (StackTraceElement elem : elems) {
                UnittestLog.logError(elem.toString());
            }
            UnittestLog.logMessage("END STACKTRACE");
        } else {
            UnittestLog.logMessage("No Exception Stacktrace:");
        }
    }
    
    public void onTestSkipped(ITestResult result) {
        UnittestLog.logError("Skipped, " +
            result.getTestClass().getName() + "." +
            result.getMethod().getMethodName());
        Throwable t = result.getThrowable();
        if (t != null) {
            UnittestLog.logError("Exception STACKTRACE, message:" + t.getMessage());
            UnittestLog.logError("Exception Class:" + t.getClass().getName());
            StackTraceElement[] elems = t.getStackTrace();
            for (StackTraceElement elem : elems) {
                UnittestLog.logMessage(elem.toString());
            }
            UnittestLog.logMessage("END STACTRACE");
        } else {
            UnittestLog.logMessage("No Exception Stacktrace:");
        }
    }
    
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        UnittestLog.logError("Failed within success percentage," +
            result.getTestClass().getName() + "." +
            result.getMethod().getMethodName());
        Throwable t = result.getThrowable();
        if (t != null) {
            UnittestLog.logError("Exception STACKTRACE, message:" + t.getMessage());
            UnittestLog.logError("Exception Class:" + t.getClass().getName());
            StackTraceElement[] elems = t.getStackTrace();
            for (StackTraceElement elem : elems) {
                UnittestLog.logMessage(elem.toString());
            }
            UnittestLog.logMessage("END STACKTRACE");
        } else {
                UnittestLog.logMessage("No Exception Stacktrace:");
        }
    }
    
    public void onStart(ITestContext context) {
    }
    
    public void onFinish(ITestContext context) {
    }
}

