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
 * $Id: WSSUserNameTokenContentHandler.java,v 1.2 2008/06/25 05:51:49 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class WSSUserNameTokenContentHandler extends DefaultHandler {
    
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        if (uri != null) {
            if (uri.equals(NAMESPACE_SOAPENV)) {
                if (localName.equals(ELEMENT_SOAPENV_ENVELOPE)) {
                    enterSoapEnvelope();
                } else if (localName.equals(ELEMENT_SOAPENV_HEADER)) {
                    enterSoapEnvelopeHeader();
                }
            } else if (uri.equals(NAMESPACE_WSSE)) {
                if (localName.equals(ELEMENT_WSSE_SECURITY)) {
                    enterWsseSecurity();
                } else if (localName.equals(ELEMENT_WSSE_USERNAME_TOKEN)) {
                    enterWsseUsernameToken();
                } else if (localName.equals(ELEMENT_WSSE_USERNAME)) {
                    enterWsseUsername();
                } else if (localName.equals(ELEMENT_WSSE_PASSWORD)) {
                    enterWssePassword();
                }
            }
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (uri != null) {
            if (uri.equals(NAMESPACE_SOAPENV)) {
                if (localName.equals(ELEMENT_SOAPENV_ENVELOPE)) {
                    exitSoapEnvelope();
                } else if (localName.equals(ELEMENT_SOAPENV_HEADER)) {
                    exitSoapEnvelopeHeader();
                }
            } else if (uri.equals(NAMESPACE_WSSE)) {
                if (localName.equals(ELEMENT_WSSE_SECURITY)) {
                    exitWsseSecurity();
                } else if (localName.equals(ELEMENT_WSSE_USERNAME_TOKEN)) {
                    exitWsseUsernameToken();
                } else if (localName.equals(ELEMENT_WSSE_USERNAME)) {
                    exitWsseUsername();
                } else if (localName.equals(ELEMENT_WSSE_PASSWORD)) {
                    exitWssePassword();
                }
            }
        }
    }

    public void endDocument() throws SAXException {
        if (getUsername() == null) {
            throw new SAXException("Cannot locate Username");
        }
        if (getPassword() == null) {
            throw new SAXException("Cannot locate Password");
        }
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (inSoapEnvelope() && inSoapEnvelopeHeader() && inWsseUsernameToken()) {
            if (inWsseUsername()) {
                setUsername(new String(ch, start, length));
            } else if (inWssePassword()) {
                setPassword(new String(ch, start, length));
            }
        }
    }

    public String getUsername() {
        return _username;
    }

    public String getPassword() {
        return _password;
    }

    private void setUsername(String username) {
        _username = username;
    }

    private void setPassword(String password) {
        _password = password;
    }

    private void exitWssePassword() throws SAXException {
        failIf(!inWssePassword(), "Not in WSSE Password");
        _inWssePassword = false;
    }

    private void enterWssePassword() throws SAXException {
        failIf(inWssePassword(), "Already in WSSE Password");
        _inWssePassword = true;
    }

    private boolean inWssePassword() {
        return _inWssePassword;
    }

    private void exitWsseUsername() throws SAXException {
        failIf(!inWsseUsername(), "Not in WSSE Username");
        _inWsseUsername = false;
    }

    private void enterWsseUsername() throws SAXException {
        failIf(inWsseUsername(), "Already in WSSE Username");
        _inWsseUsername = true;
    }

    private boolean inWsseUsername() {
        return _inWsseUsername;
    }

    private void exitWsseUsernameToken() throws SAXException {
        failIf(!inWsseUsernameToken(), "Not in WSSE UsernameToken");
        _inWsseUsernameToken = false;
    }

    private void enterWsseUsernameToken() throws SAXException {
        failIf(inWsseUsernameToken(), "Already in WSSE UsernameToken");
        _inWsseUsernameToken = true;
    }

    private boolean inWsseUsernameToken() {
        return _inWsseUsernameToken;
    }

    private void exitWsseSecurity() throws SAXException {
        failIf(!inWsseSecurity(), "Not in WSSE Security");
        _inWsseSecurity = false;
    }

    private void enterWsseSecurity() throws SAXException {
        failIf(inWsseSecurity(), "Already in WSSE Security");
        _inWsseSecurity = true;
    }

    private boolean inWsseSecurity() {
        return _inWsseSecurity;
    }

    private void exitSoapEnvelopeHeader() throws SAXException {
        failIf(!inSoapEnvelopeHeader(), "Not in SOAP Envelope Header");
        _inSoapEnvelopeHeader = false;
    }

    private void enterSoapEnvelopeHeader() throws SAXException {
        failIf(inSoapEnvelopeHeader(), "Already in SOAP Envelope Header");
        failIf(!inSoapEnvelope(), "Not in SOAP Envelope");
        _inSoapEnvelopeHeader = true;
    }

    private boolean inSoapEnvelopeHeader() {
        return _inSoapEnvelopeHeader;
    }

    private void exitSoapEnvelope() throws SAXException {
        failIf(!inSoapEnvelope(), "Not in SOAP Envelope");
        _inSoapEnvelope = false;
    }

    private void enterSoapEnvelope() throws SAXException {
        failIf(inSoapEnvelope(), "Already in SOAP Envelope");
        _inSoapEnvelope = true;
    }

    private boolean inSoapEnvelope() {
        return _inSoapEnvelope;
    }

    private void failIf(boolean condition, String message) throws SAXException {
        if (condition) {
            throw new SAXException(message);
        }
    }

    private String _username = null;

    private String _password = null;

    private boolean _inSoapEnvelope = false;

    private boolean _inSoapEnvelopeHeader = false;

    private boolean _inWsseSecurity = false;

    private boolean _inWsseUsernameToken = false;

    private boolean _inWsseUsername = false;

    private boolean _inWssePassword = false;

    private static final String NAMESPACE_SOAPENV = 
        "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String NAMESPACE_WSSE = 
        "http://docs.oasis-open.org/wss/2004/01/"
            + "oasis-200401-wss-wssecurity-secext-1.0.xsd";

    private static final String ELEMENT_SOAPENV_ENVELOPE = "Envelope";

    private static final String ELEMENT_SOAPENV_HEADER = "Header";

    private static final String ELEMENT_WSSE_SECURITY = "Security";

    private static final String ELEMENT_WSSE_USERNAME_TOKEN = "UsernameToken";

    private static final String ELEMENT_WSSE_USERNAME = "Username";

    private static final String ELEMENT_WSSE_PASSWORD = "Password";
}
