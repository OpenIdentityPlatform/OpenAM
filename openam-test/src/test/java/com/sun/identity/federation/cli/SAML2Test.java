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
 * $Id: SAML2Test.java,v 1.9 2008/11/20 17:53:03 veiming Exp $
 *
 */

package com.sun.identity.federation.cli;

import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIRequest;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.DevNullOutputWriter;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.XACMLPDPConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.XACMLAuthzDecisionQueryConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLPDPDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.test.common.TestBase;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SAML2Test extends TestBase {
    private CommandManager cmdManager;
    private static DevNullOutputWriter outputWriter = new DevNullOutputWriter();
    private static String NAME_COT = "clitest";
    private static String NAME_IDP = "www.idp.com";

    public SAML2Test() {
        super("FederationCLI");
    }
    
    /**
     * Create the CLIManager.
     */
    @BeforeTest(groups = {"cli"})
    public void suiteSetup()
        throws CLIException
    {
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "famadm");
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.federation.cli.FederationManager");
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        cmdManager = new CommandManager(env);
    }

    @Test(groups = {"samlv2"})
    public void createCircleOfTrust()
        throws CLIException, COTException, SAML2MetaException {
        entering("createCircleOfTrust", null);
        String[] args = {"create-cot",
            CLIConstants.PREFIX_ARGUMENT_LONG + FedCLIConstants.ARGUMENT_COT,
            NAME_COT
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            CircleOfTrustManager cotManager = new CircleOfTrustManager();
            CircleOfTrustDescriptor objCircleOfTrust = 
                cotManager.getCircleOfTrust("/", NAME_COT);
            assert(objCircleOfTrust != null);
        } finally {
            exiting("createCircleOfTrust");
        }
    }

    @Test(groups = {"samlv2", "samlv2op"},
        dependsOnMethods={"createCircleOfTrust"})
    public void createMetaTemplate()
        throws CLIException {
        entering("createMetaTemplate", null);
        String[] args = {
            "create-metadata-templ",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_ENTITY_ID,
            NAME_IDP,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_METADATA,
            "meta",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_EXTENDED_DATA,
            "extended",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_SERVICE_PROVIDER,
            "/sp",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_IDENTITY_PROVIDER,
            "/idp",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_PDP,
            "/pdp",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_PEP,
            "/pep",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.SPECIFICATION_VERSION,
            FedCLIConstants.SAML2_SPECIFICATION
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("createMetaTemplate");
    }

    @Test(groups = {"samlv2", "samlv2op"},
        dependsOnMethods={"createMetaTemplate"})
    public void importEntity()
        throws CLIException, SAML2MetaException {
        entering("importEntity", null);
        String[] args = {
            "import-entity",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_METADATA,
            "meta",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_EXTENDED_DATA,
            "extended",
            CLIConstants.PREFIX_ARGUMENT_LONG + 
                FedCLIConstants.ARGUMENT_COT,
            NAME_COT,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.SPECIFICATION_VERSION,
            FedCLIConstants.SAML2_SPECIFICATION
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        SAML2MetaManager mgr = new SAML2MetaManager();
        EntityDescriptorElement entity = mgr.getEntityDescriptor(
            "/", NAME_IDP);
        assert (entity != null);

        SPSSODescriptorElement spElt = mgr.getSPSSODescriptor("/", NAME_IDP);
        assert(spElt != null);
        IDPSSODescriptorElement idpElt = mgr.getIDPSSODescriptor("/", NAME_IDP);
        assert(idpElt != null);
        XACMLPDPDescriptorElement pdpElt = mgr.getPolicyDecisionPointDescriptor(
            "/", NAME_IDP);
        assert(pdpElt != null);
        XACMLAuthzDecisionQueryDescriptorElement pepElt =
            mgr.getPolicyEnforcementPointDescriptor("/", NAME_IDP);
        assert(pepElt != null);

        IDPSSOConfigElement idpConfig = mgr.getIDPSSOConfig("/", NAME_IDP);
        assert(idpConfig != null);
        SPSSOConfigElement spConfig = mgr.getSPSSOConfig("/", NAME_IDP);
        assert(spConfig != null);
        XACMLPDPConfigElement pdpConfig = mgr.getPolicyDecisionPointConfig(
            "/", NAME_IDP);
        assert(pdpConfig != null);
        XACMLAuthzDecisionQueryConfigElement pepConfig = 
            mgr.getPolicyEnforcementPointConfig("/", NAME_IDP);
        assert(pepConfig != null);

        exiting("importEntity");
    }

    @Test(groups = {"samlv2", "samlv2entityop"},
        dependsOnMethods={"importEntity"})
    public void listEntity()
        throws CLIException, SAML2MetaException {
        entering("listEntity", null);
        String[] args = {
            "list-entities",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.SPECIFICATION_VERSION,
            FedCLIConstants.SAML2_SPECIFICATION
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("listEntity");
    }
    
    @Test(groups = {"samlv2", "samlv2entityop"},
        dependsOnMethods={"importEntity"})
    public void listCircleOfTrustMembers()
        throws CLIException, SAML2MetaException {
        entering("listCircleOfTrustMembers", null);
        String[] args = {
            "list-cot-members",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_COT,
            NAME_COT,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.REALM_NAME,
            "/"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("listCircleOfTrustMembers");
    }

    @Test(groups = {"samlv2", "samlv2entityop"},
        dependsOnMethods={"importEntity"})
    public void listCircleOfTrusts()
        throws CLIException, SAML2MetaException {
        entering("listCircleOfTrusts", null);
        String[] args = {
            "list-cots",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.REALM_NAME,
            "/"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("listCircleOfTrusts");
    }

    @Test(groups = {"samlv2", "samlv2entityop"},
        dependsOnMethods={"importEntity"})
    public void exportEntity()
        throws CLIException, SAML2MetaException {
        entering("exportEntity", null);
        String[] args = {
            "export-entity",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_ENTITY_ID,
            NAME_IDP,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_METADATA,
            "meta",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_EXTENDED_DATA,
            "extended",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.SPECIFICATION_VERSION,
            FedCLIConstants.SAML2_SPECIFICATION
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("exportEntity");
    }
    
    @Test(groups = {"samlv2", "samlv2op"},
        dependsOnMethods={"removeProviderFromCircleOfTrust"})
    public void deleteEntity()
        throws CLIException, SAML2MetaException {
        entering("deleteEntity", null);
        String[] args = {
            "delete-entity",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_ENTITY_ID,
            NAME_IDP,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.SPECIFICATION_VERSION,
            FedCLIConstants.SAML2_SPECIFICATION
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        SAML2MetaManager mgr = new SAML2MetaManager();
        EntityDescriptorElement entity = mgr.getEntityDescriptor(
            "/", NAME_IDP);

        assert (entity == null);
        exiting("deleteEntity");
    }

    @Test(groups = {"samlv2", "samlv2op"}, dependsOnGroups={"samlv2entityop"})
    public void removeProviderFromCircleOfTrust()
        throws CLIException, SAML2MetaException {
        entering("removeProviderFromCircleOfTrust", null);
        String[] args = {
            "remove-cot-member",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_ENTITY_ID,
            NAME_IDP,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.ARGUMENT_COT,
            NAME_COT,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                FedCLIConstants.SPECIFICATION_VERSION,
            FedCLIConstants.SAML2_SPECIFICATION
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("removeProviderFromCircleOfTrust");
    }

    @Test(groups = {"samlv2"}, 
        dependsOnMethods = {"removeProviderFromCircleOfTrust"},
        expectedExceptions = {COTException.class})
    public void deleteCircleOfTrust()
        throws CLIException, COTException, SAML2MetaException {
        entering("deleteCircleOfTrust", null);
        String[] args = {"delete-cot",
            CLIConstants.PREFIX_ARGUMENT_LONG + FedCLIConstants.ARGUMENT_COT,
            NAME_COT
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        try {
            cmdManager.serviceRequestQueue();
            CircleOfTrustManager cotManager = new CircleOfTrustManager();
            CircleOfTrustDescriptor objCircleOfTrust = 
                cotManager.getCircleOfTrust("/", NAME_COT);
        } finally {
            exiting("deleteCircleOfTrust");
        }
    }
}
