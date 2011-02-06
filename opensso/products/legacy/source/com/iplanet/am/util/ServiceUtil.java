/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ServiceUtil.java,v 1.4 2009/11/20 23:52:57 ww203982 Exp $
 *
 */

package com.iplanet.am.util;

import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.DataLayer;
import com.iplanet.ums.SearchTemplate;
import com.iplanet.ums.TemplateManager;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResourceManager;
import com.sun.identity.policy.Rule;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.LDAPModificationSet;
import com.sun.identity.shared.ldap.LDAPSearchConstraints;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPv2;
import com.sun.identity.shared.ldap.LDAPRequestParser;
import com.sun.identity.shared.ldap.LDAPDeleteRequest;
import com.sun.identity.shared.ldap.LDAPModifyRequest;
import com.sun.identity.shared.ldap.LDAPSearchRequest;


/**
 * The <code>ServiceUtil</code> class provides methods to
 * delete all the entry's related to a service.
 * The methods in this class works directly with 
 * the backend datastore (usually a directory server).
 */
public class ServiceUtil {

    private static SSOToken ssoToken; 
    private static String baseDN;
    private static Principal connPrincipal; 
    private static DataLayer dlayer;
    private static Debug debug;


    /** Gets the connection to the DSAME datastore if the Session is valid.
     * @param token a valid SSOToken.
     * @param debugInst a valid Debug instance.
     */
    public ServiceUtil(SSOToken token, Debug debugInst) {
        this.ssoToken = token;
	debug = debugInst;
	try {
	    ServerInstance serverInstance = null;
            DSConfigMgr mgr = DSConfigMgr.getDSConfigMgr();
	    if (mgr != null) {
	        serverInstance = mgr.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            }
            if (serverInstance != null) {
		dlayer = DataLayer.getInstance(serverInstance);
		baseDN = serverInstance.getBaseDN();
            }
	    connPrincipal = ssoToken.getPrincipal();
	    if (dlayer == null || baseDN == null || connPrincipal == null) {
		debug.error("Service Util init failed");
            }
	    debug.message("ServiceUtil init successfull");
        } catch (Exception e) {
	    if (debug != null) {
	       debug.error("Service Util init failed", e);
            }
	}
    }

    /** Deletes all the entrys related to a service from the data store.
     * @param serviceName name of the service whose entry should be 
     * deleted from the data store.
     * @param userAtt if true deletes all user config related to the service
     * if false will not delete user config.
     * @param version this is the version of the service to be deleted.
     * @return returns true if successfull, else returns false.
     */
    public boolean deleteService(String serviceName, 
				 boolean userAtt, String version) {
	ServiceManager sm = null;
	ServiceSchemaManager scm = null;
	try {
	  
            sm = new ServiceManager(ssoToken);
            scm = sm.getSchemaManager(serviceName, version);

            
            // delete every thing related org schema

            if(scm.getOrganizationSchema() != null) { 
	        debug.message("Processing Organization Schema");
	        deleteOrgConfig(serviceName);
            } 

            // delete every thing related to user schema 
	    if(scm.getUserSchema() != null) {
		debug.message("Processing User Schema");
	        if(userAtt) { 
                    ServiceSchema userSchema = scm.getUserSchema();
	               if (userSchema  != null) {
                          Set schemanameattr = 
				  userSchema.getAttributeSchemaNames();
		          deleteUserServiceAttributes(schemanameattr, 
						      serviceName);
                       } 

                }
            }

	    // delete every thing related to dynamic schema 
	    if(scm.getDynamicSchema() != null) {
		debug.message("Processing Dyanmic Schema");
		deleteCos(serviceName); 
                if(userAtt) {
                    //Get the default service value from the global schema
	            ServiceSchema globalschema = scm.getGlobalSchema();
		    if (globalschema != null) {
	                Map defaultvalues = globalschema.getAttributeDefaults();
	                Set set = (Set)defaultvalues.get("serviceObjectClasses");
			if (set.size() > 0) {
	                    Iterator itrvalues = set.iterator();
                            String serviceobjectclass = 
						itrvalues.next().toString(); 
		            deleteUserObjectClass(serviceobjectclass,
						serviceName); 
                        } else {
			    debug.error("ERROR deleting  user objectclass");
                           
                        }
                    }
                }
            }  

            // delete every thing releated to Policy schema
            if (scm.getPolicySchema() != null) {
		debug.message("Processing Policy Schema");
	        deletePolicies(serviceName);
            }

            // delete ServiceType from org serviceStatus
	    deleteOrgServiceStatus(serviceName);   

	    return true;

    	} catch (Exception e) {
	    debug.error("ERROR : while deleting service " + serviceName, e);
	    return false;
	}
    }



