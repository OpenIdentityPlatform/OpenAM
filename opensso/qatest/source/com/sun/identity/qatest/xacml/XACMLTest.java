/* The contents of this file are subject to the terms
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
 * $Id: XACMLTest.java,v 1.5 2009/08/18 19:11:07 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.xacml;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.PolicyCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.ResourceBundle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;


/**
 * This class does the following
 * (1) Create PEP and PDP entities
 * (2) Create PEP and PDP COT
 * (3) Load PEP and PDP meta
 * (4) Generate dynamic propertied for the cases permit,deny,indeterminate
 * (5) Does the XACML tests for Permit,Deny and Indeterminate cases
 */
public class XACMLTest extends TestCommon {
    
    private WebClient webClient;
    private String amadmURL;
    private String loginURL;
    private String logoutURL;
    private String baseDir;
    private String testAction;
    private String allowedUser;
    private String denyUser;
    private String acessResource;
    private String allowPolicyName;
    private String denyPolicyName;
    private String servicename = "sunfmSAML2SOAPBindingService";
    private String schematype = "Global";
    private List<String> attributevalues = new ArrayList<String>();
    private List list;
    private FederationManager spfm;
    
    //Response Validation
    private String expectedPermit = "<xacml-context:Decision>Permit" +
            "</xacml-context:Decision>";
    private String expectedDeny = "<xacml-context:Decision>Deny" +
            "</xacml-context:Decision>";
    private String expectedInderminate = "<xacml-context:Decision>Indeterminate"
            + "</xacml-context:Decision>";
    
    
    //XACML Configuration related
    private ResourceBundle testResources;
    private String pdpentityname;
    private String pepentityname;
    private String certAlias;
    private String pdpCot;
    private String pepCot;
    private String pdpmetaAlias;
    private String pepmetaAlias;
    
    //XACML Policy related
    private int polIdx;
    private ResourceBundle rbg;
    private ResourceBundle rbp;
    private String strLocRB = "xacmlPolicyTest";
    private String strGblRB = "xacmlPolicyGlobal";
    private PolicyCommon mpc;
    
    /**
     * Creates a new instance of XACMLTest
     * Setups the environment to test XACML and
     * runs the xacml tests for POST and GET
     * for various Decisions allowed,deny,Indeterminate
     */
    public XACMLTest() {
        super("XACMLTest");
        loginURL = getLoginURL(realm);
        logoutURL = protocol + ":" + "//" + host + ":" + port +
                uri + "/UI/Logout";
        amadmURL = protocol + ":" + "//" + host + ":" + port + uri;
        spfm = new FederationManager(amadmURL);
    }
    
