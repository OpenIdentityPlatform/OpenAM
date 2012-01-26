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
 * $Id: IDPPTest.java,v 1.2 2008/06/25 05:48:24 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws;

import com.sun.identity.liberty.ws.dst.DSTQuery;
import com.sun.identity.liberty.ws.dst.DSTQueryResponse;
import com.sun.identity.liberty.ws.dst.DSTModify;
import com.sun.identity.liberty.ws.dst.DSTModifyResponse;
import com.sun.identity.liberty.ws.dst.DSTException;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.test.UnitTestBase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class IDPPTest extends UnitTestBase {

    private static String PP_NAME_SPACE_URI = "urn:liberty:id-sis-pp:2003-08";

    public IDPPTest() {
        super("FedLibrary-PersonalProfile");
    }
    
    @Parameters({"idppquery-filename"})
    @Test(groups = {"idwsf"})
    public void parseIDPPQuery(String xmlFile)
        throws DSTException, FileNotFoundException
    {
        entering("parseIDPPQuery", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            DSTQuery query = new DSTQuery(element);
  
            assert (query.getNameSpaceURI().equals(PP_NAME_SPACE_URI)) 
                : "Wrong IDPP namespace URI";
            assert ((query.getResourceID() != null) &&
                (query.getResourceID().length() != 0)) 
                : "Null Resource ID";
            assert ((query.getQueryItems() != null) &&
                (!query.getQueryItems().isEmpty())) 
                : "Empty Query Item";
            log(Level.INFO, "parseIDPPQuery", query.toString(true,true));
        } finally {
            exiting("parseIDPPQuery");
        }
    }

    @Parameters({"idppqueryresponse-filename"})
    @Test(groups = {"idwsf"})
    public void parseIDPPQueryResponse(String xmlFile)
        throws DSTException, FileNotFoundException
    {
        entering("parseIDPPQueryResponse", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            DSTQueryResponse resp = new DSTQueryResponse(element);
  
            assert (resp.getNameSpaceURI().equals(PP_NAME_SPACE_URI)) 
                : "Wrong IDPP namespace URI";
            assert (resp.getStatus() != null)
                : "Missing Status Element";
            assert ((resp.getData() != null) && (!resp.getData().isEmpty()))
                : "Null or empty returned Data";
            log(Level.INFO, "parseIDPPQueryResponse", 
                resp.toString(true, true));
        } finally {
            exiting("parseIDPPQueryResponse");
        }
    }

    @Parameters({"idppmodify-filename"})
    @Test(groups = {"idwsf"})
    public void parseIDPPModify(String xmlFile)
        throws DSTException, FileNotFoundException
    {
        entering("parseIDPPModify", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            DSTModify mod = new DSTModify(element);
  
            assert (mod.getNameSpaceURI().equals(PP_NAME_SPACE_URI)) 
                : "Wrong IDPP namespace URI";
            assert (mod.getResourceID() != null) 
                : "Null Resource ID";
            assert ((mod.getModification() != null) &&
                (!mod.getModification().isEmpty()))
                : "Null or empty Modification Element";
            log(Level.INFO, "parseIDPPModify", mod.toString(true, true));
        } finally {
            exiting("parseIDPPModify");
        }
    }

    @Parameters({"idppmodifyresponse-filename"})
    @Test(groups = {"idwsf"})
    public void parseIDPPModifyResponse(String xmlFile)
        throws DSTException, FileNotFoundException
    {
        entering("parseIDPPModifyResponse", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            DSTModifyResponse resp = new DSTModifyResponse(element);
  
            assert (resp.getNameSpaceURI().equals(PP_NAME_SPACE_URI)) 
                : "Wrong IDPP namespace URI";
            assert (resp.getStatus() != null)
                : "Missing Status Element";
            log(Level.INFO, "parseIDPPModifyResponse", 
                resp.toString(true, true));
        } finally {
            exiting("parseIDPPModifyResponse");
        }
    }
}