    private void deleteCos(String serviceName) {
       
	String searchfilter = "&(objectclass=cosclassicdefinition)" +
			    "(objectclass=ldapsubentry)(cn=" + serviceName + ")";
	try {
	    LDAPSearchResults  cosdefs = 
		  searchResults(baseDN,searchfilter, LDAPv2.SCOPE_SUB, null);
	    while (cosdefs.hasMoreElements()) {
	        LDAPEntry cosdefentry = cosdefs.next();
                String cosdefdn = cosdefentry.getDN().toString();

		LDAPSearchResults  costemplates = 
		    searchResults(cosdefdn,"(objectclass=costemplate)",
			                        LDAPv2.SCOPE_SUB, null);

                while (costemplates.hasMoreElements()) {
		    LDAPEntry costempentry = costemplates.next();
		    String costempdn = costempentry.getDN();
		    delete(costempdn); 
                }

                delete(cosdefdn);

            }
    	} catch (Exception e) {
	    debug.error("ERROR : while deleting Cos for service " + 
			      serviceName, e);
	}
    }


    private void deletePolicies(String serviceName) {

        String searchfilter = getObjectSearchFilter(AMObject.ORGANIZATION);
	if (searchfilter == null) {
	    debug.error("searchfilter is null for AMObject.ORGANIZATION");
        }
        try {
           LDAPSearchResults  orgs = 
	       searchResults(baseDN, searchfilter, LDAPv2.SCOPE_SUB, null);
           PolicyManager pm = null; 
	   while (orgs.hasMoreElements()) {
	       LDAPEntry orgentry = orgs.next();
	       String orgdn = orgentry.getDN().toString();
	       pm = new PolicyManager(ssoToken, orgdn);
	       ResourceManager rm = pm.getResourceManager();
               if(rm.getManagedResourceNames().size() > 0 ||
				   LDAPDN.equals(orgdn, baseDN)) {
	           Set policys = pm.getPolicyNames();
                   Iterator itrpolicys = policys.iterator();

	           while(itrpolicys.hasNext()) {
	               String policyName = itrpolicys.next().toString();
		       Policy policy = pm.getPolicy(policyName);
                       Set rules = policy.getRuleNames();
                       Iterator itrrules = rules.iterator();

		       while(itrrules.hasNext()) {
		           String ruleName = itrrules.next().toString();
	                   Rule rule = policy.getRule(ruleName);
	                   String serviceTypeName = rule.getServiceTypeName(); 
	                   if(serviceTypeName.equalsIgnoreCase(serviceName)) {
		               Rule ruleRemoved = policy.removeRule(ruleName);
		               if(ruleRemoved != null) {
		                   pm.replacePolicy(policy);   
                               }else {
                               }
                           }
                       } 
                   } 
               } else {
	           Set policys = pm.getPolicyNames();
                   Iterator itrpolicys = policys.iterator();

	           while(itrpolicys.hasNext()) {
	               String policyName = itrpolicys.next().toString();
		       pm.removePolicy(policyName);
                   }
               } 
          	
            }
        }catch (Exception e) {
	    debug.error("ERROR : while deleting policys for service " + 
			     serviceName, e);
        }
    }                   


    private void deleteUserObjectClass(String attrValue, String serviceName) {
       
	String userSearchFilter = getObjectSearchFilter(AMObject.USER);
	String searchfilter = "&(" + userSearchFilter + ")" +
			       "(objectclass=" + attrValue + ")";
	try {
	    LDAPSearchResults  users = 
		searchResults(baseDN, searchfilter, LDAPv2.SCOPE_SUB, null);
	    while (users.hasMoreElements()) {
	       LDAPEntry userentry = users.next();
	       LDAPModificationSet modset = new LDAPModificationSet();
	       LDAPAttribute attr = new LDAPAttribute("objectclass", attrValue);
	       modset.add(LDAPModification.DELETE, attr);
	       String userdn = userentry.getDN();
	       try {
	           modify(userdn,modset);
               }catch (Exception ex) {
	       
	       }
            }
    	} catch (Exception e) {
	    debug.error("ERROR : while deleting UserObjectClass for service "
			   + serviceName, e);
	}

    }


    private void deleteOrgServiceStatus(String serviceName) {
       
	String orgSearchFilter = getObjectSearchFilter(AMObject.ORGANIZATION);
	if (orgSearchFilter == null) {
	    debug.error("searchfilter is null for AMObject.ORGANIZATION");
        }
	String searchfilter = "&(" + orgSearchFilter + ")" + 
		       "(iplanet-am-service-status=" + serviceName+ ")";
	try {
	    LDAPSearchResults  orgs = 
		searchResults(baseDN ,searchfilter, LDAPv2.SCOPE_SUB, null);
	    while (orgs.hasMoreElements()) {
	        LDAPEntry orgentry = orgs.next();
                String orgdn = orgentry.getDN().toString();
		LDAPModificationSet modset = new LDAPModificationSet();
		LDAPAttribute attr = 
		    new LDAPAttribute("iplanet-am-service-status", serviceName);
		modset.add(LDAPModification.DELETE, attr);
		modify(orgdn,modset);
            } 
        }catch (Exception e) {
	    debug.error("ERROR : while deleting OrgServiceStatus for" +
		    " service " + serviceName, e);
	}
    }