    /**
     * This method creates the hosted PDP and PEP metadata template & loads it.
     * This method creates the PDP, PEP entities and circle-of-trusts for
     * PEP and PDP, add each other into the COT.
     * @param actionmethos GET or POST can be action methods for xacml
     */
    @Parameters({"actionmethod"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String actionmethod)
    throws Exception {
        Object[] params = {actionmethod};
        entering("setup", params);
        String[] arrMetadata= {"", ""};
        String[] arrpMetadata= {"", ""};
        try {
            testAction = actionmethod;
            baseDir = getTestBase();
            testResources = ResourceBundle.getBundle("xacml" + fileseparator +
                    "XACMLTest");
            pdpentityname = testResources.getString(
                    "fam-xacml-pdp-entity-name");
            pepentityname = testResources.getString(
                    "fam-xacml-pep-entity-name");
            certAlias = testResources.getString("fam-xacml-cert-alias");
            pdpCot = testResources.getString("fam-xacml-pdp-cot");
            pepCot = testResources.getString("fam-xacml-pep-cot");
            pdpmetaAlias = testResources.getString("fam-xacml-pdp-metaAlias");
            pepmetaAlias = testResources.getString("fam-xacml-pep-metaAlias");
            webClient = new WebClient();
            HtmlPage pepmetaPage;
            HtmlPage pdpmetaPage;
            consoleLogin(webClient, loginURL, adminUser, adminPassword);
            
            if (FederationManager.getExitCode(spfm.createCot(webClient,
                    pepCot, realm , null , null )) != 0 ) {
                log(Level.SEVERE, "setup", "create-cot " + pepCot +
                        " famadm command failed");
                assert false;
            }
            
            
            if (FederationManager.getExitCode(spfm.createCot(webClient,
                    pdpCot, realm , null , null )) != 0 ) {
                log(Level.SEVERE, "setup", "create-cot " + pdpCot +
                        " famadm command failed");
                assert false;
            }
            //Load PDP meta
            pdpmetaPage = spfm.createMetadataTempl(webClient,
                    pdpentityname, true, true, null, null, null,
                    null, null, null, pdpmetaAlias, null , null,
                    null, null, null, null, null , null, null,
                    null, null, null, null, null,null, null,
                    null, null, null );
            
            if (FederationManager.getExitCode(pdpmetaPage) != 0) {
                log(Level.SEVERE, "setup", "create-metadata-templ" +
                        " command failed");
                assert false;
            }
            arrpMetadata[0] = getMetadataFromPage(pdpmetaPage, "saml2");
            arrpMetadata[1] = getExtMetadataFromPage(pdpmetaPage, "saml2");
            
            if ((arrpMetadata[0].equals(null)) ||
                    (arrpMetadata[1].equals(null))) {
                assert(false);
            } else {
                if (FederationManager.getExitCode(spfm.importEntity(webClient,
                        realm , arrpMetadata[0], arrpMetadata[1],
                        pdpCot, " ")) != 0) {
                    log(Level.SEVERE, "setup", "import-entity command failed");
                    arrpMetadata[0] = null;
                    arrpMetadata[1] = null;
                    assert(false);
                }
            }
            
            //Load PEP Meta
            pepmetaPage = spfm.createMetadataTempl(webClient,
                    pepentityname, true, true, null, null, null,
                    null, null, pepmetaAlias, null, null , null,
                    null, null, null, null, null , null, null,
                    null, null, null, null, null, null, null,
                    null, null, null );
            if (FederationManager.getExitCode(pepmetaPage) != 0) {
                log(Level.SEVERE, "setup", "create-metadata-templ" +
                        " command failed");
                assert false;
            }
            arrMetadata[0] = getMetadataFromPage(pepmetaPage, "saml2");
            arrMetadata[1] = getExtMetadataFromPage(pepmetaPage, "saml2");
            if ((arrMetadata[0].equals(null)) || (arrMetadata[1].equals(null)))
            {
                assert(false);
            } else {
                arrMetadata[0] = arrMetadata[0].replaceAll("&lt;", "<");
                arrMetadata[0] = arrMetadata[0].replaceAll("&gt;", ">");
                arrMetadata[1] = arrMetadata[1].replaceAll("&lt;", "<");
                arrMetadata[1] = arrMetadata[1].replaceAll("&gt;", ">");
                if (FederationManager.getExitCode(spfm.importEntity(webClient,
                        realm, arrMetadata[0], arrMetadata[1],
                        pepCot, " ")) != 0) {
                    arrMetadata[0] = null;
                    arrMetadata[1] = null;
                    assert(false);
                }
            }
            if (FederationManager.getExitCode(spfm.addCotMember(webClient,
                    pdpCot, pepentityname, realm, "saml2")) != 0 ) {
                assert(false);
            }
            
            if (FederationManager.getExitCode(spfm.addCotMember(webClient,
                    pepCot, pdpentityname, realm, "saml2")) != 0 ) {
                assert(false);
            }
            
            attributevalues.add("sunSAML2RequestHandlerList=key="
                    + pdpmetaAlias + "|" +
                    "class=com.sun.identity.xacml.plugins." +
                    "XACMLAuthzDecisionQueryHandler");
            
            if (FederationManager.getExitCode(spfm.addAttrDefs(webClient,
                    servicename, schematype, attributevalues, null)) != 0 ) {
                assert(false);
            }
            // strLocRB and strGblRB contain properties that are needed to
            // create XACML policies for the tests these are not changable
            // by the user.
            ResourceBundle userRes = ResourceBundle.getBundle("xacml" +
                    fileseparator + strLocRB);
            ResourceBundle accessRes = ResourceBundle.getBundle("xacml" +
                    fileseparator + strGblRB);
            acessResource = accessRes.getString("xacmlPolicyGlobal.resource0");
            allowedUser = userRes.getString("xacmlPolicyTest0.identity0.name");
            denyUser = userRes.getString("xacmlPolicyTest0.identity1.name");
            allowPolicyName = userRes.getString(
                    "xacmlPolicyTest0.policy0.name");
            denyPolicyName = userRes.getString("xacmlPolicyTest0.policy1.name");
            createTestPolicy();
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("setup");
        
    }
    
    /**
     * Validates for the XACML Decision for the Permit
     *
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testPermitXACML()
    throws Exception {
        try {
            String testProp = createTestProperties("Permit");
            XACMLClient xacmlTestVal = new XACMLClient();
            String content = xacmlTestVal.testXACML("xacml" + fileseparator +
                    testProp);
            if (content.indexOf(expectedPermit) == -1) {
                log(Level.SEVERE, "testPermitXACML", "The expected result did "+
                        "NOT match with the output");
                log(Level.SEVERE, "testPermitXACML", "The actual XACML" +
                        " Response output is: \n" + content);
            }
            assert(content.indexOf(expectedPermit) != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "testPermitXACML", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testPermitXACML");
    }
    
    /**
     * Validates for the XACML Decision for the Deny
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testDenyXACML()
    throws Exception {
        try {
            String testProp = createTestProperties("Deny");
            XACMLClient xacmlTestVal = new XACMLClient();
            String content = xacmlTestVal.testXACML("xacml" + fileseparator +
                    testProp);
            if (content.indexOf(expectedDeny) == -1) {
                log(Level.SEVERE, "testDenyXACML", "The expected result did "+
                        "NOT match with the output");
                log(Level.SEVERE, "testDenyXACML", "The actual XACML" +
                        " Response output" + "is: \n" + content);
            }
            assert(content.indexOf(expectedDeny) != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "testDenyXACML", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testDenyXACML");
    }
    
    /**
     * Validates for the XACML Decision for the Indeterminate
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testIndeterminateXACML()
    throws Exception {
        try {
            String testProp = createTestProperties("Indeterminate");
            XACMLClient xacmlTestVal = new XACMLClient();
            String content = xacmlTestVal.testXACML("xacml" + fileseparator +
                    testProp);
            if (content.indexOf(expectedInderminate) == -1) {
                log(Level.SEVERE, "testIndeterminateXACML", "The expected " +
                        "result did " + "NOT match with the output");
                log(Level.SEVERE, "testIndeterminateXACML", "The actual" +
                        " XACML Response output" + "is: \n" + content);
            }
            assert(content.indexOf(expectedInderminate) != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "testIndeterminateXACML", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIndeterminateXACML");
    }
    
   
    /**
     * This method is to clear the initial setup. It does the following:
     * (1) Deletes users
     * (2) Delete PEP and PDP entities
     * (3) Delete Policies
     * (4) Delete COTs
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            WebClient webClient = new WebClient();
            consoleLogin(webClient, loginURL, adminUser, adminPassword);
            list = new ArrayList();
            list.add(allowedUser);
            list.add(denyUser);
            log(Level.FINE, "cleanup", "Deleting users  ...");
            if (FederationManager.getExitCode(spfm.deleteIdentities(webClient,
                    realm, list, "User")) != 0) {
                log(Level.SEVERE, "cleanup",
                        "deleteIdentities (User) famadm command failed");
            }
            list.clear();
            list.add(allowPolicyName);
            list.add(denyPolicyName);
            
            if (FederationManager.getExitCode(spfm.deletePolicies(webClient,
                    realm, list )) !=0 ) {
                log(Level.SEVERE, "cleanup",
                        "delete Policies famadm command failed");
            }
            
            if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                    pdpentityname, realm, false, "saml2")) !=0 ) {
                log(Level.SEVERE, "cleanup",
                        "delete PDP Entity famadm command failed");
            }
            if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                    pepentityname, realm, false, "saml2")) !=0 ) {
                log(Level.SEVERE, "cleanup",
                        "delete PEP Entity famadm command failed");
            }
            if (FederationManager.getExitCode(spfm.deleteCot(webClient, pdpCot,
                    realm)) !=0 ) {
                log(Level.SEVERE, "cleanup",
                        "delete PDP COT famadm command failed");
            }
            if (FederationManager.getExitCode(spfm.deleteCot(webClient, pepCot,
                    realm)) !=0 ) {
                log(Level.SEVERE, "cleanup",
                        "delete PEP COT famadm command failed");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("cleanup");
    }
    
     /**
     * This method grep Metadata from the htmlpage & returns as the string.
     * @param HtmlPage page which contains metadata
     * @param spec spec
     */
    private String getMetadataFromPage(HtmlPage page, String spec)
    throws Exception {
        String metadata = "";
        if ((spec.equals("saml2")) || (spec.equals("idff"))) {
            metadata = MultiProtocolCommon.getMetadataFromPage(page);
        }
        return metadata;
    }
    
    /**
     * This method grep ExtendedMetadata from the htmlpage & returns the string
     * @param HtmlPage page which contains extended metadata
     * @param spec spec
     */
    private String getExtMetadataFromPage(HtmlPage page, String spec) 
            throws Exception {
        String metadata = "";
        if ((spec.equals("saml2")) || (spec.equals("idff"))) {
            metadata = MultiProtocolCommon.getExtMetadataFromPage(page);
        }
        return metadata;
    }
    
    /**
     * Dynamically generates the XACML test properties file
     * which is consumed by each of the XACML tests
     * @param property file for type of test
     */
    private String  createTestProperties(String strProp) {
        String xamlprop ;
        xamlprop = "xacmlTest_" + strProp;
        String absFileName = baseDir + "xacml" + fileseparator + xamlprop;
        try {
            Properties pro = new Properties();
            pro.setProperty("pdp.entityId" , pdpentityname);
            pro.setProperty("pep.entityId" , pepentityname);
            if (strProp.equalsIgnoreCase("Permit")){
                pro.setProperty("subject.id" , "id=" + allowedUser
                        + ",ou=user," + basedn);
                
            } else if (strProp.equalsIgnoreCase("Deny")){
                pro.setProperty("subject.id" , "id=" + denyUser
                        + ",ou=user," + basedn);
            } else {
                pro.setProperty("subject.id" , "id=IndeterUser"
                        +",ou=user," + basedn);
            }
            pro.setProperty("subject.id.datatype",
                    "urn:oasis:names:tc:xacml:1.0:data-type:x500Name");
            pro.setProperty("subject.category",
                    "urn:oasis:names:tc:xacml:1.0:subject-category:" +
                    "access-subject");
            pro.setProperty("resource.id" , acessResource);
            pro.setProperty("resource.id.datatype" ,
                    "http://www.w3.org/2001/XMLSchema#string");
            pro.setProperty("resource.servicename" ,
                    "iPlanetAMWebAgentService");
            pro.setProperty("resource.servicename.datatype",
                    "http://www.w3.org/2001/XMLSchema#string");
            pro.setProperty("action.id", testAction);
            pro.setProperty("action.id.datatype" ,
                    "http://www.w3.org/2001/XMLSchema#string");
            pro.store(new FileOutputStream(absFileName + ".properties"), null);
        } catch (Exception e) {
            log(Level.SEVERE, "createTestProperties", e.getMessage());
            e.printStackTrace();
        }
        return xamlprop;
    }
    
    /**
     * Creates the XACML Policies
     * (1) policy00 and (2) policy01
     * xacmlPolicyTest and xacmlGlobal are the policy related
     * property files used to create policies these files
     * should not modified by the end user.
     */
    private void createTestPolicy() {
        
        int polIdx = 0;
        try {
            mpc = new PolicyCommon();
            rbg = ResourceBundle.getBundle("xacml" + fileseparator + strGblRB);
            rbp = ResourceBundle.getBundle("xacml" + fileseparator + strLocRB);
            mpc.createIdentities("xacml" + fileseparator + strLocRB, polIdx,
                    realm );
            mpc.createPolicyXML("xacml" + fileseparator + strGblRB, "xacml" +
                    fileseparator + strLocRB, polIdx , "xacml" + fileseparator +
                    strLocRB + ".xml", realm);
            mpc.createPolicy("xacml" + fileseparator + strLocRB + ".xml",
                    realm);
        } catch (Exception e){
            log(Level.SEVERE, "createTestPolicy", "Create xacml policy failed");
            log(Level.SEVERE, "createTestPolicy",e.getMessage());
            e.printStackTrace();
        }
    }    
}
