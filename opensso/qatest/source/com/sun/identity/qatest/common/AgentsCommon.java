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
 * $Id: AgentsCommon.java,v 1.10 2008/06/26 20:10:38 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdType;
import com.sun.identity.policy.client.PolicyEvaluator;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.SMSCommon;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * This class has common methods related to policy management and is
 * consumed both, by policy api tests and agents tests.
 */
public class AgentsCommon extends TestCommon {
    
    private String loginURL;
    private String logoutURL;
    private String fmadmURL;
    private String baseDir;
    private String strGlobalRB;
    private String strLocalRB;
    private FederationManager fmadm;
    private IDMCommon idmc;
    private WebClient webClient;
    private SSOToken usertoken;
    private SSOToken admintoken;
    
    /**
     * Class constructor. Sets class variables.
     */
    public AgentsCommon()
    throws Exception{
        super("AgentsCommon");
        loginURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Login";
        logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";
        fmadmURL = protocol + ":" + "//" + host + ":" + port + uri;
        ResourceBundle rbAMConfig = ResourceBundle.
                getBundle(TestConstants.TEST_PROPERTY_AMCONFIG);
        idmc = new IDMCommon();
        try {
            baseDir = getTestBase();
            fmadm = new FederationManager(fmadmURL);
        } catch (Exception e) {
            log(Level.SEVERE, "AgentsCommon", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Create policy xml file.
     */
    public void createPolicyXML(String strGblRB, String strLocRB,
            int policyIdx, String policyFile)
    throws Exception {
        String strResource;
        strGlobalRB = strGblRB.substring(strGblRB.indexOf(fileseparator) + 1,
                strGblRB.length());;
        strLocalRB = strLocRB.substring(strLocRB.indexOf(fileseparator) + 1,
                strLocRB.length());;
        ResourceBundle rbg = ResourceBundle.getBundle(strGblRB);
        ResourceBundle rbp = ResourceBundle.getBundle(strLocRB);
        String glbPolIdx = strLocalRB + policyIdx + ".";
        String locPolIdx = glbPolIdx + "policy";
        
        int noOfPolicies = new Integer(rbp.getString(glbPolIdx +
                "noOfPolicies")).intValue();
        
        FileWriter fstream = new FileWriter(baseDir + policyFile);
        BufferedWriter out = new BufferedWriter(fstream);
        
        writeHeader(out);
        out.write("<Policies>");
        out.write(newline);
        
        for (int i = 0; i < noOfPolicies; i++) {
            
            out.write("<Policy name=\"" + rbp.getString(locPolIdx + i +
                    ".name") + "\" referralPolicy=\"" +
                    rbp.getString(locPolIdx + i + ".referral") +
                    "\" active=\"" + rbp.getString(locPolIdx + i +
                    ".active") + "\">");
            out.write(newline);
            
            int noOfRules = new Integer(rbp.getString(locPolIdx + i +
                    ".noOfRules")).intValue();
            if (noOfRules != 0) {
                for (int j = 0; j < noOfRules; j++) {
                    out.write("<Rule name=\"" + rbp.getString(locPolIdx + i +
                            ".rule" + j + ".name") + "\">" );
                    out.write(newline);
                    out.write("<ServiceName name=\"" +
                            rbp.getString(locPolIdx + i + ".rule" + j +
                            ".serviceName") + "\"/>" );
                    out.write(newline);
                    strResource = rbp.getString(locPolIdx + i + ".rule" + j +
                            ".resource");
                    out.write("<ResourceName name=\"" +
                            rbg.getString(strResource) + "\"/>" );
                    int noOfActions = new Integer(rbp.getString(locPolIdx + i +
                            ".rule" + j + ".noOfActions")).intValue();
                    if (noOfActions != 0) {
                        List list = new ArrayList();
                        for (int k = 0; k < noOfActions; k++) {
                            list.add(rbp.getString(locPolIdx + i + ".rule" + j +
                                    ".action" + k));
                        }
                        createSubXML(rbg, rbp, out, null, null, null, list);
                    }
                    out.write("</Rule>");
                }
                out.write(newline);
            }
            
            int noOfSubjects = new Integer(rbp.getString(locPolIdx + i +
                    ".noOfSubjects")).intValue();
            if (noOfSubjects != 0) {
                out.write("<Subjects name=\"" + "subjects" +
                        "\" description=\"" + "description\">");
                out.write(newline);
                String name;
                String type;
                String incType;
                for (int j = 0; j < noOfSubjects; j++) {
                    name = rbp.getString(locPolIdx + i + ".subject" + j +
                            ".name");
                    type = rbp.getString(locPolIdx + i + ".subject" + j +
                            ".type");
                    incType = rbp.getString(locPolIdx + i + ".subject" + j +
                            ".includeType");
                    int noOfAttributes = new Integer(rbp.getString(locPolIdx +
                            i + ".subject" + j + ".noOfAttributes")).intValue();
                    if (noOfAttributes != 0) {
                        List list = new ArrayList();
                        for (int k = 0; k < noOfAttributes; k++) {
                            list.add(rbp.getString(locPolIdx + i + ".subject" +
                                    j + ".att" + k));
                        }
                        createSubXML(rbg, rbp, out, "Subject", name, type,
                                incType, list);
                    } else
                        createSubXML(rbg, rbp, out, "Subject", name, type,
                                incType, null);
                }
                out.write("</Subjects>");
                out.write(newline);
            }
            
            int noOfConditions = new Integer(rbp.getString(locPolIdx + i +
                    ".noOfConditions")).intValue();
            if (noOfConditions != 0) {
                out.write("<Conditions name=\"" + "conditions" +
                        "\" description=\"" + "desc\">");
                out.write(newline);
                String name;
                String type;
                for (int j = 0; j < noOfConditions; j++) {
                    name = rbp.getString(locPolIdx + i + ".condition" + j +
                            ".name");
                    type = rbp.getString(locPolIdx + i + ".condition" + j +
                            ".type");
                    int noOfAttributes = new Integer(rbp.getString(locPolIdx +
                            i + ".condition" + j +
                            ".noOfAttributes")).intValue();
                    if (noOfAttributes != 0) {
                        List list = new ArrayList();
                        for (int k = 0; k < noOfAttributes; k++) {
                            list.add(rbp.getString(locPolIdx + i +
                                    ".condition" + j + ".att" + k));
                        }
                        createSubXML(rbg, rbp, out, "Condition", name, type,
                                list);
                    }
                }
                out.write("</Conditions>");
                out.write(newline);
            }
            
            int noOfResponseProviders = new Integer(rbp.getString(locPolIdx +
                    i + ".noOfResponseProviders")).intValue();
            if (noOfResponseProviders != 0) {
                Map map = new HashMap();
                out.write("<ResponseProviders name=\"" + "responseproviders" +
                        "\" description=\"" + "desc\">");
                out.write(newline);
                String name;
                String type;
                for (int j = 0; j < noOfResponseProviders; j++) {
                    name = rbp.getString(locPolIdx + i + ".responseprovider" +
                            j + ".name");
                    type = rbp.getString(locPolIdx + i + ".responseprovider" +
                            j + ".type");
                    int noOfStatic = new Integer(rbp.getString(locPolIdx + i +
                            ".responseprovider" + j +
                            ".noOfStatic")).intValue();
                    if (noOfStatic != 0) {
                        List list = new ArrayList();
                        String strStaticAttribute = rbp.getString(locPolIdx +
                                i + ".responseprovider" + j +
                                ".staticAttributeName");
                        for (int k = 0; k < noOfStatic; k++) {
                            list.add(rbp.getString(locPolIdx + i +
                                    ".responseprovider" + j + ".static" + k));
                        }
                        map.put(strStaticAttribute, list);
                    }
                    int noOfDynamic = new Integer(rbp.getString(locPolIdx + i +
                            ".responseprovider" + j +
                            ".noOfDynamic")).intValue();
                    if (noOfDynamic != 0) {
                        List list = new ArrayList();
                        List dynSetList = new ArrayList();
                        String strDynamicAttribute = rbp.getString(locPolIdx +
                                i + ".responseprovider" + j +
                                ".dynamicAttributeName");
                        for (int k = 0; k < noOfDynamic; k++) {
                            String attrValue = rbp.getString(locPolIdx + i +
                                    ".responseprovider" + j + ".dynamic" + k);
                            list.add(attrValue);
                            dynSetList.add("sun-am-policy-dynamic-response-" +
                                    "attributes" +  "=" + attrValue);
                        }
                        map.put(strDynamicAttribute, list);
                        setDynamicRespAttribute(realm, dynSetList);
                        createRPXML(out, "ResponseProvider", name, type, map);
                    }
                }
                out.write("</ResponseProviders>");
                out.write(newline);
            }
            out.write("</Policy>");
            out.write(newline);
        }
        out.write("</Policies>");
        out.write(newline);
        out.close();
    }
    
    /**
     * Creates part of policy xml related to atttributes. Does not take
     * Subject includeType as part of input parameters.
     */
    public void createSubXML(ResourceBundle rbg, ResourceBundle rbp,
            BufferedWriter out, String nameType, String name, String type,
            List list)
    throws Exception {
        createSubXML(rbg, rbp, out, nameType, name, type, null, list);
    }
    
    /**
     * Creates part of policy xml related to atttributes.
     */
    public void createSubXML(ResourceBundle rbg, ResourceBundle rbp,
            BufferedWriter out, String nameType, String name, String type,
            String includeType, List list)
    throws Exception {
        
        if (nameType != null) {
            if (includeType != null)
                out.write("<" + nameType + " name=\"" + name + "\"" +
                        " type=\"" + type + "\" includeType=\"" +
                        includeType + "\">");
            else
                out.write("<" + nameType + " name=\"" + name + "\"" +
                        " type=\"" + type + "\">");
        }
        out.write(newline);
        if (list != null) {
            if (list.size() > 0) {
                int iIdx;
                String key;
                String value;
                String subType;
                String subName;
                String idPrefix = null;
                String idSuffix = null;
                String uuid;
                for (int i = 0; i < list.size(); i++) {
                    out.write("<AttributeValuePair>");
                    out.write(newline);
                    iIdx = ((String)list.get(i)).indexOf("=");
                    key = ((String)list.get(i)).substring(0, iIdx);
                    value = ((String)list.get(i)).substring(iIdx + 1,
                            ((String)list.get(i)).length());
                    
                    if (includeType != null) {
                        subType = rbp.getString(value + ".type");
                        subName = rbp.getString(value + ".name");
                        idPrefix = rbg.getString(strGlobalRB + ".uuid.prefix."
                                + type + "." + subType);
                        idSuffix = rbg.getString(strGlobalRB + ".uuid.suffix."
                                + type + "." + subType);
                        if (idSuffix.equals(null) || idSuffix.equals("")) {
                            uuid = idPrefix + "=" + subName + "," + basedn;
                        } else {
                            uuid = idPrefix + "=" + subName + "," + idSuffix +
                                    "," + basedn;
                        }
                        out.write("<Attribute name=\"" + key + "\"/><Value>" +
                                uuid + "</Value>");
                    } else
                        out.write("<Attribute name=\"" + key + "\"/><Value>" +
                                value + "</Value>");
                    out.write(newline);
                    out.write("</AttributeValuePair>");
                    out.write(newline);
                }
            }
        }
        if (nameType != null) {
            out.write("</" + nameType + ">");
            out.write(newline);
        }
    }
    
    /**
     * Creates part of policy xml related to atttributes for ResponseProviders.
     */
    public void createRPXML(BufferedWriter out, String nameType, String name,
            String type, Map map) 
    throws Exception {
        if (nameType != null) {
            out.write("<" + nameType + " name=\"" + name + "\"" + " type=\"" +
                    type + "\">");
            out.write(newline);
        }
        if (map != null) {
            if (map.size() > 0) {
                int iIdx;
                String key;
                List value;
                Set s = map.keySet();
                Iterator it = s.iterator();
                while (it.hasNext()) {
                    key = (String)it.next();
                    value = (List)map.get(key);
                    out.write("<AttributeValuePair>");
                    out.write("<Attribute name=\"" + key + "\"/>");
                    out.write(newline);
                    for (int i = 0; i < value.size(); i++) {
                        out.write("<Value>" + (String)value.get(i) +
                                "</Value>");
                        out.write(newline);
                    }
                    out.write("</AttributeValuePair>");
                    out.write(newline);
                }
            }
        }
        if (nameType != null) {
            out.write("</" + nameType + ">");
            out.write(newline);
        }
    }
    
    /**
     * Creates identities (users, roles and groups) required by the policy
     * definition and policy evaluation.
     */
    public void createIdentities(String strLocRB, int glbPolIdx)
    throws Exception {
        try {
            ResourceBundle rb = ResourceBundle.getBundle(strLocRB);
            String strPolIdx = strLocRB.substring(
                    strLocRB.indexOf(fileseparator) + 1, strLocRB.length()) +
                    glbPolIdx;
            int noOfIdentities = new Integer(rb.getString(strPolIdx +
                    ".noOfIdentities")).intValue();
            for (int i = 0; i < noOfIdentities; i++) {
                String name = rb.getString(strPolIdx + ".identity" + i +
                        ".name");
                String type = rb.getString(strPolIdx + ".identity" + i +
                        ".type");
                String strAttList = rb.getString(strPolIdx + ".identity" + i +
                        ".attributes");
                List list = null;
                if (!(strAttList.equals("")))
                    list = getAttributeList(strAttList, ",");
                webClient = new WebClient();
                consoleLogin(webClient, loginURL, adminUser, adminPassword);
                //fixed the properties files, now there is no identity with out
                //an attribute value
                if (list != null) {
                    int retval = FederationManager.getExitCode( 
                            fmadm.createIdentity( webClient, realm, name, 
                            type, list));
                    if (retval != 0 ) {
                        log(Level.SEVERE, "createIdentities",
                            "createIdentity (not null list) famadm" +
                            " command failed for Identity : " + name + 
                            ", of type : " + type);
                        assert false;
                    }
                } else {
                    log(Level.SEVERE, "createIdentities",
                        "Identity attribute list cannot be null" +
                        "check your properties file, make sure " +
                        "all the identities have atleast one " +
                        "attribute defined");
                    assert false;
                }
                
                String isMemberOf = rb.getString(strPolIdx + ".identity" + i +
                        ".isMemberOf");
                if (isMemberOf.equals("yes")) {
                    String memberList = rb.getString(strPolIdx + ".identity" +
                            i + ".memberOf");
                    List lstMembers = getAttributeList(memberList, ",");
                    String strIDIdx;
                    String memberType;
                    String memberName;
                    for (int j = 0; j < lstMembers.size(); j++) {
                        strIDIdx = (String)lstMembers.get(j);
                        memberType = rb.getString(strIDIdx + ".type");
                        memberName = rb.getString(strIDIdx + ".name");
                        if (FederationManager.getExitCode(fmadm.addMember(
                                webClient, realm, name, type, memberName,
                                memberType)) != 0) {
                            log(Level.SEVERE, "createIdentities", "addMember" +
                                    " famadm command failed");
                            assert false;
                        }
                    }
                }
            }
        } catch(Exception e) {
            log(Level.SEVERE, "createIdentities", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
    }

    /**
     * Creates user, role and group identities using the client API's.
     */
    public void createIdentitiesUsingAPI(String strLocRB, int glbPolIdx)
    throws Exception {
        try {
            ResourceBundle rb = ResourceBundle.getBundle(strLocRB);
            String strPolIdx = strLocRB.substring(
                    strLocRB.indexOf(fileseparator) + 1, strLocRB.length()) +
                    glbPolIdx;
            int noOfIdentities = new Integer(rb.getString(strPolIdx +
                    ".noOfIdentities")).intValue();
            IdType idtype = null;
            for (int i = 0; i < noOfIdentities; i++) {
                String name = rb.getString(strPolIdx + ".identity" + i +
                        ".name");
                String type = rb.getString(strPolIdx + ".identity" + i +
                        ".type");

                if (type.equals("User"))
                    idtype = IdType.USER;
                else if (type.equals("Role"))
                    idtype = IdType.ROLE;
                else if (type.equals("Group"))
                    idtype = IdType.GROUP;

                String strAttList = rb.getString(strPolIdx + ".identity" + i +
                        ".attributes");
                Map map = null;
                if (!(strAttList.equals("")))
                    map = getAttributeMap(strAttList, ",");
                admintoken = getToken(adminUser, adminPassword, basedn);
                if (map != null)
                    idmc.createIdentity(admintoken, realm, idtype, name, map);
                else
                    idmc.createIdentity(admintoken, realm, idtype, name,
                            new HashMap());
                String isMemberOf = rb.getString(strPolIdx + ".identity" + i +
                        ".isMemberOf");
                if (isMemberOf.equals("yes")) {
                    String memberList = rb.getString(strPolIdx + ".identity" +
                            i + ".memberOf");
                    List lstMembers = getAttributeList(memberList, ",");
                    String strIDIdx;
                    String memberType;
                    String memberName;
                    IdType memberIdType = null;
                    for (int j = 0; j < lstMembers.size(); j++) {
                        strIDIdx = (String)lstMembers.get(j);
                        memberType = rb.getString(strIDIdx + ".type");
                        if (memberType.equals("Role"))
                            memberIdType = IdType.ROLE;
                        else if (memberType.equals("GROUP"))
                            memberIdType = IdType.GROUP;
                        memberName = rb.getString(strIDIdx + ".name");
                        idmc.addUserMember(admintoken, name, memberName,
                                memberIdType); 
                    }
                }
            }
        } catch(Exception e) {
            log(Level.SEVERE, "createIdentities", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(admintoken);
        }
    }
    
    /**
     * Create actual policy in the system.
     */
    public void createPolicy(String fileName)
    throws Exception {
        try{
            webClient = new WebClient();
            consoleLogin(webClient, loginURL, adminUser, adminPassword);
            String absFileName = baseDir + fileName;
            String policyXML;
            if (fileName != null) {
                StringBuffer contents = new StringBuffer();
                BufferedReader input = new BufferedReader
                        (new FileReader(absFileName));
                String line = null;
                while ((line = input.readLine()) != null) {
                    contents.append(line + "\n");
                }
                if (input != null)
                    input.close();
                policyXML = contents.toString();
                log(Level.FINEST, "createPolicy", newline + policyXML);
                HtmlPage policyCheckPage ;
                if (FederationManager.getExitCode(fmadm.createPolicies(
                        webClient, realm, policyXML)) != 0) {
                    log(Level.SEVERE, "createPolicy", "createPolicies famadm" +
                            " command failed for policyXML : \n " + policyXML);
                    assert false;
                }
            }
        } catch(Exception e) {
            log(Level.SEVERE, "createPolicy", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
    }
    
    /**
     * Deletes all the identities.
     */
    public void deleteIdentities(String strLocRB, int gPolIdx)
    throws Exception {
        try {
            ResourceBundle rb = ResourceBundle.getBundle(strLocRB);
            String glbPolIdx = strLocRB.substring(
                    strLocRB.indexOf(fileseparator) + 1, strLocRB.length()) +
                    gPolIdx;
            List list = new ArrayList();
            webClient = new WebClient();
            consoleLogin(webClient, loginURL, adminUser, adminPassword);
            String type;
            String name;
            int noOfIdentities = new Integer(rb.getString(glbPolIdx +
                    ".noOfIdentities")).intValue();
            for (int i = 0; i < noOfIdentities; i++) {
                type = rb.getString(glbPolIdx + ".identity" + i + ".type");
                name = rb.getString(glbPolIdx + ".identity" + i + ".name");
                list.clear();
                list.add(name);
                if (FederationManager.getExitCode(fmadm.deleteIdentities(
                        webClient, realm, list, type)) != 0) {
                    log(Level.SEVERE, "deleteIdentities", "deleteIdentities" +
                            " famadm command failed for list : " + list + 
                            ", of type : " + type);
                    assert false;
                }
            }
        } catch(Exception e) {
            log(Level.SEVERE, "deleteIdentities", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
    }
    
    /**
     * Deletes the policies.
     */
    public void deletePolicies(String strLocRB, int gPolIdx)
    throws Exception {
        try {
            ResourceBundle rb = ResourceBundle.getBundle(strLocRB);
            String glbPolIdx = strLocRB.substring(
                    strLocRB.indexOf(fileseparator) + 1, strLocRB.length()) +
                    gPolIdx;
            String locPolIdx = glbPolIdx + ".policy";
            List list = new ArrayList();
            webClient = new WebClient();
            consoleLogin(webClient, loginURL, adminUser, adminPassword);
            String name;
            int noOfPolicies = new Integer(rb.getString(glbPolIdx +
                    ".noOfPolicies")).intValue();
            for (int i = 0; i < noOfPolicies; i++) {
                name = rb.getString(locPolIdx + i + ".name");
                list.add(name);
            }
            if (FederationManager.getExitCode(fmadm.deletePolicies(webClient,
                    realm, list)) != 0) {
                log(Level.SEVERE, "deletePolicies", "deletePolicies famadm" +
                        " command failed for policy list : " + list);
                assert false;
            }
        } catch(Exception e) {
            log(Level.SEVERE, "deletePolicies", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
    }
    
    /**
     * Creates policy header xml
     */
    public void writeHeader(BufferedWriter out)
    throws Exception {
        out.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        out.write(newline);
        out.write("<!DOCTYPE Policies");
        out.write(newline);
        out.write("PUBLIC \"-//Sun Java System Access Manager 7.1 2006Q3" +
                " Admin CLI DTD//EN\"");
        out.write(newline);
        out.write("\"jar://com/sun/identity/policy/policyAdmin.dtd\"");
        out.write(newline);
        out.write(">");
        out.write(newline);
    }
    
    /**
     * Evaluates policy using agents
     */
    public void evaluatePolicyThroughAgents(String resource, String username,
            String password, String expResult)
    throws Exception {
        try {
            webClient = new WebClient();
            HtmlPage page = consoleLogin(webClient, resource, username,
                    password);
            int iIdx = getHtmlPageStringIndex(page, expResult);
            assert (iIdx != -1);
        } catch (com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException e)
        {
            if (resource.indexOf("notvalid") != 0)
                assert true;
            else
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyThroughAgents", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
    }

    /**
     * Evaluates whether attributes are present as header's in the request
     * send back to the agent.
     */
    public void evaluateHeaderAttributes(WebClient webClient, String resource,
            String username, String password, String expResult,
            String attributeName, String attributeValue, String evalScriptURL,
            String login)
    throws Exception {
        try {
            HtmlPage page;
            int iIdx;
            if (login.equals("yes"))
            {
                page = consoleLogin(webClient, resource, username, password);
                iIdx = getHtmlPageStringIndex(page, expResult);
                assert (iIdx != -1);
            }
            log(Level.FINEST, "evaluateHeaderAttributes",
                    "Header Script URL: " + evalScriptURL);
            URL url = new URL(evalScriptURL);
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, attributeName + " : " +
                    attributeValue);
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateHeaderAttributes", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
    }

    /**
     * Evaluates policy using api
     */
    public void evaluatePolicyThroughAPI(String resource, SSOToken usertoken,
            String action, Map envMap, String expResult, int idIdx)
    throws Exception {
        try {
            PolicyEvaluator pe =
                    new PolicyEvaluator("iPlanetAMWebAgentService");
            
            Set actions = new HashSet();
            actions.add(action);
            
            boolean pResult = pe.isAllowed(usertoken, resource, action, envMap);
            log(Level.FINEST, "evaluatePolicyThroughAPI", "Policy Decision: " +
                    pResult);
            
            PolicyDecision pd = pe.getPolicyDecision(usertoken, resource,
                    actions, envMap);
            log(Level.FINEST, "evaluatePolicyThroughAPI",
                    "Polciy Decision XML: " +  pd.toXML());
            boolean expectedResult = new Boolean(expResult).booleanValue();
            
            assert (pResult == expectedResult);
            
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyThroughAPI", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(usertoken);
        }
    }
    
    /**
     * Returns the enviornment map used for policy evaluation when evaluation
     * is through api.
     */
    public Map getPolicyEnvParamMap(String strLocRB, int polIdx, int evalIdx)
    throws Exception {
        Map map = null;
        try {
            ResourceBundle rbp = ResourceBundle.getBundle(strLocRB);
            String glbPolIdx = strLocRB.substring(
                    strLocRB.indexOf(fileseparator) + 1, strLocRB.length()) +
                    polIdx;
            String locEvalIdx = glbPolIdx + ".evaluation";
            int noOfEnvParams = new Integer(rbp.getString(locEvalIdx +
                    evalIdx + ".noOfEnvParamSet")).intValue();
            if (noOfEnvParams != 0) {
                map = new HashMap();
                String type;
                int noOfVal;
                for (int i = 0; i < noOfEnvParams; i++) {
                    type =  rbp.getString(locEvalIdx + evalIdx +
                            ".envParamSet" + i + ".type");
                    noOfVal =  new Integer(rbp.getString(locEvalIdx + evalIdx +
                            ".envParamSet" + i + ".noOfValues")).intValue();
                    Set set = null;
                    if (noOfVal != 0) {
                        set = new HashSet();
                        String strVal;
                        for (int j = 0; j < noOfVal; j++) {
                            strVal =  rbp.getString(locEvalIdx + evalIdx +
                                    ".envParamSet" + i + ".val" + j);
                            set.add(strVal);
                        }
                    }
                    map.put(type, set);
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "getPolicyEnvParamMap", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return (map);
    }
    
    /**
     * sets the requested properties in the sso token
     * @param SSOToken, Map, int
     */
    public void setProperty(String strLocRB, SSOToken usertoken, int polIdx,
            int idIdx)
    throws Exception {
        ResourceBundle rbp = ResourceBundle.getBundle(strLocRB);
        String glbPolIdx = strLocRB.substring(
                strLocRB.indexOf(fileseparator) + 1, strLocRB.length()) +
                polIdx;
        String locEvalIdx = glbPolIdx + ".identity" + idIdx;
        boolean hasSessionAttr = new Boolean(rbp.getString(locEvalIdx +
                ".hasSessionAttributes")).booleanValue();
        if (hasSessionAttr) {
            int noOfSessionAttr = new Integer(rbp.getString(locEvalIdx +
                    ".noOfSessionAttributes")).intValue();
            String strVal;
            String propName;
            String propValue;
            int j;
            for (int i = 0; i < noOfSessionAttr; i++) {
                strVal = rbp.getString(locEvalIdx + ".sessionAttribute" + i);
                j = strVal.indexOf("=");
                propName = strVal.substring(0, j);
                propValue = strVal.substring(j + 1, strVal.length());
                usertoken.setProperty(propName, propValue);
            }
        }
    }
    
    /**
     * Gets the index for identity from the identity resource bundle string
     * identifier
     */
    public int getIdentityIndex(String strIdx)
    throws Exception {
        List list = getAttributeList(strIdx, ".");
        int iLen = ((String)list.get(1)).length();
        int idx = new Integer(((String)list.get(1)).
                substring(iLen - 1, iLen)).intValue();
        return (idx);
    }

    /**
     * Enables the dynamic referral to true at the top level org
     * @param none
     */
    public void setDynamicRespAttribute(String strRealm, List dynRespList)
    throws Exception {
        WebClient webClient = new WebClient();
        try {
           consoleLogin(webClient, loginURL, adminUser, adminPassword);
           if (FederationManager.getExitCode(fmadm.setSvcAttrs(webClient,
                     strRealm, "iPlanetAMPolicyConfigService", dynRespList))
                     != 0) {
                log(Level.SEVERE, "setDynamicRespAttribute", "setSvcAttrs" +
                        " famadm command failed");
                assert false;
            }
       } catch (Exception e) {
           log(Level.SEVERE, "setDynamicRespAttribute", e.getMessage());
           e.printStackTrace();
           throw e;
       } finally {
           consoleLogout(webClient, logoutURL);
       }
    }

    /**
     * Enables the dynamic referral to true at the top level org
     * @param none
     */
    public void setDynamicRespAttributeUsingAPI(String strRealm,
            List dynRespList)
    throws Exception {
       try {
           admintoken = getToken(adminUser, adminPassword, basedn);
           Set set = new HashSet();
           for (int i = 0; i < dynRespList.size(); i++ ) {
               set.add(dynRespList.get(i));
           }
           SMSCommon smsc = new SMSCommon(admintoken);
           smsc.updateSvcAttribute("iPlanetAMPolicyConfigService",
                    "sun-am-policy-dynamic-response-attributes", set,
                    "Organization");
       } catch (Exception e) {
           log(Level.SEVERE, "setDynamicRespAttributeUsingAPI", e.getMessage());
           e.printStackTrace();
           throw e;
       } finally {
           destroyToken(admintoken);
       }
    }
}
