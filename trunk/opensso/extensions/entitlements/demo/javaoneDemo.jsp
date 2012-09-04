<%@ page import="java.security.AccessController" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.util.*" %>
<%@ page import="javax.security.auth.Subject" %>
<%@ page import="com.iplanet.sso.SSOToken" %>
<%@ page import="com.sun.identity.authentication.internal.server.AuthSPrincipal" %>
<%@ page import="com.sun.identity.entitlement.*" %>
<%@ page import="com.sun.identity.entitlement.util.*" %>
<%@ page import="com.sun.identity.entitlement.opensso.*" %>
<%@ page import="com.sun.identity.security.AdminTokenAction" %>


<%
   String APPL_NAME = "javaoneDemo";

   SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
       AdminTokenAction.getInstance());
   Subject adminSubject = SubjectUtils.createSubject(adminToken);
   ApplicationType appType = ApplicationTypeManager.getAppplicationType(adminSubject, 
       ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
   Application appl = new Application("/", APPL_NAME, appType);
   Set<String> resources = new HashSet<String>();
   resources.add("https://*");
   resources.add("http://*");
   appl.addResources(resources);
   appl.setEntitlementCombiner(DenyOverride.class);
   ApplicationManager.saveApplication(adminSubject, "/", appl);

   Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
       actionValues.put("GET", Boolean.TRUE);
   EntitlementSubject authenticatedSubject = new
       AuthenticatedESubject();

   //Create self privilege - can get info on your own phone
   PrivilegeManager prm = PrivilegeManager.getInstance("/", adminSubject );
   boolean javademoSelfExist = false;

   	try {
       javademoSelfExist = (prm.getPrivilege("javademoSelf") != null);
   	} catch (Exception e) {
		out.println("<p>"+e.getMessage()+"</p>");
   	}
    if (javademoSelfExist) {
   	   	out.println("<p>Deleting old self privilege</p>");
   	   	prm.removePrivilege("javademoSelf");
   	   	out.println("<p>Deleted old self privilege</p>");
    }

	out.println("<p>Creating new self privilege</p>");
    String resourceName =
        "http://localhost:8080/C1DemoServer/resources/phones/$SELF*";
    Entitlement entitlement = new Entitlement("javaoneDemo",
        resourceName, actionValues);
   	entitlement.setName("ent1");

    Privilege privilege = new OpenSSOPrivilege("javademoSelf",
        entitlement, authenticatedSubject, null, null);
    prm.addPrivilege(privilege);

	out.println("<p>Created new self privilege</p>");

    // Create account privilege - head of household can get and set info on the 
    // account
    boolean javademoAccountExist = false;

   	try {
       	javademoAccountExist = (prm.getPrivilege("javademoAccount") != null);
   	} catch (Exception e) {
		out.println("<p>"+e.getMessage()+"</p>");
   	}
    if (javademoAccountExist) {
   	   	out.println("<p>Deleting old account privilege</p>");
   	   	prm.removePrivilege("javademoAccount");
   	   	out.println("<p>Deleted old account privilege</p>");
    }
    
	out.println("<p>Creating new account privilege</p>");
    resourceName =
        "http://localhost:8080/C1DemoServer/resources/accounts/*";
        
    actionValues.put("POST", Boolean.TRUE);
        
    entitlement = new Entitlement("javaoneDemo",
        resourceName, actionValues);
    entitlement.setName("ent2");

    EntitlementCondition condition = new JavaOneDemoAccountCondition();

    privilege = new OpenSSOPrivilege("javademoAccount",
        entitlement, authenticatedSubject, condition, null);
    prm.addPrivilege(privilege);
   
	out.println("<p>Created new account privilege</p>");

    // Create phone privilege - head of household can get and set info on all 
    // phones on the account
    boolean javademoPhoneExist = false;

   	try {
       	javademoPhoneExist = (prm.getPrivilege("javademoPhone") != null);
   	} catch (Exception e) {
		out.println("<p>"+e.getMessage()+"</p>");
   	}
    if (javademoPhoneExist) {
   	   	out.println("<p>Deleting old phone privilege</p>");
   	   	prm.removePrivilege("javademoPhone");
   	   	out.println("<p>Deleted old phone privilege</p>");
    }
    
	out.println("<p>Creating new phone privilege</p>");
    resourceName =
        "http://localhost:8080/C1DemoServer/resources/phones/*";
        
    entitlement = new Entitlement("javaoneDemo",
        resourceName, actionValues);
    entitlement.setName("ent3");

    condition = new JavaOneDemoCondition();

    privilege = new OpenSSOPrivilege("javademoPhone",
        entitlement, authenticatedSubject, condition, null);
    prm.addPrivilege(privilege);
   
	out.println("<p>Created new phone privilege</p>");

    out.println("<p>Done</p>");
%>
