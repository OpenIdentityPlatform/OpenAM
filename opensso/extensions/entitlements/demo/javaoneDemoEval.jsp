<%@ page import="java.security.AccessController" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.Set" %>
<%@ page import="javax.security.auth.Subject" %>
<%@ page import="com.iplanet.sso.SSOToken" %>
<%@ page import="com.sun.identity.authentication.internal.server.AuthSPrincipal" %>
<%@ page import="com.sun.identity.entitlement.*" %>
<%@ page import="com.sun.identity.entitlement.opensso.SubjectUtils" %>
<%@ page import="com.sun.identity.security.AdminTokenAction" %>

<html>
<body>
<table>
<tr>
<th>Subject</th><th>Action</th><th>Resource</th><th>Allow/Deny</th>
</tr>
<%
Subject ADMIN_SUBJECT =
   SubjectUtils.createSubject((SSOToken)AccessController.doPrivileged(AdminTokenAction.getInstance()));

String subjects[]  = {"1234567890","1112223333"};
String resources[] = {"1234567890","1112223333"};
String realm = "/";
String actions[] = {"GET","POST"};

try {
	for ( int i = 0; i < subjects.length; i++ ) {
		for ( int j = 0; j < actions.length; j++ ) {
			for ( int k = 0; k < resources.length; k++ ) {
				for ( int l = 0; l < 2; l++ ) {
%>
	<tr>
<%
					String subject = "id="+subjects[i]+",ou=user,dc=opensso,dc=java,dc=net";
					String resource = "http://localhost:8080/C1DemoServer/resources/phones/"+resources[k];
					if (l == 1) {
						resource = resource + "/?param=value";
					}
				    HashSet set = new HashSet<String>();
				    set.add(actions[j]);
				    Entitlement ent = new Entitlement(resource, set);
				
				    Set<Principal> principals = new HashSet<Principal>();
				    principals.add(new AuthSPrincipal(subject));
				    Subject sbj = new Subject(false, principals, new HashSet(), new HashSet());
				    boolean allow = new Evaluator(ADMIN_SUBJECT).hasEntitlement(realm,
				       	sbj, ent, null);
				    out.println("<td>" + subjects[i] + "</td><td>" + actions[j] + "</td><td>" + resource + "</td><td style=\"color:" + (allow ? "green" : "red") + "\">" + (allow ? "allowed" : "denied")+"</td>");
%>
	</tr>
<%
				}
			}
		}
	}
} catch (EntitlementException ee) {
   out.println(ee.getMessage());
   ee.printStackTrace();
}
    
try {
	for ( int i = 0; i < subjects.length; i++ ) {
		for ( int j = 0; j < actions.length; j++ ) {
			for ( int k = 0; k < 2; k++ ) {
%>
	<tr>
<%
				String subject = "id="+subjects[i]+",ou=user,dc=opensso,dc=java,dc=net";
				String resource = "http://localhost:8080/C1DemoServer/resources/accounts/123456789012345";
				if (k == 1) {
					resource = resource + "/?param=value";
				}
			    HashSet set = new HashSet<String>();
			    set.add(actions[j]);
			    Entitlement ent = new Entitlement(resource, set);
			
			    Set<Principal> principals = new HashSet<Principal>();
			    principals.add(new AuthSPrincipal(subject));
			    Subject sbj = new Subject(false, principals, new HashSet(), new HashSet());
			    boolean allow = new Evaluator(ADMIN_SUBJECT).hasEntitlement(realm,
			       	sbj, ent, null);
			    out.println("<td>" + subjects[i] + "</td><td>" + actions[j] + "</td><td>" + resource + "</td><td style=\"color:" + (allow ? "green" : "red") + "\">" + (allow ? "allowed" : "denied")+"</td>");
%>
	</tr>
<%
			}
		}
	}
} catch (EntitlementException ee) {
   out.println(ee.getMessage());
   ee.printStackTrace();
}        
%>
</table>
</body>
</html>