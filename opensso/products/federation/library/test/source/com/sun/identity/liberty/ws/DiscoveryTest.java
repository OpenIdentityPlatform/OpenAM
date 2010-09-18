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
 * $Id: DiscoveryTest.java,v 1.2 2008/06/25 05:48:24 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws;

import com.sun.identity.liberty.ws.disco.Query;
import com.sun.identity.liberty.ws.disco.QueryResponse;
import com.sun.identity.liberty.ws.disco.Modify;
import com.sun.identity.liberty.ws.disco.ModifyResponse;
import com.sun.identity.liberty.ws.disco.DiscoveryException;
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

public class DiscoveryTest extends UnitTestBase {
    public DiscoveryTest() {
        super("FedLibrary-DiscoveryService");
    }
    
    @Parameters({"dsquery-filename"})
    @Test(groups = {"idwsf"})
    public void parseDSQuery(String xmlFile)
        throws DiscoveryException, FileNotFoundException
    {
        entering("parseDSQuery", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            Query query = new Query(element);
  
            assert (query.getResourceID() != null) 
                : "Null Resource ID";
            log(Level.INFO, "parseDSQuery", query.toString());
        } finally {
            exiting("parseDSQuery");
        }
    }

    @Parameters({"dsqueryresponse-filename"})
    @Test(groups = {"idwsf"})
    public void parseDSQueryResponse(String xmlFile)
        throws DiscoveryException, FileNotFoundException
    {
        entering("parseDSQueryResponse", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            QueryResponse resp = new QueryResponse(element);
  
            assert (resp.getStatus() != null)
                : "Missing Status Element";
            assert ((resp.getResourceOffering() != null) &&
                (!resp.getResourceOffering().isEmpty()))
                : "Null or empty resource ID";
            log(Level.INFO, "parseDSQueryResponse", resp.toString());
        } finally {
            exiting("parseDSQueryResponse");
        }
    }

    @Parameters({"dsmodify-filename"})
    @Test(groups = {"idwsf"})
    public void parseDSModify(String xmlFile)
        throws DiscoveryException, FileNotFoundException
    {
        entering("parseDSModify", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            Modify mod = new Modify(element);
  
            assert (mod.getResourceID() != null) 
                : "Null Resource ID";
            assert ((mod.getInsertEntry() != null) &&
                (!mod.getInsertEntry().isEmpty()))
                : "Null or empty InsertEntry";
            log(Level.INFO, "parseDSModify", mod.toString());
        } finally {
            exiting("parseDSModify");
        }
    }

    @Parameters({"dsmodifyresponse-filename"})
    @Test(groups = {"idwsf"})
    public void parseDSModifyResponse(String xmlFile)
        throws DiscoveryException, FileNotFoundException
    {
        entering("parseDSModifyResponse", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            ModifyResponse resp = new ModifyResponse(element);
  
            assert (resp.getStatus() != null)
                : "Missing Status Element";
            log(Level.INFO, "parseDSModifyResponse", resp.toString());
        } finally {
            exiting("parseDSModifyResponse");
        }
    }
}