    private void deleteOrgConfig(String serviceName) {

	String orgSearchFilter = getObjectSearchFilter(AMObject.ORGANIZATION);
	if (orgSearchFilter == null) {
	    debug.error("searchfilter is null for AMObject.ORGANIZATION");
        }
	String searchfilter = "&(" + orgSearchFilter + ")" +
		       "(iplanet-am-service-status=" + serviceName+ ")";
	try {
	    LDAPSearchResults  orgs = 
		searchResults(baseDN ,searchfilter, LDAPv2.SCOPE_SUB, null);
            AMStoreConnection amconn = new AMStoreConnection(ssoToken);
	    while (orgs.hasMoreElements()) {
	        LDAPEntry orgentry = orgs.next();
                String orgdn = orgentry.getDN().toString();
		AMOrganization amorg = amconn.getOrganization(orgdn);
		AMTemplate amtemp = amorg.getTemplate(serviceName,
				  AMTemplate.ORGANIZATION_TEMPLATE);
		amtemp.delete();

            }
    	} catch (Exception e) {
	    debug.error("ERROR : while deleting OrgConfig for service " +
			     serviceName, e);
	}
    }


    private void deleteUserServiceAttributes(Set userAttrNames, 
					     String serviceName) {
	String searchfilter = getObjectSearchFilter(AMObject.USER);
	try {
	    LDAPSearchResults  users = 
		searchResults(baseDN, searchfilter, LDAPv2.SCOPE_SUB, null);
	    while (users.hasMoreElements()) {
	       LDAPEntry userentry = users.next();
	       Iterator itr = userAttrNames.iterator();
	       LDAPModificationSet modset = new LDAPModificationSet();
	       while (itr.hasNext()) {
		   String schemaattrname = itr.next().toString();
		   LDAPAttribute attr = userentry.getAttribute(schemaattrname);
                   if(attr != null) {
		       modset.add(LDAPModification.DELETE, attr);
                   }
               }

	       String userdn = userentry.getDN();
	       if(modset.size() > 0) {
	           modify(userdn,modset);
               }
            }
    	} catch (Exception e) {
	    debug.error("ERROR: while deleting UserAttributes for service " + 
			    serviceName, e);
	}

    }


    private LDAPConnection getConnection() throws Exception {
       try {
	  LDAPConnection conn = dlayer.getConnection(connPrincipal);
	  if (conn == null) {
	      throw new Exception ("ERROR: Unable to connect to server!");
          }
	  return conn;
       } catch (Exception e) {
	  throw new Exception(e.toString());
       }
    }


    private LDAPSearchResults searchResults(String baseDN, String filter,
					    int scope, String attrs[])
        throws Exception {
        LDAPConnection ldapConnect = null;
        try {
            LDAPSearchRequest request = LDAPRequestParser.parseSearchRequest(
                baseDN, scope, filter, attrs, false);
            try {
                ldapConnect = getConnection();
	        // Get the sub entries
                return ldapConnect.search(request);
            } finally {
                if (ldapConnect != null) {
                    dlayer.releaseConnection(ldapConnect);
                }
            }
        } catch (Exception e) {
	   throw (new Exception(e.toString()));
        }
    }


    private void modify(String dn, LDAPModificationSet attrs) throws Exception { 
        LDAPConnection ldapConn = null;
	try {
            LDAPModifyRequest request = LDAPRequestParser.parseModifyRequest(
                dn, attrs);
            try {
                ldapConn = getConnection();
	        ldapConn.modify(request);
            } finally {
                if (ldapConn != null) {
                    dlayer.releaseConnection(ldapConn);
                }
            }
        } catch (Exception e) {
	   throw new Exception(e.toString());
        }
    }


    private void delete(String dn) throws Exception {
        LDAPConnection ldapConn = null;
	try {
            LDAPDeleteRequest request = LDAPRequestParser.parseDeleteRequest(
                dn);
            try {
                ldapConn = getConnection();
                ldapConn.delete(request);
            } finally {
                if (ldapConn != null) {
                    dlayer.releaseConnection(ldapConn);
                }
            }
        } catch(Exception e) {
	    throw new Exception(e.toString());
        }
    }

    private String getObjectSearchFilter(int objectType) {
         SearchTemplate searchTemp = null;
         try {
             TemplateManager mgr = TemplateManager.getTemplateManager();
             switch(objectType) {
                 case AMObject.USER:
		     searchTemp =  mgr.getSearchTemplate("BasicUserSearch", null);
		     return searchTemp.getSearchFilter();
                 case AMObject.ORGANIZATION:
	             searchTemp = mgr.getSearchTemplate("BasicOrganizationSearch",
							null);
		     return searchTemp.getSearchFilter();
                 default:
	             return null;
             }

         } catch (Exception e) {
	     debug.error("ERROR while getting Object class", e);
	     return null;
         }

    }  

}
