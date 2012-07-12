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
 * $Id: SchemaTest.java,v 1.6 2008/06/25 05:44:19 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.cli.schema;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIRequest;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.DevNullOutputWriter;
import com.sun.identity.cli.IArgument;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.PluginInterface;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.test.common.TestBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class SchemaTest extends TestBase {
    private CommandManager cmdManager;
    private static DevNullOutputWriter outputWriter = new DevNullOutputWriter();
    private static String MOCK_DIR = "mock/cli";
    private static String TEST_SERVICE_XML = MOCK_DIR + "/testService.xml";
    private static String TEST_SERVICE = "TestService";

    public SchemaTest() {
        super("CLI");
    }
    
    /**
     * Create the CLIManager.
     *
     */
    @BeforeTest(groups = {"cli"})
    public void suiteSetup()
        throws CLIException
    {
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "amadm");
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.cli.AccessManager");
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        cmdManager = new CommandManager(env);
    }

    @BeforeTest(groups = {"schema", "subschema"})
    public void loadSchema()
        throws CLIException, SMSException, SSOException {
        entering("loadSchema", null);
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
        } catch (SMSException ex) {
            //create the service if it does not exist
            List<String> list = new ArrayList<String>();
            list.add(TEST_SERVICE_XML);

            try {
                createServices(list);
                assert serviceExists(TEST_SERVICE);
                exiting("loadSchema");
            } catch (CLIException e) {
                this.log(Level.SEVERE, "loadSchema", e.getMessage());
                throw e;
            } catch (SMSException e) {
                this.log(Level.SEVERE, "loadSchema", e.getMessage());
                throw e;
            } catch (SSOException e) {
                this.log(Level.SEVERE, "loadSchema", e.getMessage());
                throw e;
            }
        }
    }

    @AfterTest(groups = {"schema", "subschema"})
    public void deleteService() throws Exception {
        entering("deleteService", null);
        List<String> serviceNames = new ArrayList<String>();
        serviceNames.add(TEST_SERVICE);
        deleteServices(serviceNames);

        try {
            cmdManager.serviceRequestQueue();
            assert !serviceExists(TEST_SERVICE);
        } catch (Exception e) {
            this.log(Level.SEVERE, "deleteService", e.getMessage());
            throw e;
        } finally {
            exiting("deleteService");
        }
    }

    
    @Test(groups = {"schema", "create-svc"})
    public void loadMultipleServices()
        throws CLIException, SMSException, SSOException {
        entering("loadMultipleServices", null);
        List<String> list = new ArrayList<String>();
        list.add(MOCK_DIR + "/testService1.xml");
        list.add(MOCK_DIR + "/testService2.xml");
        list.add(MOCK_DIR + "/testService3.xml");

        try {
            createServices(list);
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                "TestService1", getAdminSSOToken());
            assert serviceExists("TestService1");
            assert serviceExists("TestService2");
            assert serviceExists("TestService3");
            exiting("loadMultipleServices");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "loadMultipleServices", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "loadMultipleServices", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "loadMultipleServices", e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"schema", "delete-svc"},
        dependsOnMethods = {"loadMultipleServices"}
    )
    public void deleteMultipleServices() throws Exception {
        entering("deleteMultipleServices", null);
        List<String> list = new ArrayList<String>();
        list.add("TestService1");
        list.add("TestService2");
        list.add("TestService3");

        try {
            deleteServices(list);

            assert !serviceExists("TestService1");
            assert !serviceExists("TestService2");
            assert !serviceExists("TestService3");
        } catch (Exception e) {
            this.log(Level.SEVERE, "deleteMultipleServices", e.getMessage());
            throw e;
        } finally {
            exiting("deleteMultipleServices");
        }
    }

    @Test(groups = {"schema", "set-inheritance"})
    public void setInheritance()
        throws CLIException, SMSException, SSOException {
        entering("setInheritance", null);
        String[] args = {"set-inheritance",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            TEST_SERVICE,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE,
            "global",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SUBSCHEMA_NAME,
            "subschema-inheritance",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                ModifyInheritance.ARGUMENT_INHERITANCE,
            "multiple"
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema globalSchema = mgr.getSchema(SchemaType.GLOBAL);
            ServiceSchema ss = globalSchema.getSubSchema(
                "subschema-inheritance");
            assert ss.supportsMultipleConfigurations();
            exiting("setInheritance");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setInheritance", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setInheritance", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setInheritance", e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"schema", "create-sub-cfg"})
    public void createSubConfiguration()
        throws CLIException, SMSException, SSOException {
        entering("createSubConfiguration", null);
        String[] args = {"create-sub-cfg",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            TEST_SERVICE,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SUB_CONFIGURATION_ID,
            "subschemaX",
            CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.SUB_CONFIGURATION_NAME,
            "testConfig",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            "attr1=1",
            "attr2=2",
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            ServiceConfigManager scm = new ServiceConfigManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceConfig sc = scm.getGlobalConfig(null);
            sc = sc.getSubConfig("testConfig");
            assert (sc != null);
            exiting("createSubConfiguration");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "createSubConfiguration", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "createSubConfiguration", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "createSubConfiguration", e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"schema", "delete-sub-cfg"},
        dependsOnMethods = {"setSubConfiguration"})
    public void deleteSubConfiguration()
        throws CLIException, SMSException, SSOException {
        entering("deleteSubConfiguration", null);
        String[] args = {"delete-sub-cfg",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            TEST_SERVICE,
            CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.SUB_CONFIGURATION_NAME,
            "/testConfig"
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            ServiceConfigManager scm = new ServiceConfigManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceConfig sc = scm.getGlobalConfig(null);
            sc = sc.getSubConfig("testConfig");
            assert (sc == null);
            exiting("deleteSubConfiguration");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "deleteSubConfiguration", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "deleteSubConfiguration", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "deleteSubConfiguration", e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"schema", "set-sub-cfg"},
        dependsOnMethods = {"createSubConfiguration"})
    public void setSubConfiguration()
        throws CLIException, SMSException, SSOException {
        entering("setSubConfiguration", null);

        String[] args = {"set-sub-cfg",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            TEST_SERVICE,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                ModifySubConfiguration.ARGUMENT_OPERATION,
            "set",
            CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.SUB_CONFIGURATION_NAME,
            "/testConfig",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            "attr1=2",
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            Map map = getSubConfigurationValues("/testConfig");
            Set set = (Set)map.get("attr1");
            String attr1 = (String)set.iterator().next();
            assert attr1.equals("2");

            args[4] = "delete";
            req = new CLIRequest(null, args, getAdminSSOToken());
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            map = getSubConfigurationValues("/testConfig");
            set = (Set)map.get("attr1");
            assert (set == null) || set.isEmpty();

            args[4] = "add";
            args[8] = "attr3=2";
            req = new CLIRequest(null, args, getAdminSSOToken());
            cmdManager.addToRequestQueue(req);
            cmdManager.serviceRequestQueue();
            map = getSubConfigurationValues("/testConfig");
            set = (Set)map.get("attr3");
            attr1 = (String)set.iterator().next();
            assert attr1.equals("2");

            exiting("setSubConfiguration");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setSubConfiguration", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setSubConfiguration", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setSubConfiguration", e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"schema", "add-plugin-interface"})
    public void addPluginInterface()
        throws CLIException, SMSException, SSOException {
        entering("addPluginInterface", null);
        String[] args = {"add-plugin-interface", 
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            TEST_SERVICE,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                AddPluginInterface.ARGUMENT_I18N_KEY,
            "123",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                AddPluginInterface.ARGUMENT_PLUGIN_NAME,
            "testPlugIn",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                AddPluginInterface.ARGUMENT_INTERFACE_NAME,
            "com.sun.identity.cli.schema.DummyInterface"};
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            PluginInterface pl = mgr.getPluginInterface("testPlugIn");
            assert (pl != null);
            exiting("addPluginInterface");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "addPluginInterface", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "addPluginInterface", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "addPluginInterface", e.getMessage());
            throw e;
        }
    }

    private Map getSubConfigurationValues(String name)
        throws SMSException, SSOException {
        ServiceConfigManager scm = new ServiceConfigManager(
            TEST_SERVICE, getAdminSSOToken());
        ServiceConfig sc = scm.getGlobalConfig(null);
        sc = sc.getSubConfig("testConfig");
        return sc.getAttributes();
    }

    @Test(groups = {"schema", "set-svc-i18n-key"})
    public void setServiceI18nKey() 
        throws CLIException, SMSException, SSOException {
        entering("setServiceI18nKey", null);
        String[] args = {"set-svc-i18n-key", 
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            TEST_SERVICE,
            CLIConstants.PREFIX_ARGUMENT_LONG + 
                SetServiceSchemaI18nKey.ARGUMENT_I18N_KEY,
            "service-18nKey"
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            assert mgr.getI18NKey().equals("service-18nKey");
            exiting("setServiceI18nKey");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setServiceI18nKey", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setServiceI18nKey", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setServiceI18nKey", e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"schema", "set-svc-view-bean-url"})
    public void setServiceViewBeanURL()
        throws CLIException, SMSException, SSOException {
        entering("setServiceViewBeanURL", null);
        String[] args = {"set-svc-view-bean-url",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            TEST_SERVICE,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                SetServiceSchemaPropertiesViewBeanURL.ARGUMENT_URL,
            "mockviewbeanURL"
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            assert mgr.getPropertiesViewBeanURL().equals("mockviewbeanURL");
            exiting("setServiceViewBeanURL");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setServiceViewBeanURL", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setServiceViewBeanURL", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setServiceViewBeanURL", e.getMessage());
            throw e;
        }
    }

    private void updateService() 
        throws CLIException, SMSException, SSOException {
        entering("updateService", null);
        String[] args = {"update-svc", 
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.XML_FILE,
            TEST_SERVICE_XML
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            assert serviceExists(TEST_SERVICE);
            exiting("updateService");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "updateService", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "updateService", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "updateService", e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"schema", "add-sub-schema"})
    public void addSubSchema()
        throws CLIException, SMSException, SSOException {
        entering("addSubSchema", null);
        String[] args = {"add-sub-schema", 
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            TEST_SERVICE,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE,
            "global",
            CLIConstants.PREFIX_ARGUMENT_LONG + AddSubSchema.ARGUMENT_FILENAME,
            MOCK_DIR + "/subschema.xml"
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema globalSchema = mgr.getSchema(SchemaType.GLOBAL);
            ServiceSchema s = globalSchema.getSubSchema("subschema");
            assert (s != null);
            exiting("addSubSchema");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "addSubSchema", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "addSubSchema", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "addSubSchema", e.getMessage());
            throw e;
        }
    }
    
    @Test(groups = {"schema", "remove-sub-schema"}, 
        dependsOnMethods = {"addSubSchema"})
    public void removeSubSchema()
        throws CLIException, SMSException, SSOException {
        entering("removeSubSchema", null);
        String[] args = {"remove-sub-schema", 
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            TEST_SERVICE,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE,
            "global",
            CLIConstants.PREFIX_ARGUMENT_LONG + 
                RemoveSubSchema.ARGUMENT_SCHEMA_NAMES,
            "subschema"
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema globalSchema = mgr.getSchema(SchemaType.GLOBAL);
            ServiceSchema s = globalSchema.getSubSchema("subschema");
            assert (s == null);
            exiting("removeSubSchema");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "removeSubSchema", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "removeSubSchema", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "removeSubSchema", e.getMessage());
            throw e;
        }
    }
    
    @Test(groups = {"schema", "set-revision-number", "get-revision-number"})
    public void setGetServiceRevisionNumber()
        throws CLIException, SMSException, SSOException {
        entering("setGetServiceRevisionNumber", null);
        String[] args = {"set-revision-number", 
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            TEST_SERVICE,
            CLIConstants.PREFIX_ARGUMENT_LONG + 
                SetServiceRevisionNumber.ARGUMENT_VERSION,
            "20"
        };
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        String[] arg1s = {"get-revision-number", 
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            TEST_SERVICE
        };
        req = new CLIRequest(null, arg1s, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            assert (mgr.getRevisionNumber() == 20);
            exiting("setGetServiceRevisionNumber");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setGetServiceRevisionNumber",
                e.getMessage());
            throw e;
        }
    }

    @Parameters({"subschema"})
    @Test(groups = {"schema", "add-attrs", "subschema"})
    public void addAttributeSchema(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("addAttributeSchema", params);

        String[] args = (subschema.length() > 0) ?
            new String[9] : new String[7];

        args[0] = "add-attrs";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            AddAttributeSchema.ARGUMENT_SCHEMA_FILES;
        args[6] = MOCK_DIR + "/addAttributeSchema.xml";

        if (subschema.length() > 0) {
            args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[8] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
            assert (as != null);
            exiting("addAttributeSchema");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "addAttributeSchema", e.getMessage(),
                params);
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "addAttributeSchema", e.getMessage(),
                params);
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "addAttributeSchema", e.getMessage(),
                params);
            throw e;
        }
    }

    @Parameters({"subschema"})
    @Test(groups = {"schema", "add-attr-defs", "attribute-schema-ops",
        "subschema"},
        dependsOnMethods = {"addAttributeSchema"}
    )
    public void addAttributeDefaultValues(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("addAttributeDefaultValues", params);
        String[] args = (subschema.length() == 0) 
            ? new String[7] : new String[9];
        args[0] = "add-attr-defs";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.ATTRIBUTE_VALUES;
        args[6] = "mock-add=test1";

        if (subschema.length() > 0) {
            args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[8] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        ServiceSchemaManager mgr = new ServiceSchemaManager(
            TEST_SERVICE, getAdminSSOToken());
        ServiceSchema serviceSchema = mgr.getGlobalSchema();

        if (subschema.length() > 0) {
            serviceSchema = serviceSchema.getSubSchema(subschema);
        }
            
        AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
        Set values = as.getDefaultValues();
        assert (values.size() == 1);
        assert (values.contains("test1"));
        exiting("addAttributeDefaultValues");
    }

    @Parameters({"subschema"})
    @Test(groups = {"schema", "add-attribute-default-values", "attribute-schema-ops",
        "subschema"},
        dependsOnMethods = {"showAttributeDefaultValues"}
    )
    public void deleteAttributeDefaultValues(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("deleteAttributeDefaultValues", params);
        String[] args = (subschema.length() == 0) 
            ? new String[9] : new String[11];
        args[0] = "delete-attr-def-values";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_SCHEMA;
        args[6] = "mock-add";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.DEFAULT_VALUES;
        args[8] = "test1";

        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        ServiceSchemaManager mgr = new ServiceSchemaManager(
            TEST_SERVICE, getAdminSSOToken());
        ServiceSchema serviceSchema = mgr.getGlobalSchema();

        if (subschema.length() > 0) {
            serviceSchema = serviceSchema.getSubSchema(subschema);
        }
            
        AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
        Set values = as.getDefaultValues();
        assert (!values.contains("test1"));
        exiting("deleteAttributeDefaultValues");
    }
    
    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-defs", "attribute-schema-ops",
        "subschema"},
        dependsOnMethods = {"deleteAttributeDefaultValues"}
    )
    public void setAttributeDefaults(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("setAttributeDefaults", params);

        SSOToken adminSSOToken = getAdminSSOToken();
        ServiceSchemaManager mgr = new ServiceSchemaManager(
            TEST_SERVICE, adminSSOToken);
        ServiceSchema serviceSchema = mgr.getGlobalSchema();
        if (subschema.length() > 0) {
            serviceSchema = serviceSchema.getSubSchema(subschema);
        }
        AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
        as.addChoiceValue("testx", "testx");
        //as.addChoiceValue("testy", "testy");
        
        String[] args = (subschema.length() == 0) 
            ? new String[7] : new String[9];
        args[0] = "set-attr-defs";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.ATTRIBUTE_VALUES;
        args[6] = "mock-add=testx";
        //args[7] = "mock-add=testy";

        if (subschema.length() > 0) {
            args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[8] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        mgr = new ServiceSchemaManager(TEST_SERVICE, adminSSOToken);
        serviceSchema = mgr.getGlobalSchema();
        if (subschema.length() > 0) {
            serviceSchema = serviceSchema.getSubSchema(subschema);
        }
            
        as = serviceSchema.getAttributeSchema("mock-add");
        Set defaultValues = as.getDefaultValues();
        assert (defaultValues.size() == 1);
        assert (defaultValues.contains("testx"));
        //assert (defaultValues.contains("testy"));
        exiting("setAttributeDefaults");
    }
    
    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-view-bean-url",
        "attribute-schema-ops", "subschema"},
        dependsOnMethods = {"addAttributeSchema"}
    )
    public void setAttributeViewBeanURL(String subschema)
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        
        entering("setAttributeViewBeanURL", params);
        String[] args = (subschema.length() == 0)
            ? new String[9] : new String[11];
        
        args[0] = "set-attr-view-bean-url";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.ATTRIBUTE_SCHEMA;
        args[6] = "mock-add";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ModifyAttributeSchemaPropertiesViewBeanURL.ARGUMENT_URL;
        args[8] = "mockattributeURL";

        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }
            
            AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
            assert (as.getPropertiesViewBeanURL().equals("mockattributeURL"));
            exiting("setAttributeViewBeanURL");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setAttributeViewBeanURL", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setAttributeViewBeanURL", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setAttributeViewBeanURL", e.getMessage());
            throw e;
        }
    }

    
    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-any", "attribute-schema-ops",
        "subschema"},
        dependsOnMethods = {"addAttributeSchema"}
    )
    public void setAttributeAny(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("setAttributeAny", params);
        String[] args = (subschema.length() == 0)
            ? new String[9] : new String[11];

        args[0] = "set-attr-any";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.ATTRIBUTE_SCHEMA;
        args[6] = "mock-add";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ModifyAttributeSchemaAny.ARGUMENT_ANY;
        args[8] = "admin";

        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }
        
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();
            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }
            AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
            assert (as.getAny().equals("admin"));
            exiting("setAttributeAny");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setAttributeAny", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setAttributeAny", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setAttributeAny", e.getMessage());
            throw e;
        }
    }
    
    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-i18n-key", "attribute-schema-ops",
        "subschema"},
        dependsOnMethods = {"addAttributeSchema"}
    )
    public void setAttributeI18nKey(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("setAttributeI18nKey", params);
        String[] args = (subschema.length() == 0)
            ? new String[9] : new String[11];

        args[0] = "set-attr-i18n-key";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.ATTRIBUTE_SCHEMA;
        args[6] = "mock-add";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ModifyAttributeSchemaI18nKey.ARGUMENT_I18N_KEY;
        args[8] = "123";

        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }
        
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();
            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }
            AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
            assert (as.getI18NKey().equals("123"));
            exiting("setAttributeI18nKey");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setAttributeI18nKey", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setAttributeI18nKey", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setAttributeI18nKey", e.getMessage());
            throw e;
        }
    }

    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-type", "attribute-schema-ops",
        "subschema"},
        dependsOnMethods = {"addAttributeSchema", "showAttributeDefaultValues"}
    )
    public void setAttributeSchemaType(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("setAttributeSchemaType", params);
        String[] args = (subschema.length() == 0)
            ? new String[9] : new String[11];
        
        args[0] = "set-attr-type";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_SCHEMA;
        args[6] = "mock-add";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ModifyAttributeSchemaType.ARGUMENT_TYPE;
        args[8] = "multiple_choice";

        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
            assert (as.getType().equals(AttributeSchema.Type.MULTIPLE_CHOICE));
            exiting("setAttributeSchemaType");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setAttributeSchemaType", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setAttributeSchemaType", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setAttributeSchemaType", e.getMessage());
            throw e;
        }
    }

    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-syntax", "attribute-schema-ops",
        "subschema"},
        dependsOnMethods = {"addAttributeSchema", "showAttributeDefaultValues"}
    )
    public void setAttributeSchemaSyntax(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("setAttributeSchemaSyntax", params);
        String[] args = (subschema.length() == 0)
            ? new String[9] : new String[11];

        args[0] = "set-attr-syntax";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.ATTRIBUTE_SCHEMA;
        args[6] = "mock-add";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ModifyAttributeSchemaSyntax.ARGUMENT_SYNTAX;
        args[8] = "paragraph";
    
        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }
        
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
            assert (as.getSyntax().equals(AttributeSchema.Syntax.PARAGRAPH));
            exiting("setAttributeSchemaSyntax");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setAttributeSchemaSyntax", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setAttributeSchemaSyntax", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setAttributeSchemaSyntax", e.getMessage());
            throw e;
        }
    }

    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-ui-type", "attribute-schema-ops",
        "subschema"},
        dependsOnMethods = {"addAttributeSchema", "showAttributeDefaultValues"}
    )
    public void setAttributeSchemaUIType(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("setAttributeSchemaUIType", params);
        String[] args = (subschema.length() == 0)
            ? new String[9] : new String[11];
        
        args[0] = "set-attr-ui-type";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.ATTRIBUTE_SCHEMA;
        args[6] = "mock-add";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ModifyAttributeSchemaUIType.ARGUMENT_UI_TYPE;
        args[8] = "button";

        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
            assert (as.getUIType().equals(AttributeSchema.UIType.BUTTON));
            exiting("setAttributeSchemaUIType");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setAttributeSchemaUIType", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setAttributeSchemaUIType", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setAttributeSchemaUIType", e.getMessage());
            throw e;
        }
    }

    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-start-range", "subschema"})
    public void setAttributeSchemaStartRange(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("setAttributeSchemaStartRange", params);
        String[] args = (subschema.length() == 0)
            ? new String[9] : new String[11];

        args[0] = "set-attr-start-range";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.ATTRIBUTE_SCHEMA;
        args[6] = "mock-number";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            SetAttributeSchemaStartRange.ARGUMENT_RANGE;
        args[8] = "10";

        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }
        
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = 
                serviceSchema.getAttributeSchema("mock-number");
            assert as.getStartRange().equals("10");
            exiting("setAttributeSchemaStartRange");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setAttributeSchemaStartRange",
                e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setAttributeSchemaStartRange",
                e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setAttributeSchemaStartRange",
                e.getMessage());
            throw e;
        }
    }

    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-end-range", "subschema"})
    public void setAttributeSchemaEndRange(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("setAttributeSchemaEndRange", params);
        String[] args = (subschema.length() == 0)
            ? new String[9] : new String[11];
        
        args[0] = "set-attr-end-range";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.ATTRIBUTE_SCHEMA;
        args[6] = "mock-number";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            SetAttributeSchemaEndRange.ARGUMENT_RANGE;
        args[8] = "100";

        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = serviceSchema.getAttributeSchema(
                "mock-number");
            assert as.getEndRange().equals("100");
            exiting("setAttributeSchemaEndRange");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setAttributeSchemaEndRange",
                e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setAttributeSchemaEndRange",
                e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setAttributeSchemaEndRange",
                e.getMessage());
            throw e;
        }
    }
    
    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-validator",
        "attribute-schema-ops", "subschema"})
    public void setAttributeSchemaValidator(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("setAttributeSchemaValidator", params);
        String[] args = (subschema.length() == 0)
            ? new String[9] : new String[11];

        args[0] = "set-attr-validator";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.ATTRIBUTE_SCHEMA;
        args[6] = "mock-add";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            SetAttributeSchemaValidator.ARGUMENT_VALIDATOR;
        args[8] = "com.dummy.Dummy";

        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
            /*
             * unable to verify because AttributeSchema does not have 
             * getValidator method. Hence pass if there are no exceptions.
             */
            exiting("setAttributeSchemaValidator");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setAttributeSchemaValidator",
                e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setAttributeSchemaValidator",
                e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setAttributeSchemaValidator",
                e.getMessage());
            throw e;
        }
    }
    
    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-choicevals",
        "attribute-schema-ops", "subschema"})
    public void setAttributeSchemaChoiceValues(String subschema)
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("setAttributeSchemaChoiceValues", params);
        String[] args = (subschema.length() == 0)
            ? new String[9] : new String[11];

        args[0] = "set-attr-choicevals";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_NAME;
        args[6] = "mock-single-choice";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.CHOICE_VALUES;
        args[8] = "i18nKey1=choice1";

        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = serviceSchema.getAttributeSchema(
                "mock-single-choice");
            String[] choiceValues = as.getChoiceValues();
            assert (choiceValues.length == 1);
            assert choiceValues[0].equals("choice1");
            String i18nKey = as.getChoiceValueI18NKey("choice1");
            assert i18nKey.equals("i18nKey1");
            exiting("setAttributeSchemaChoiceValues");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setAttributeSchemaChoiceValues",
                e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setAttributeSchemaChoiceValues",
                e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setAttributeSchemaChoiceValues",
                e.getMessage());
            throw e;
        }
    }

    @Parameters({"subschema"})
    @Test(groups = {"schema", "remove-attr-choicevals",
        "attribute-schema-ops", "subschema"},
        dependsOnMethods = {"setAttributeSchemaChoiceValues"})
    public void removeAttributeSchemaChoiceValues(String subschema)
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("removeAttributeSchemaChoiceValues", params);
        String[] args = (subschema.length() == 0)
            ? new String[9] : new String[11];

        args[0] = "remove-attr-choicevals";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_NAME;
        args[6] = "mock-single-choice";
        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.CHOICE_VALUES;
        args[8] = "choice1";
        
        if (subschema.length() > 0) {
            args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[10] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = serviceSchema.getAttributeSchema(
                "mock-single-choice");
            String[] choiceValues = as.getChoiceValues();
            assert (choiceValues.length == 0);
            exiting("removeAttributeSchemaChoiceValues");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "removeAttributeSchemaChoiceValues",
                e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "removeAttributeSchemaChoiceValues",
                e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "removeAttributeSchemaChoiceValues",
                e.getMessage());
            throw e;
        }
    }

    @Parameters({"subschema"})
    @Test(groups = {"schema", "set-attr-bool-values", "subschema"})
    public void setAttributeSchemaBooleanValues(String subschema)
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("setAttributeSchemaBooleanValues", params);
        String[] args = (subschema.length() == 0)
            ? new String[15] : new String[17];

        args[0]= "set-attr-bool-values";
        args[1]= CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2]= TEST_SERVICE;
        args[3]= CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4]= "global";
        args[5]= CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_NAME;
        args[6]= "mock-boolean";
        args[7]= CLIConstants.PREFIX_ARGUMENT_LONG +
            SetAttributeSchemaBooleanValues.ARGUMENT_TRUE_VALUE;
        args[8]= "true";
        args[9]= CLIConstants.PREFIX_ARGUMENT_LONG +
            SetAttributeSchemaBooleanValues.ARGUMENT_TRUE_I18N_KEY;
        args[10]= "truei18nKey";
        args[11]= CLIConstants.PREFIX_ARGUMENT_LONG +
            SetAttributeSchemaBooleanValues.ARGUMENT_FALSE_VALUE;
        args[12]= "false";
        args[13]= CLIConstants.PREFIX_ARGUMENT_LONG +
            SetAttributeSchemaBooleanValues.ARGUMENT_FALSE_I18N_KEY;
        args[14]= "falsei18nKey";
        
        if (subschema.length() > 0) {
            args[15] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[16] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);

        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();
            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = serviceSchema.getAttributeSchema(
                "mock-boolean");
            assert (as.getTrueValue().equals("true"));
            assert (as.getTrueValueI18NKey().equals("truei18nKey"));
            assert (as.getFalseValue().equals("false"));
            assert (as.getFalseValueI18NKey().equals("falsei18nKey"));
            exiting("setAttributeSchemaBooleanValues");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "setAttributeSchemaBooleanValues",
                e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "setAttributeSchemaBooleanValues",
                e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "setAttributeSchemaBooleanValues",
                e.getMessage());
            throw e;
        }
    }
    
    @Parameters({"subschema"})
    @Test(groups = {"schema", "delete-attr", "subschema"},
        dependsOnGroups = {"attribute-schema-ops"}
    )
    public void deleteAttributeSchema(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("deleteAttributeSchema", params);
        String[] args = (subschema.length() == 0)
            ? new String[7] : new String[9];

        args[0] = "delete-attr";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +IArgument.ATTRIBUTE_SCHEMA;
        args[6] = "mock-add";
        
        if (subschema.length() > 0) {
            args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[8] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
            assert (as == null);
            exiting("deleteAttributeSchema");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "deleteAttributeSchema", e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "deleteAttributeSchema", e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "deleteAttributeSchema", e.getMessage());
            throw e;
        }
    }
    
    @Parameters({"subschema"})
    @Test(groups = {"schema", "show-attr-defs",
        "attribute-schema-ops", "subschema"},
        dependsOnMethods = {"addAttributeDefaultValues"}
    )
    public void showAttributeDefaultValues(String subschema) 
        throws CLIException {
        Object[] params = {subschema};
        entering("showAttributeDefaultValues", params);
        String[] args = (subschema.length() == 0)
            ? new String[7] : new String[9];

        args[0] = "get-attr-defs";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_NAMES;
        args[6] = "mock-add";

        if (subschema.length() > 0) {
            args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[8] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            String messages = outputWriter.getMessages();
            assert (messages.indexOf("mock-add=test1") != -1);
        } catch (CLIException e) {
            this.log(Level.SEVERE, "showAttributeDefaultValues",e.getMessage());
            throw e;
        }
    }

    @Parameters({"subschema"})
    @Test(groups = {"schema", "remove-attr-defs", 
            "attribute-schema-ops", "subschema"},
        dependsOnMethods = {"showAttributeDefaultValues"}
    )
    public void removeAttributeDefaultValues(String subschema) 
        throws CLIException, SMSException, SSOException {
        Object[] params = {subschema};
        entering("removeAttributeDefaultValues", params);
        String[] args = (subschema.length() == 0)
            ? new String[7] : new String[9];

        args[0] = "remove-attr-defs";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        args[2] = TEST_SERVICE;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SCHEMA_TYPE;
        args[4] = "global";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_NAMES;
        args[6] = "mock-add";

        if (subschema.length() > 0) {
            args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.SUBSCHEMA_NAME;
            args[8] = subschema;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        
        try {
            cmdManager.serviceRequestQueue();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                TEST_SERVICE, getAdminSSOToken());
            ServiceSchema serviceSchema = mgr.getGlobalSchema();

            if (subschema.length() > 0) {
                serviceSchema = serviceSchema.getSubSchema(subschema);
            }

            AttributeSchema as = serviceSchema.getAttributeSchema("mock-add");
            Set values = as.getDefaultValues();
            assert values.isEmpty();
            exiting("removeAttributeDefaultValues");
        } catch (CLIException e) {
            this.log(Level.SEVERE, "removeAttributeDefaultValues",
                e.getMessage());
            throw e;
        } catch (SMSException e) {
            this.log(Level.SEVERE, "removeAttributeDefaultValues",
                e.getMessage());
            throw e;
        } catch (SSOException e) {
            this.log(Level.SEVERE, "removeAttributeDefaultValues",
                e.getMessage());
            throw e;
        }
    }
     
    private void createServices(List<String> xmlFileNames)
        throws CLIException {
        String[] args = new String[xmlFileNames.size() +2];
        args[0] = "create-svc";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.XML_FILE;
        int cnt = 2;
        for (String xml : xmlFileNames) {
            args[cnt++] = xml;
        }
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
    }

    private void deleteServices(List<String> serviceNames) throws CLIException {
        String[] args = new String[serviceNames.size() +2];
        args[0] = "delete-svc";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME;
        int cnt = 2;
        for (String xml : serviceNames) {
            args[cnt++] = xml;
        }
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
    }

    private boolean serviceExists(String serviceName) throws SMSException, SSOException {
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(serviceName, getAdminSSOToken());
            return true;
        } catch (ServiceNotFoundException snfe) {
            return false;
        }
    }
}
