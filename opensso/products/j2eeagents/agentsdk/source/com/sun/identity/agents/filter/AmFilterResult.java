/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AmFilterResult.java,v 1.3 2008/10/07 17:32:31 huacui Exp $
 *
 */

package com.sun.identity.agents.filter;

import com.sun.identity.agents.common.IHttpServletRequestHelper;
import com.sun.identity.agents.common.SSOValidationResult;

/**
 * The result of the processing of an agent task handler
 */
public class AmFilterResult {
    public AmFilterResult(AmFilterResultStatus status, String redirectURL,
            String data) {

        setStatus(status);
        setRedirectURL(redirectURL);
        setData(data);
        setNotEnforcedFlag(false);
        setProcessResponseFlag(false);
    }

    public AmFilterResult(AmFilterResultStatus status, String redirectURL) {
        this(status, redirectURL, null);
        }

        public AmFilterResult(AmFilterResultStatus status) {
            this(status, null, null);
        }

        public AmFilterResultStatus getStatus() {
                   return _status;
        }
        
        public void markBlocked() {
            _blocked = true;
        }
        
        public boolean isBlocked() {
            return _blocked;
        }
        
        public String getRedirectURL() {
            return _redirectURL;
        }

        public String getRequestURL() {
            return _requestURL;
        }

        public IHttpServletRequestHelper getRequestHelper() {
            return _requestHelper;
        }
        
        public SSOValidationResult getSSOValidationResult() {
                return _ssoValidationResult;
        }

        public boolean getProcessResponseFlag() {
            return _processResponse;
        } 

        public  void markAsNotEnforced() {
            setNotEnforcedFlag(true);
        }

        protected boolean isNotEnforced() {
            return _notEnforced;
        }

        public String toString() {

            StringBuffer buff = new StringBuffer();
                 buff.append(NEW_LINE);
                 buff.append(SEPARATOR);
                 buff.append(NEW_LINE);
                 buff.append("FilterResult:");
                 buff.append(NEW_LINE);
                 buff.append("\tStatus    \t: ");
                 buff.append(getStatus().toString());
                 buff.append(NEW_LINE);
                 buff.append("\tProcessResponse    \t: ");
                 buff.append(getProcessResponseFlag());
                 buff.append(NEW_LINE);
                 buff.append("\tRedirectURL\t: ");
                 buff.append(getRedirectURL());
                 buff.append(NEW_LINE);
                 buff.append("\tRequestURL\t: ");
                 buff.append(getRequestURL());
                 buff.append(NEW_LINE);
                 buff.append("\tRequestHelper: ");
                 buff.append(NEW_LINE);
                 buff.append("\t\t" + getRequestHelper());
                 buff.append(NEW_LINE);
                 buff.append(NEW_LINE);
                 buff.append("\tData: ");
                 buff.append(NEW_LINE);
                 buff.append("\t\t");
                 buff.append(getDataToServe());
                 buff.append(NEW_LINE);
                 buff.append(NEW_LINE);
                 buff.append(SEPARATOR);
                 buff.append(NEW_LINE);

                 return buff.toString();
        }

        public boolean isAllowed() {
                    return(getStatus().getIntValue()
                            == AmFilterResultStatus.INT_STATUS_CONTINUE);
        }

        public void setHttpServletRequestHelper(
                IHttpServletRequestHelper helper) {
            _requestHelper = helper;
        }
        
        public String getDataToServe() {
            return _data;
        }
        
        void setSSOValidationResult(SSOValidationResult ssoValidationResult) {
                _ssoValidationResult = ssoValidationResult;
        }
        
        private void setNotEnforcedFlag(boolean flag) {
            _notEnforced = flag;
        }

        private void setStatus(AmFilterResultStatus status) {
            _status = status;
        }

        private void setRedirectURL(String redirectURL) {
            _redirectURL = redirectURL;
        }

        void setRequestURL(String reqURL) {
            _requestURL = reqURL;
        }

        private void setData(String data) {
            _data = data;
        }

        public void setProcessResponseFlag(boolean processResponse) {
            _processResponse = processResponse;
        }

        private boolean                   _notEnforced;
        private AmFilterResultStatus      _status;
        private String                    _redirectURL;
        private String                    _requestURL;
        private String                    _data;
        private IHttpServletRequestHelper _requestHelper;
        private boolean                   _blocked;
        private SSOValidationResult       _ssoValidationResult;
        private boolean                   _processResponse;        

        public static final String NEW_LINE = 
                            System.getProperty("line.separator", "\n");
        
        public static final String SEPARATOR =
        "-----------------------------------------------------------";
}
