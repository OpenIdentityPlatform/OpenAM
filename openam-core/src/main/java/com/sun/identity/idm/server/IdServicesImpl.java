/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
* $Id: IdServicesImpl.java,v 1.61 2010/01/20 01:08:36 goodearth Exp $
*
*/

/*
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.sun.identity.idm.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;

import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.common.DNUtils;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPrivilege;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoFatalException;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdServices;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.idm.common.IdRepoUtils;
import com.sun.identity.idm.plugins.internal.SpecialRepo;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import javax.security.auth.callback.NameCallback;

public class IdServicesImpl implements IdServices {

   private final static String DELEGATION_ATTRS_NAME = "attributes";

   protected static Debug debug = Debug.getInstance("amIdm");

   // Cache to hold special identities stored in SpecialRepo
   protected Set specialIdentityNames;
   protected IdSearchResults specialIdentities;
   protected IdSearchResults emptyUserIdentities =
       new IdSearchResults(IdType.USER, "");

   private IdRepoPluginsCache idrepoCache;

   protected static volatile boolean shutdownCalled;

   private static HashSet READ_ACTION = new HashSet(2);

   private static HashSet WRITE_ACTION = new HashSet(2);

   private static IdServices _instance;

   static {
       READ_ACTION.add("READ");
       WRITE_ACTION.add("MODIFY");
   }

   protected static synchronized IdServices getInstance() {
       if (_instance == null) {
           getDebug().message("IdServicesImpl.getInstance(): "
                   + "Creating new Instance of IdServicesImpl()");
           ShutdownManager shutdownMan = ShutdownManager.getInstance();
           if (shutdownMan.acquireValidLock()) {
               try {
                   _instance = new IdServicesImpl();
                   shutdownMan.addShutdownListener(
                       new ShutdownListener() {
                           public void shutdown() {
                               synchronized (_instance) {
                                   shutdownCalled = true;
                               }
                               _instance.clearIdRepoPlugins();
                           }
                   });
               } finally {
                   shutdownMan.releaseLockAndNotify();
               }
           }
       }
       return _instance;
   }

   protected IdServicesImpl() {
       idrepoCache = new IdRepoPluginsCache();
   }

   protected static Debug getDebug() {
       return debug;
   }

   public void reinitialize() {
       idrepoCache.initializeListeners();
   }

   public static boolean isShutdownCalled() {
       return shutdownCalled;
   }

   /**
    * Returns the set of fully qualified names for the identity.
    * The fully qualified names would be unique for a given datastore.
    *
    * @param token SSOToken that can be used by the datastore
    *     to determine the fully qualified name
    * @param type type of the identity
    * @param name name of the identity
    *
    * @return fully qualified names for the identity
    * @throws IdRepoException If there are repository related error conditions
    * @throws SSOException If identity's single sign on token is invalid
    */
   public Set getFullyQualifiedNames(SSOToken token,
       IdType type, String name, String orgName)
       throws IdRepoException, SSOException {
       if (getDebug().messageEnabled()) {
           getDebug().message("IdServicesImpl::getFullyQualifiedNames " +
               "called for type: " + type + " name: " + name +
               " org: " + orgName);
       }

       // Get IdRepo plugins
       Set repos = idrepoCache.getIdRepoPlugins(
           orgName, IdOperation.READ, type);

       // Verify if it is an internal/special identity
       // to avoid calling other plugins for special users
       CaseInsensitiveHashSet answer = new CaseInsensitiveHashSet();
       if (isSpecialIdentity(token, name, type, orgName)) {
           for (Iterator items = repos.iterator();
               items.hasNext();) {
               IdRepo idRepo = (IdRepo) items.next();
               if (idRepo.getClass().getName().equals(
                   IdConstants.SPECIAL_PLUGIN)) {
                   answer.add(idRepo.getFullyQualifiedName(token, type, name));
               }
           }
           return (answer);
       }


       // Get the fully qualified names from IdRepo plugins
       IdRepoException firstException = null;
       if ((repos != null) && !repos.isEmpty()) {
           for (Iterator items = repos.iterator(); items.hasNext();) {
               IdRepo idRepo = (IdRepo) items.next();
               // Skip users in Special Repo
               if (idRepo.getClass().getName().equals(
                   IdConstants.SPECIAL_PLUGIN)) {
                   continue;
               }
               try {
                   String fqn = idRepo.getFullyQualifiedName(token,
                       type, name);
                   if (fqn != null) {
                       answer.add(fqn);
                   }
               } catch (IdRepoException ide) {
                   if (firstException == null) {
                       firstException = ide;
                   }
               }
           }
       }
       if ((firstException != null) && answer.isEmpty()) {
           throw (firstException);
       }
       return (answer);
   }

   /**
    * Returns <code>true</code> if the data store has successfully
    * authenticated the identity with the provided credentials. In case the
    * data store requires additional credentials, the list would be returned
    * via the <code>IdRepoException</code> exception.
    * 
    * @param orgName
    *            realm name to which the identity would be authenticated
    * @param credentials
    *            Array of callback objects containing information such as
    *            username and password.
    * 
    * @return <code>true</code> if data store authenticates the identity;
    *         else <code>false</code>
    */
   public boolean authenticate(String orgName, Callback[] credentials)
           throws IdRepoException, AuthLoginException {
       if (getDebug().messageEnabled()) {
           getDebug().message(
               "IdServicesImpl.authenticate: called for org: " + orgName);
       }

       IdRepoException firstException = null;
       AuthLoginException authException = null;

       // Get the list of plugins and check if they support authN
       Set cPlugins = null;
       try {
           cPlugins = idrepoCache.getIdRepoPlugins(orgName);
       } catch (SSOException ex) {
           // Debug the message and return false
           if (getDebug().messageEnabled()) {
               getDebug().message(
                   "IdServicesImpl.authenticate: " + "Error obtaining " +
                   "IdRepo plugins for the org: " + orgName);
           }
           return (false);
       } catch (IdRepoException ex) {
           // Debug the message and return false
           if (getDebug().messageEnabled()) {
               getDebug().message(
                   "IdServicesImpl.authenticate: " + "Error obtaining " +
                   "IdRepo plugins for the org: " + orgName);
           }
           return (false);
       }
       
       // Check for internal user. If internal user, use SpecialRepo only
       String name = null;
       for (int i = 0; i < credentials.length; i++) {
           if (credentials[i] instanceof NameCallback) {
               name = ((NameCallback) credentials[i]).getName();
               if (DN.isDN(name)) {
                   // Obtain the firsr RDN
                   name = LDAPDN.explodeDN(name, true)[0];
               }
               break;
           }
       }
       SSOToken token = (SSOToken) AccessController.doPrivileged(
           AdminTokenAction.getInstance());
       try {
           if ((name != null) &&
               isSpecialIdentity(token, name, IdType.USER, orgName)) {
               for (Iterator tis = cPlugins.iterator(); tis.hasNext();) {
                   IdRepo idRepo = (IdRepo) tis.next();
                   if (idRepo.getClass().getName().equals(
                       IdConstants.SPECIAL_PLUGIN)) {
                       if (idRepo.authenticate(credentials)) {
                           if (debug.messageEnabled()) {
                               debug.message("IdServicesImpl.authenticate: " +
                                   "AuthN success using special repo " +
                                   idRepo.getClass().getName() +
                                   " user: " + name);
                           }
                           return (true);
                       } else {
                           // Invalid password used for internal user
                           debug.error("IdServicesImpl.authenticate: " +
                               "AuthN failed using special repo " +
                               idRepo.getClass().getName() +
                               " user: " + name);
                           return (false);
                       }

                   }
               }
           }
       } catch (SSOException ssoe) {
           // Ignore the exception
           debug.error("IdServicesImpl.authenticate: AuthN failed " +
               "checking for special users", ssoe);
           return (false);
       }
       
       for (Iterator items = cPlugins.iterator(); items.hasNext();) {
           IdRepo idRepo = (IdRepo) items.next();
           if (idRepo.supportsAuthentication()) {
               if (getDebug().messageEnabled()) {
                   getDebug().message(
                       "IdServicesImpl.authenticate: " + "AuthN to " +
                       idRepo.getClass().getName() + " in org: " + orgName);
               }
               try {
                   if (idRepo.authenticate(credentials)) {
                       // Successfully authenticated
                       if (getDebug().messageEnabled()) {
                           getDebug().message(
                               "IdServicesImpl.authenticate: " +
                               "AuthN success for " +
                               idRepo.getClass().getName());
                       }
                       return (true);
                   }
               } catch (IdRepoException ide) {
                   // Save the exception to be thrown later if
                   // all authentication calls fail
                   if (firstException == null) {
                       firstException = ide;
                   }
               } catch (AuthLoginException authex) {
                   if (authException == null) {
                       authException = authex;
                   }
               }
           } else if (getDebug().messageEnabled()) {
               getDebug().message(
                   "IdServicesImpl.authenticate: AuthN " +
                   "not supported by " + idRepo.getClass().getName());
           }
       }
       if (authException != null) {
           throw (authException);
       }
       if (firstException != null) {
           throw (firstException);
       }
       return (false);
   }

   private AMIdentity getRealmIdentity(SSOToken token, String orgDN)
           throws IdRepoException {
       String universalId = "id=ContainerDefaultTemplateRole,ou=realm,"
               + orgDN;
       return IdUtils.getIdentity(token, universalId);
   }

   private AMIdentity getSubRealmIdentity(SSOToken token, String subRealmName,
           String parentRealmName) throws IdRepoException, SSOException {
       String realmName = parentRealmName;
       if (DN.isDN(parentRealmName)) {
           // Wouldn't be a DN if it starts with "/"
           realmName = DNMapper.orgNameToRealmName(parentRealmName);
       }

       String fullRealmName = realmName + IdConstants.SLASH_SEPARATOR
               + subRealmName;
       String subOrganizationDN = DNMapper.orgNameToDN(fullRealmName);

       return getRealmIdentity(token, subOrganizationDN);
   }

   private AMIdentity createRealmIdentity(SSOToken token, IdType type,
           String name, Map attrMap, String orgName) throws IdRepoException,
           SSOException {

       try {
           OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                   token, orgName);

           Map serviceAttrsMap = new HashMap();
           serviceAttrsMap.put(IdConstants.REPO_SERVICE, attrMap);

           orgMgr.createSubOrganization(name, serviceAttrsMap);

           return getSubRealmIdentity(token, name, orgName);
       } catch (SMSException sme) {
           debug.error("AMIdentityRepository.createIdentity() - "
                   + "Error occurred while creating " + type.getName() + ":"
                   + name, sme);
           throw new IdRepoException(sme.getMessage());
       }
   }

   public AMIdentity create(SSOToken token, IdType type, String name,
       Map attrMap, String amOrgName) throws IdRepoException, SSOException {

       if (type.equals(IdType.REALM)) {                        
           return createRealmIdentity(token, type, name, attrMap, amOrgName);
       }

       IdRepoException origEx = null;
       // First get the list of plugins that support the create operation.
       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, attrMap.keySet(),
               IdOperation.CREATE, type);
       if (type.equals(IdType.USER)) {
           IdRepoAttributeValidator attrValidator = 
               IdRepoAttributeValidatorManager.getInstance().
               getIdRepoAttributeValidator(amOrgName);
           attrValidator.validateAttributes(attrMap, IdOperation.CREATE);
       }
       String amsdkdn = null;
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.CREATE, type);
       if ((configuredPluginClasses == null) ||
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       IdRepo idRepo;
       while (it.hasNext()) {
           idRepo = (IdRepo) it.next();
           try {

               Map cMap = idRepo.getConfiguration(); // do stuff to map attr
               // names.
               Map mappedAttributes = mapAttributeNames(attrMap, cMap);
               String representation = idRepo.create(token, type, name,
                       mappedAttributes);
               if (idRepo.getClass().getName()
                       .equals(IdConstants.AMSDK_PLUGIN)) {
                   amsdkdn = representation;
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.create: "
                       + "Unable to create identity in the"
                       + " following repository "
                       + idRepo.getClass().getName() + ":: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error(
                   "IdServicesImpl.create: "
                   + "Create: Fatal Exception", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.create: "
                       + "Unable to create identity in the following "
                       + "repository "
                       + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           }
       }
       AMIdentity id = new AMIdentity(token, name, type, amOrgName, amsdkdn);
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.create: "
                   + "Unable to create identity " + type.getName() + " :: "
                   + name + " in any of the configured data stores", origEx);
           }
           throw origEx;
       } else {
           return id;
       }

   }

   private void deleteRealmIdentity(SSOToken token, String realmName,
           String orgDN) throws IdRepoException {

       try {
           // By default a Realm is not a leaf node, delete the
           // whole realm tree.
           String parentRealmName = DNMapper.orgNameToRealmName(orgDN);
           OrganizationConfigManager orgMgr =
               new OrganizationConfigManager(token, parentRealmName);
           orgMgr.deleteSubOrganization(realmName, true);
       } catch (SMSException sme) {
           throw new IdRepoException(sme.getMessage());
       }

   }

   /*
    * (non-Javadoc)
    */
   public void delete(SSOToken token, IdType type, String name,
           String orgName, String amsdkDN) throws IdRepoException,
           SSOException {        

       if (type.equals(IdType.REALM)) {
           deleteRealmIdentity(token, name, orgName);
           return;
       }    

       IdRepoException origEx = null;
       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, orgName, name, null, IdOperation.DELETE, type);

       // Get the list of plugins that support the delete operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(orgName,
           IdOperation.DELETE, type);
       if ((configuredPluginClasses == null) || 
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       if (!name.equalsIgnoreCase(IdConstants.ANONYMOUS_USER)) {
           noOfSuccess--;
       }
       IdRepo idRepo;
       while (it.hasNext()) {
           idRepo = (IdRepo) it.next();
           try {
               if (idRepo.getClass().getName()
                   .equals(IdConstants.AMSDK_PLUGIN) && amsdkDN != null) {
                   idRepo.delete(token, type, amsdkDN);
               } else {
                   idRepo.delete(token, type, name);
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.delete: "
                       + "Unable to delete identity in the following "
                       + "repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error(
                   "IdServicesImpl.delete: Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.delete: "
                       + "Unable to delete identity in the following "
                       + "repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               if (!ide.getErrorCode().equalsIgnoreCase("220")) {
                   origEx = ide;
               }
           }
       }
       if ((noOfSuccess <= 0) && (origEx != null)) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.delete: "
                   + "Unable to delete identity " + type.getName() + " :: "
                   + name + " in any of the configured data stores", origEx);
           }
           throw origEx;
       }
       removeIdentityFromPrivileges(name, type, amsdkDN, orgName);
   }

   private void removeIdentityFromPrivileges(
       String name, 
       IdType type,
       String amsdkDN,
       String orgName
   ) {
       SSOToken superAdminToken = (SSOToken)
           AccessController.doPrivileged(AdminTokenAction.getInstance());
       AMIdentity id = new AMIdentity(superAdminToken, name, type, 
           orgName, amsdkDN);
       String uid = id.getUniversalId();

       try {
           DelegationManager mgr = new DelegationManager(
               superAdminToken, orgName);
           Set privilegeObjects = mgr.getPrivileges();

           for (Iterator i = privilegeObjects.iterator(); i.hasNext();) {
               DelegationPrivilege p = (DelegationPrivilege) i.next();
               Set subjects = p.getSubjects();
               if (subjects.contains(uid)) {
                   subjects.remove(uid);
                   mgr.addPrivilege(p);
               }
           }
       } catch (SSOException ex) {
           debug.warning("IdServicesImpl.removeIdentityFromPrivileges", ex);
       } catch (DelegationException ex) {
           debug.warning("IdServicesImpl.removeIdentityFromPrivileges", ex);
       }
   }

   /*
    * (non-Javadoc)
    */
   public Map getAttributes(SSOToken token, IdType type, String name,
           Set attrNames, String amOrgName, String amsdkDN, boolean isString)
           throws IdRepoException, SSOException {
       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, attrNames, IdOperation.READ,
               type);
       // Get the list of plugins that support the read operation
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.READ, type);
       if ((configuredPluginClasses == null) || 
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       // Verify if it is an internal/special identity
       // to avoid calling other plugins for special users
       Set attrMapsSet = new HashSet();
       if (isSpecialIdentity(token, name, type, amOrgName)) {
           try {
               for (Iterator items = configuredPluginClasses.iterator();
                   items.hasNext();) {
                   IdRepo idRepo = (IdRepo) items.next();
                   if (idRepo.getClass().getName().equals(
                       IdConstants.SPECIAL_PLUGIN)) {
                       attrMapsSet.add(idRepo.getAttributes(token, type, name,
                           attrNames));
                       return (combineAttrMaps(attrMapsSet, true));
                   }
               }
           } catch (Exception e) {
               // Ignore and continue
           }
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       IdRepo idRepo;
       while (it.hasNext()) {
           idRepo = (IdRepo) it.next();
           try {
               Map cMap = idRepo.getConfiguration();
               // do stuff to map attr names.
               Set mappedAttributeNames = mapAttributeNames(attrNames, cMap);
               Map aMap = null;
               if (idRepo.getClass().getName()
                   .equals(IdConstants.AMSDK_PLUGIN) && amsdkDN != null) {
                   if (isString) {
                       aMap = idRepo.getAttributes(token, type, amsdkDN,
                               mappedAttributeNames);
                   } else {
                       aMap = idRepo.getBinaryAttributes(token, type, amsdkDN,
                               mappedAttributeNames);
                   }
               } else {
                   if (isString) {
                       aMap = idRepo.getAttributes(token, type, name,
                               mappedAttributeNames);
                   } else {
                       aMap = idRepo.getBinaryAttributes(token, type, name,
                               mappedAttributeNames);
                   }
               }
               aMap = reverseMapAttributeNames(aMap, cMap);
               attrMapsSet.add(aMap);
           } catch (IdRepoUnsupportedOpException ide) {
               if (getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.getAttributes: "
                       + "Unable to read identity in the following "
                       + "repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error("GetAttributes: Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.getAttributes: "
                       + "Unable to read identity in the following "
                       + "repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           }
       }

       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning("idServicesImpl.getAttributes: " +
                   "Unable to get attributes for identity " + type.getName() + 
                   ", " + name + " in any configured data store", origEx);
           }
           throw origEx;
       }

       return combineAttrMaps(attrMapsSet, isString);
   }

   /*
    * (non-Javadoc)
    */
   public Map getAttributes(
       SSOToken token,
       IdType type,
       String name,
       String amOrgName,
       String amsdkDN
   ) throws IdRepoException, SSOException {
       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, null, IdOperation.READ, type);
       // Get the list of plugins that support the read operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.READ, type);
       if ((configuredPluginClasses == null) || 
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       // Verify if it is an internal/special identity
       // to avoid calling other plugins for special users
       Set attrMapsSet = new HashSet();
       if (isSpecialIdentity(token, name, type, amOrgName)) {
           try {
               for (Iterator items = configuredPluginClasses.iterator();
                   items.hasNext();) {
                   IdRepo idRepo = (IdRepo) items.next();
                   if (idRepo.getClass().getName().equals(
                       IdConstants.SPECIAL_PLUGIN)) {
                       attrMapsSet.add(idRepo.getAttributes(
                           token, type, name));
                       return (combineAttrMaps(attrMapsSet, true));
                   }
               }
           } catch (Exception e) {
               // Ignore and continue
           }
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();

       while (it.hasNext()) {
           IdRepo idRepo = (IdRepo) it.next();
           try {
               Map cMap = idRepo.getConfiguration();
               Map aMap = null;
               if (idRepo.getClass().getName()
                   .equals(IdConstants.AMSDK_PLUGIN) && (amsdkDN != null)) {
                   aMap = idRepo.getAttributes(token, type, amsdkDN);
               } else {
                   aMap = idRepo.getAttributes(token, type, name);
               }
               if (getDebug().messageEnabled()) {
                   getDebug().message("IdServicesImpl.getAttributes: " +
                       "before reverseMapAttributeNames aMap=" +
                        IdRepoUtils.getAttrMapWithoutPasswordAttrs(aMap, null));
               }
               aMap = reverseMapAttributeNames(aMap, cMap);
               attrMapsSet.add(aMap);
               if (getDebug().messageEnabled()) {
                   for(Iterator iter = attrMapsSet.iterator();iter.hasNext();){
                       Map attrMap = (Map)iter.next();
                       getDebug().message("IdServicesImpl.getAttributes: " +
                       "after before reverseMapAttributeNames attrMapsSet=" + 
                       IdRepoUtils.getAttrMapWithoutPasswordAttrs(attrMap,
                       null));
                   }
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.getAttributes: "
                       + "Unable to read identity in the following "
                       + "repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error("IdServicesImpl.getAttributes: "
                       + "Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning("IdServicesImpl.getAttributes: "
                       + "Unable to read identity in the following "
                       + "repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning("IdServicesImpl.getAttributes: "
                   + "Unable to get attributes for identity "
                   + type.getName() +
                   "::" + name + " in any configured data store", origEx);
           }
           throw origEx;
       } else {
           Map returnMap = combineAttrMaps(attrMapsSet, true);
           getDebug().warning("IdServicesImpl.getAttributes exit: " +
               "returnMap=" + IdRepoUtils.getAttrMapWithoutPasswordAttrs(
               returnMap, null));
           return returnMap;
       }
   }

   /*
    * (non-Javadoc)
    */
   public Set getMembers(SSOToken token, IdType type, String name,
           String amOrgName, IdType membersType, String amsdkDN)
           throws IdRepoException, SSOException {
       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, null, IdOperation.READ, type);

       // Get the list of plugins that support the read operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.READ, type);
       if ((configuredPluginClasses == null) ||
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       Set membersSet = new HashSet();
       Set amsdkMembers = new HashSet();
       boolean amsdkIncluded = false;

       while (it.hasNext()) {
           IdRepo idRepo = (IdRepo) it.next();
           if (!idRepo.getSupportedTypes().contains(membersType) ||
               idRepo.getClass().getName().equals(IdConstants.SPECIAL_PLUGIN)) {
               // IdRepo plugin does not support the idType for
               // memberships
               noOfSuccess--;
               continue;
           }
           try {
               boolean isAMSDK = idRepo.getClass().getName().equals(
                       IdConstants.AMSDK_PLUGIN);
               Set members = (isAMSDK && (amsdkDN != null)) ? 
                   idRepo.getMembers(token, type, amsdkDN, membersType) :
                   idRepo.getMembers(token, type, name, membersType);
               if (isAMSDK) {
                   amsdkMembers.addAll(members);
                   amsdkIncluded = true;
               } else {
                   membersSet.add(members);
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.getMembers: "
                       + "Unable to read identity members in the following"
                       + " repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error(
                   "IdServicesImpl.getMembers: "
                   + "Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.getMembers: "
                       + "Unable to read identity members in the following"
                       + " repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.getMembers: "
                   + "Unable to get members for identity " + type.getName()
                   + "::" + name + " in any configured data store", origEx);
           }
           if (origEx != null) {
               throw origEx;
           } else {
               return (Collections.EMPTY_SET);
           }
       } else {
           Set results = combineMembers(token, membersSet, membersType,
                   amOrgName, amsdkIncluded, amsdkMembers);
           return results;
       }
   }

   /*
    * (non-Javadoc)
    */
   public Set getMemberships(
       SSOToken token, 
       IdType type, 
       String name,
       IdType membershipType,
       String amOrgName,
       String amsdkDN
   ) throws IdRepoException, SSOException {
       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, null, IdOperation.READ, type);

       // Get the list of plugins that support the read operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.READ, type);
       if ((configuredPluginClasses == null) || 
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       // If Special Identity, call SpecialRepo
       if (isSpecialIdentity(token, name, type, amOrgName)) {
           try {
               for (Iterator items = configuredPluginClasses.iterator();
                   items.hasNext();) {
                   IdRepo idRepo = (IdRepo) items.next();
                   if (idRepo.getClass().getName().equals(
                       IdConstants.SPECIAL_PLUGIN)) {
                       return (idRepo.getMemberships(token, type,
                           name, membershipType));
                   }
               }
           } catch (Exception e) {
               // Ignore and continue
           }
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       Set membershipsSet = new HashSet();
       Set amsdkMemberShips = new HashSet();
       boolean amsdkIncluded = false;

       while (it.hasNext()) {
           IdRepo idRepo = (IdRepo)it.next();
           if (!idRepo.getSupportedTypes().contains(membershipType) ||
               idRepo.getClass().getName().equals(IdConstants.SPECIAL_PLUGIN)) {
               // IdRepo plugin does not support the idType for
               // memberships
               noOfSuccess--;
               continue;
           }
           try {
               boolean isAMSDK = idRepo.getClass().getName().equals(
                       IdConstants.AMSDK_PLUGIN);
               Set members = (isAMSDK && (amsdkDN != null)) ? 
                   idRepo.getMemberships(token, type, amsdkDN, membershipType)
                   : idRepo.getMemberships(token, type, name, membershipType);
               if (isAMSDK) {
                   amsdkMemberShips.addAll(members);
                   amsdkIncluded = true;
               } else {
                   membershipsSet.add(members);
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.getMemberships: "
                       + "Unable to get memberships in the following "
                       + "repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error(
                   "IdServicesImpl.getMemberships: "
                   + "Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.getMemberships: "
                       + "Unable to read identity in the following "
                       + "repository " + idRepo.getClass().getName(), ide);
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.getMemberships: "
                   + "Unable to get members for identity " + type.getName()
                   + "::" + name + " in any configured data store", origEx);
           }
           if (origEx != null) {
               throw origEx;
           } else {
               return (Collections.EMPTY_SET);
           }
       } else {
           Set results = combineMembers(token, membershipsSet, membershipType,
                   amOrgName, amsdkIncluded, amsdkMemberShips);
           return results;
       }
   }

   /*
    * (non-Javadoc)
    */
   public boolean isExists(SSOToken token, IdType type, String name,
           String amOrgName) throws SSOException, IdRepoException {
       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, null, IdOperation.READ, type);

       // Get the list of plugins that support the read operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.READ, type);
       if ((configuredPluginClasses == null) ||
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       // Verify if it is an internal/special identity
       // To avoid loading other plugins
       if (isSpecialIdentity(token, name, type, amOrgName)) {
           try {
               for (Iterator items = configuredPluginClasses.iterator();
                   items.hasNext();) {
                   IdRepo idRepo = (IdRepo) items.next();
                   if (idRepo.getClass().getName().equals(
                       IdConstants.SPECIAL_PLUGIN)) {
                       return (idRepo.isExists(token, type, name));
                   }
               }
           } catch (Exception idm) {
               // Ignore the exception
           }
       }

       // Iterate through other plugins
       Iterator it = configuredPluginClasses.iterator();
       boolean exists = false;
       try {
           while (it.hasNext()) {
               IdRepo idRepo = (IdRepo) it.next();
               exists = idRepo.isExists(token, type, name);
               if (exists) {
                   break;
               }
           }
       } catch (Exception idm) {
           // Ignore the exception if not found in one plugin.
           // Iterate through all configured plugins and look for the
           // identity and if found break the loop, if not finally return
           // false.
       }
       return exists;
   }

   public boolean isActive(SSOToken token, IdType type, String name,
           String amOrgName, String amsdkDN) throws SSOException,
           IdRepoException {
       IdRepoException origEx = null;
       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, null, IdOperation.READ, type);

       // First get the list of plugins that support the create operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.READ, type);
       if ((configuredPluginClasses == null) ||
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       // Verify if it is an internal/special identity
       // To avoid loading other plugins
       if (isSpecialIdentity(token, name, type, amOrgName)) {
           try {
               for (Iterator items = configuredPluginClasses.iterator();
                   items.hasNext();) {
                   IdRepo idRepo = (IdRepo) items.next();
                   if (idRepo.getClass().getName().equals(
                       IdConstants.SPECIAL_PLUGIN)) {
                       return (idRepo.isActive(token, type, name));
                   }
               }
           } catch (Exception idm) {
               // Ignore exception
           }
       }

       // Iterator through the plugins
       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       boolean active = false;
       while (it.hasNext()) {
           IdRepo idRepo = (IdRepo) it.next();
           try {
               if (idRepo.getClass().getName().equals(
                   IdConstants.AMSDK_PLUGIN) && (amsdkDN != null)) {
                   active = idRepo.isActive(token, type, amsdkDN);
               } else if (idRepo.getClass().getName().equals(
                   IdConstants.SPECIAL_PLUGIN)) {
                   // Already checked above
                   noOfSuccess--;
                   continue;
               } else {
                   active = idRepo.isActive(token, type, name);
               }
               if (active) {
                   break;
               }
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               debug.error("IdServicesImpl.isActive: Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   debug.warning("IdServicesImpl.isActive: "
                       + "Unable to check isActive identity in the "
                       + "following repository "
                       + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           }
       }

       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.isActive: "
                   + "Unable to check if identity is active " + type.getName()
                   + "::" + name + " in any configured data store", origEx);
           }
           if (origEx != null) {
               throw origEx;
           } else {
               Object args[] = { "isActive", IdOperation.READ.getName()};
               throw new IdRepoUnsupportedOpException(
                   IdRepoBundle.BUNDLE_NAME, "305", args);
           }
       }

       return active;
   }

   /*
    * (non-Javadoc)
    */
   public void setActiveStatus(SSOToken token, IdType type, String name,
       String amOrgName, String amsdkDN, boolean active)
       throws SSOException, IdRepoException {

       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, null, IdOperation.EDIT, type);

       // First get the list of plugins that support the edit operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.EDIT, type);
       if ((configuredPluginClasses == null) ||
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       while (it.hasNext()) {
           IdRepo idRepo = (IdRepo) it.next();
           try {
               if (idRepo.getClass().getName().equals(
                   IdConstants.AMSDK_PLUGIN) && amsdkDN != null) {
                   idRepo.setActiveStatus(token, type, amsdkDN, active);
               } else {
                   idRepo.setActiveStatus(token, type, name, active);
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning("IdServicesImpl:setActiveStatus: "
                           + "Unable to set attributes in the following "
                           + "repository" + idRepo.getClass().getName()
                           + " :: " + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide : origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error("IsActive: Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "Unable to setActiveStatus in the " +
                       "following repository" + idRepo.getClass().getName() +
                       " :: " + ide.getMessage());
               }
               noOfSuccess--;
               // 220 is entry not found. this error should have lower
               // precedence than other error because we search thru all
               // the ds and this entry might exist in one of the other ds.
               if (!ide.getErrorCode().equalsIgnoreCase("220") ||
                   (origEx == null)) {
                   origEx = ide;
               }
           }
       }
       if (noOfSuccess == 0) {
           getDebug().error("Unable to setActiveStatus for identity "
                   + type.getName() + "::" + name + " in any configured "
                   + "datastore", origEx);
           throw origEx;
       }
   }

   private void validateMembers(
       SSOToken token,
       Set members,
       IdType type,
       String amOrgName
   ) throws IdRepoException, SSOException {
       for (Iterator i = members.iterator(); i.hasNext(); ) {
           String name = (String)i.next();
           if (!isExists(token, type, name, amOrgName)) {
               Object[] args = {name, type.getName() };
               throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "223",args);
           }
       }
   }

   /*
    * (non-Javadoc)
    */
   public void modifyMemberShip(SSOToken token, IdType type, String name,
           Set members, IdType membersType, int operation, String amOrgName)
           throws IdRepoException, SSOException {
       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, null, IdOperation.EDIT, type);

       // First get the list of plugins that support the create operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.EDIT, type);
       if ((configuredPluginClasses == null) || 
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       //check if the identity exist
       if (!isExists(token, type, name, amOrgName)) {
           Object[] args = {name, type.getName()};
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "223", args);
       }
       validateMembers(token, members, membersType, amOrgName);

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       while (it.hasNext()) {
           IdRepo idRepo = (IdRepo) it.next();
           if (!idRepo.getSupportedTypes().contains(membersType) ||
               idRepo.getClass().getName().equals(IdConstants.SPECIAL_PLUGIN)) {
               // IdRepo plugin does not support the idType for
               // memberships
               noOfSuccess--;
               continue;
           }
           try {
               idRepo.modifyMemberShip(token, type, name, members,
                       membersType, operation);
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning("IdServicesImpl.modifyMembership: "
                       + "Unable to modify memberships  in the following"
                       + " repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx ==null) ? ide :origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error("IdServicesImpl.modifyMembership: "
                   + "Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().error("IdServicesImpl.modifyMembership: "
                       + "Unable to modify memberships in the following"
                       + " repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx ==null) ? ide :origEx;
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning("IdServicesImpl.modifyMemberShip: "
                   + "Unable to modify members for identity " + type.getName()
                   + "::" + name + " in any configured data store", origEx);
           }
           if (origEx != null) {
               throw origEx;
           } else {
               Object args[] = { "modifyMemberShip",
                   IdOperation.EDIT.getName()};
               throw new IdRepoUnsupportedOpException(
                   IdRepoBundle.BUNDLE_NAME, "305", args);
           }
       }
   }

   /*
    * (non-Javadoc)
    */
   public void removeAttributes(SSOToken token, IdType type, String name,
           Set attrNames, String amOrgName, String amsdkDN)
           throws IdRepoException, SSOException {
       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, attrNames, IdOperation.EDIT,
               type);

       // First get the list of plugins that support the create operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.EDIT, type);
       if ((configuredPluginClasses == null) || 
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       while (it.hasNext()) {
           IdRepo idRepo = (IdRepo) it.next();
           try {
               Map cMap = idRepo.getConfiguration();
               // do stuff to map attr names.
               Set mappedAttributeNames = mapAttributeNames(attrNames, cMap);
               if (idRepo.getClass().getName().equals(
                   IdConstants.AMSDK_PLUGIN) && (amsdkDN != null)) {
                   idRepo.removeAttributes(token, type, amsdkDN, mappedAttributeNames);
               } else {
                   idRepo.removeAttributes(token, type, name, mappedAttributeNames);
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.removeAttributes: "
                       + "Unable to modify identity in the following "
                       + "repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               debug.error("IdServicesImpl.removeAttributes: " +
                   "Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning("IdServicesImpl.removeAttributes: "
                       + "Unable to remove attributes in the following "
                       + "repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               // 220 is entry not found. this error should have lower
               // precedence than other errors because we search through
               // all the ds and this entry might exist in one of the other ds.
               if (!ide.getErrorCode().equalsIgnoreCase("220")
                       || (origEx == null)) {
                   origEx = ide;
               }
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.removeAttributes: "
                   + "Unable to remove attributes  for identity "
                   + type.getName() + "::" + name
                   + " in any configured data store", origEx);
           }
           throw origEx;
       }
   }

   public IdSearchResults search(SSOToken token, IdType type, String pattern,
           IdSearchControl ctrl, String amOrgName) throws IdRepoException,
           SSOException {
       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       // In the case of web services security (wss), a search is performed
       // with the identity of shared agent and  a filter. 
       // Since shared agents do not have search permissions, might have to 
       // use admintoken and check permissions on matched objects.
       boolean checkPermissionOnObjects = false;
       SSOToken userToken = token;
       try {
           checkPermission(token, amOrgName, null, null,
               IdOperation.READ, type);
       } catch (IdRepoException ire) {
           // If permission denied and control has search filters
           // perform the search and check permissions on the matched objects
           Map filter = ctrl.getSearchModifierMap();
           if ((!ire.getErrorCode().equals("402")) || (filter == null) || 
               (filter.isEmpty())) {
               throw (ire);
           }
           // Check permissions after obtaining the matched objects
           checkPermissionOnObjects = true;
           token = (SSOToken) AccessController.doPrivileged(
               AdminTokenAction.getInstance());
       }

       // First get the list of plugins that support the create operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.READ, type);
       if ((configuredPluginClasses == null) || 
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       IdRepo idRepo;
       Object[][] amsdkResults = new Object[1][2];
       boolean amsdkIncluded = false;
       Object[][] arrayOfResult = new Object[noOfSuccess][2];
       int iterNo = 0;
       int maxTime = ctrl.getTimeOut();
       int maxResults = ctrl.getMaxResults();
       Set returnAttrs = ctrl.getReturnAttributes();
       boolean returnAllAttrs = ctrl.isGetAllReturnAttributesEnabled();
       IdSearchOpModifier modifier = ctrl.getSearchModifier();
       int filterOp = IdRepo.NO_MOD;
       if (modifier.equals(IdSearchOpModifier.AND)) {
           filterOp = IdRepo.AND_MOD;
       } else if (modifier.equals(IdSearchOpModifier.OR)) {
           filterOp = IdRepo.OR_MOD;
       }
       Map avPairs = ctrl.getSearchModifierMap();
       boolean recursive = ctrl.isRecursive();
       while (it.hasNext()) {
           idRepo = (IdRepo) it.next();
           try {
               Map cMap = idRepo.getConfiguration();
               RepoSearchResults results = idRepo.search(token, type, pattern,
                       maxTime, maxResults, returnAttrs, returnAllAttrs,
                       filterOp, avPairs, recursive);
               if (idRepo.getClass().getName()
                       .equals(IdConstants.AMSDK_PLUGIN)) {
                   amsdkResults[0][0] = results;
                   amsdkResults[0][1] = cMap;
                   amsdkIncluded = true;
               } else {
                   arrayOfResult[iterNo][0] = results;
                   arrayOfResult[iterNo][1] = cMap;
                   iterNo++;
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.search: "
                       + "Unable to search in the following repository "
                       + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error(
                   "IdServicesImpl.search: Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.search: "
                       + "Unable to search identity in the following"
                       + " repository " + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.search: "
                   + "Unable to search for identity " + type.getName()
                   + "::" + pattern
                   + " in any configured data store", origEx);
           }
           throw origEx;
       } else {
           IdSearchResults res = combineSearchResults(token, arrayOfResult,
               iterNo, type, amOrgName, amsdkIncluded, amsdkResults);
           if (checkPermissionOnObjects) {
               IdSearchResults newRes = new IdSearchResults(type, amOrgName);
               Map idWithAttrs = res.getResultAttributes();
               for (Iterator items = idWithAttrs.keySet().iterator();
                   items.hasNext();) {
                   AMIdentity id = (AMIdentity) items.next();
                   try {
                       checkPermission(userToken, amOrgName, id.getName(),
                           returnAttrs, IdOperation.READ, type);
                       // Permission checked, add to newRes
                       newRes.addResult(id, (Map) idWithAttrs.get(id));
                   } catch (Exception e) {
                       // Ignore & continue
                   }
               }
               res = newRes;
           }
           return res;
       }
   }

   public IdSearchResults getSpecialIdentities(SSOToken token, IdType type,
           String orgName) throws IdRepoException, SSOException {

       Set pluginClasses = new OrderedSet();

       if (ServiceManager.isConfigMigratedTo70()
           && ServiceManager.getBaseDN().equalsIgnoreCase(orgName)) {
           // Check the cache
           if (specialIdentities != null) {
               return (specialIdentities);
           }

           // get the "SpecialUser plugin
           Set repos = idrepoCache.getIdRepoPlugins(orgName);
           for (Iterator items = repos.iterator(); items.hasNext();) {
               IdRepo repo = (IdRepo) items.next();
               if (repo instanceof SpecialRepo) {
                   pluginClasses.add(repo);
               }
           }
       }

       // If no plugins found, return empty results
       if (pluginClasses.isEmpty()) {
           return (emptyUserIdentities);
       } else {
           IdRepo specialRepo = (IdRepo) pluginClasses.iterator().next();
           RepoSearchResults res = specialRepo.search(token, type, "*", 0, 0,
                   Collections.EMPTY_SET, false, 0, Collections.EMPTY_MAP,
                   false);
           Object obj[][] = new Object[1][2];
           obj[0][0] = res;
           obj[0][1] = Collections.EMPTY_MAP;
           specialIdentities = combineSearchResults(token, obj, 1, type,
               orgName, false, null);
       }
       return (specialIdentities);
   }

   protected boolean isSpecialIdentity(SSOToken token, String name,
       IdType type, String orgName) throws IdRepoException, SSOException {
       if (ServiceManager.isConfigMigratedTo70() &&
           ServiceManager.getBaseDN().equalsIgnoreCase(orgName) &&
           type.equals(IdType.USER)) {
           // Check the cache
           if (specialIdentityNames == null) {
               // get the "SpecialUser plugin
               Set spIds = new CaseInsensitiveHashSet();
               Set repos = idrepoCache.getIdRepoPlugins(orgName);
               for (Iterator items = repos.iterator(); items.hasNext();) {
                   IdRepo repo = (IdRepo) items.next();
                   if (repo instanceof SpecialRepo) {
                       RepoSearchResults res = repo.search(token, type, "*",
                           0, 0, Collections.EMPTY_SET, false,
                           0, Collections.EMPTY_MAP, false);
                       Set identities = res.getSearchResults();
                       for (Iterator ids = identities.iterator();
                           ids.hasNext();) {
                           spIds.add(ids.next());
                       }
                   }
               }
               specialIdentityNames = spIds;
           }

           if ((specialIdentityNames != null) &&
               !specialIdentityNames.isEmpty()) {
               return (specialIdentityNames.contains(name));
           }
       }
       return (false);
   }

   public void setAttributes(SSOToken token, IdType type, String name,
           Map attributes, boolean isAdd, String amOrgName, String amsdkDN,
           boolean isString) throws IdRepoException, SSOException {
       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, attributes.keySet(),
               IdOperation.EDIT, type);

       if (type.equals(IdType.USER)) {
           IdRepoAttributeValidator attrValidator = 
               IdRepoAttributeValidatorManager.getInstance().
               getIdRepoAttributeValidator(amOrgName);
           attrValidator.validateAttributes(attributes, IdOperation.EDIT);
       }

       // Get the list of plugins that service/edit the create operation.
       Set configuredPluginClasses = (attributes.containsKey("objectclass")) ?
           idrepoCache.getIdRepoPlugins(amOrgName, IdOperation.SERVICE, type):
           idrepoCache.getIdRepoPlugins(amOrgName, IdOperation.EDIT, type);
       if ((configuredPluginClasses == null) || 
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       IdRepo idRepo;
       while (it.hasNext()) {
           idRepo = (IdRepo) it.next();
           try {
               Map cMap = idRepo.getConfiguration();
               // do stuff to map attr names.
               Map mappedAttributes = mapAttributeNames(attributes, cMap);
               if (idRepo.getClass().getName()
                   .equals(IdConstants.AMSDK_PLUGIN) && amsdkDN != null) {
                   if (isString) {
                       idRepo.setAttributes(token, type, amsdkDN, mappedAttributes,
                               isAdd);
                   } else {
                       idRepo.setBinaryAttributes(token, type, amsdkDN,
                               mappedAttributes, isAdd);
                   }
               } else {
                   if (isString) {
                       idRepo.setAttributes(token, type, name, mappedAttributes,
                               isAdd);
                   } else {
                       idRepo.setBinaryAttributes(token, type, name,
                               mappedAttributes, isAdd);
                   }
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().messageEnabled()) {
                   getDebug().message("IdServicesImpl.setAttributes: "
                           + "Unable to set attributes in the following "
                           + "repository "
                           + idRepo.getClass().getName()
                           + " :: " + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error(
                   "IdServicesImpl.setAttributes: Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.setAttributes: "
                       + "Unable to modify identity in the "
                       + "following repository "
                       + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               // 220 is entry not found. this error should have lower
               // precedence than other error because we search thru
               // all the ds and this entry might exist in one of the other ds.
               if (!ide.getErrorCode().equalsIgnoreCase("220")
                       || (origEx == null)) {
                   origEx = ide;
               }
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.setAttributes: "
                   + "Unable to set attributes  for identity "
                   + type.getName() + "::" + name + " in any configured data"
                   + " store", origEx);
           }
           throw origEx;
       }
   }

   public void changePassword(SSOToken token, IdType type, String name,
       String oldPassword, String newPassword, String amOrgName,
       String amsdkDN) throws IdRepoException, SSOException {

       String attrName = "userPassword";
       Set attrNames = new HashSet();
       attrNames.add(attrName);

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, attrNames, IdOperation.EDIT,
           type);
       // Get the list of plugins that service/edit the create operation.

       if (type.equals(IdType.USER)) {
           IdRepoAttributeValidator attrValidator = 
               IdRepoAttributeValidatorManager.getInstance().
               getIdRepoAttributeValidator(amOrgName);
           HashMap attributes = new HashMap();
           Set values = new HashSet();
           values.add(newPassword);
           attributes.put(attrName, values);
           attrValidator.validateAttributes(attributes, IdOperation.EDIT);
       }

       Set configuredPluginClasses = 
           idrepoCache.getIdRepoPlugins(amOrgName, IdOperation.EDIT, type);
       if ((configuredPluginClasses == null) || 
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       IdRepoException origEx = null;
       IdRepo idRepo;
       while (it.hasNext()) {
           idRepo = (IdRepo) it.next();
           Map cMap = idRepo.getConfiguration();
           Set mappedAttributeNames = mapAttributeNames(attrNames, cMap);
           if ((mappedAttributeNames != null) && (!mappedAttributeNames.isEmpty())) {
               attrName = (String)mappedAttributeNames.iterator().next();
           }

           try {
               if (idRepo.getClass().getName().equals(IdConstants.AMSDK_PLUGIN)
                   && (amsdkDN != null)) {

                   idRepo.changePassword(token, type, amsdkDN, attrName, 
                       oldPassword, newPassword);
               } else {
                   idRepo.changePassword(token, type, name, attrName,
                       oldPassword, newPassword);
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().messageEnabled()) {
                   getDebug().message("IdServicesImpl.changePassword: "
                           + "Unable to change password in the following "
                           + "repository "
                           + idRepo.getClass().getName()
                           + " :: " + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error(
                   "IdServicesImpl.changePassword: Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.changePassword: "
                       + "Unable to change password "
                       + "following repository "
                       + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               // 220 is entry not found. this error should have lower
               // precedence than other error because we search thru
               // all the ds and this entry might exist in one of the other ds.
               if (!ide.getErrorCode().equalsIgnoreCase("220")
                       || (origEx == null)) {
                   origEx = ide;
               }
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.changePassword: "
                   + "Unable to change password  for identity "
                   + type.getName() + "::" + name + " in any configured data"
                   + " store", origEx);
           }
           throw origEx;
       }
   }

   public Set getAssignedServices(SSOToken token, IdType type, String name,
           Map mapOfServiceNamesAndOCs, String amOrgName, String amsdkDN)
           throws IdRepoException, SSOException {
       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, null, IdOperation.READ, type);
       // Get the list of plugins that support the service operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName, 
           IdOperation.SERVICE, type);
       if ((configuredPluginClasses == null) || 
           configuredPluginClasses.isEmpty()) {
           if (ServiceManager.getBaseDN().equalsIgnoreCase(amOrgName)
                   && (type.equals(IdType.REALM))) {
               return (configuredPluginClasses);
           } else {
               throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", 
                       null);
           }
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       IdRepo idRepo = null;
       Set resultsSet = new HashSet();
       while (it.hasNext()) {
           IdRepo repo = (IdRepo) it.next();
           try {
               Set services = null;
               if (repo.getClass().getName().equals(IdConstants.AMSDK_PLUGIN)
                       && amsdkDN != null) {
                   services = repo.getAssignedServices(token, type, amsdkDN,
                           mapOfServiceNamesAndOCs);
               } else {
                   services = repo.getAssignedServices(token, type, name,
                           mapOfServiceNamesAndOCs);
               }
               if (services != null && !services.isEmpty()) {
                   resultsSet.addAll(services);
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().messageEnabled()) {
                   getDebug().message(
                       "IdServicesImpl.getAssignedServices: "
                       + "Services not supported for repository "
                       + repo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error("IdServicesImpl.getAssignedServices: " +
                   "Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning("IdServicesImpl.getAssignedServices: "
                       + "Unable to get services for identity "
                       + "in the following repository "
                       + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning("IdServicesImpl.getAssignedServices: "
                   + "Unable to get assigned services for identity "
                   + type.getName() + "::" + name
                   + " in any configured data store", origEx);
           }
           throw origEx;
       } else {
           return resultsSet;
       }

   }

   public void assignService(SSOToken token, IdType type, String name,
           String serviceName, SchemaType stype, Map attrMap,
           String amOrgName, String amsdkDN) throws IdRepoException,
           SSOException {
       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, null,
           IdOperation.SERVICE, type);
       // Get the list of plugins that support the service operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
               IdOperation.SERVICE, type);
       if (configuredPluginClasses == null
               || configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       IdRepo idRepo = null;
       while (it.hasNext()) {
           IdRepo repo = (IdRepo) it.next();
           Map cMap = repo.getConfiguration();
           try {
               Map mappedAttributes= mapAttributeNames(attrMap, cMap);
               if (repo.getClass().getName().equals(IdConstants.AMSDK_PLUGIN)
                       && amsdkDN != null) {
                   repo.assignService(token, type, amsdkDN, serviceName,
                           stype, mappedAttributes);
               } else {
                   repo.assignService(token, type, name, serviceName, stype,
                           mappedAttributes);
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().messageEnabled()) {
                   getDebug().message("IdServicesImpl.assignService: "
                           + "Assign Services not supported for repository "
                           + repo.getClass().getName()
                           + " :: " + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error(
                   "IdServicesImpl.assignService: FatalException ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.assignService: "
                       + "Unable to assign Service identity in "
                       + "the following repository "
                       + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }   
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.assignService: "
                   + "Unable to assign service for identity " 
                   + type.getName()
                   + "::" + name + " in any configured data store ", origEx);
           }
           throw origEx;
       }
   }

   public void unassignService(SSOToken token, IdType type, String name,
           String serviceName, Map attrMap, String amOrgName, String amsdkDN)
           throws IdRepoException, SSOException {
       IdRepoException origEx = null;

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, null, IdOperation.SERVICE,
               type);
       // Get the list of plugins that support the service operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName, 
           IdOperation.SERVICE, type);
       if ((configuredPluginClasses == null) ||
           configuredPluginClasses.isEmpty()
       ) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       IdRepo idRepo = null;
       while (it.hasNext()) {
           IdRepo repo = (IdRepo) it.next();
           Map cMap = repo.getConfiguration();
           try {
               Map mappedAttributes = mapAttributeNames(attrMap, cMap);
               if (repo.getClass().getName().equals(IdConstants.AMSDK_PLUGIN)
                       && amsdkDN != null) {
                   repo.unassignService(token, type, amsdkDN, serviceName,
                           mappedAttributes);
               } else {
                   repo.unassignService(token, type, name, serviceName,
                           mappedAttributes);
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().messageEnabled()) {
                   getDebug().message(
                       "IdServicesImpl.unassignService: "
                       + "Unassign Service not supported for repository "
                       + repo.getClass().getName()
                       + " :: " + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error(
                  "IdServicesImpl.unassignService: Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.unassignService: "
                       + "Unable to unassign service in the "
                       + "following repository "
                       + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.unassignService: "
                   + "Unable to unassign Service for identity "
                   + type.getName() + "::" + name + " in any configured "
                   + "data store ", origEx);
           }
           throw origEx;
       }

   }

   /**
    * Non-javadoc, non-public methods
    * Get the service attributes of the name identity. Traverse to the global
    * configuration if necessary until all attributes are found or reached
    * the global area whichever occurs first.
    *
    * @param token is the sso token of the person performing this operation.
    * @param type is the identity type of the name parameter.
    * @param name is the identity we are interested in.
    * @param serviceName is the service we are interested in
    * @param attrNames are the name of the attributes wer are interested in.
    * @param amOrgName is the orgname.
    * @param amsdkDN is the amsdkDN.
    * @throws IdRepoException if there are repository related error conditions.
    * @throws SSOException if user's single sign on token is invalid.
    */
   public Map getServiceAttributesAscending(SSOToken token, IdType type,
           String name, String serviceName, Set attrNames, String amOrgName,
           String amsdkDN) throws IdRepoException, SSOException {

       Map finalResult = new HashMap();
       Set finalAttrName = new HashSet();
       String nextName = name;
       String nextAmOrgName = amOrgName;
       String nextAmsdkDN = amsdkDN;
       IdType nextType = type;
       Set missingAttr = new HashSet(attrNames);
       do {
           // name is the name of AMIdentity object. will change as we move
           // up the tree.
           // attrNames is missingAttr and will change as we move up the tree.
           // amOrgname will change as we move up the tree.
           // amsdkDN will change as we move up the tree.
           try {
               Map serviceResult = getServiceAttributes(token, nextType,
                   nextName, serviceName, missingAttr, nextAmOrgName,
                   nextAmsdkDN);
               if (getDebug().messageEnabled()) {
                   getDebug().message("IdServicesImpl."
                       + "getServiceAttributesAscending:"
                       + " nextType=" + nextType + "; nextName=" + nextName
                       + "; serviceName=" + serviceName + "; missingAttr="
                       + missingAttr + "; nextAmOrgName=" + nextAmOrgName
                       + "; nextAmsdkDN=" + nextAmsdkDN);
                   getDebug().message("  getServiceAttributesAscending: "
                       + "serviceResult=" + serviceResult);
                   getDebug().message("  getServiceAttributesAscending: "
                       + " finalResult=" + finalResult);
                   getDebug().message("  getServiceAttributesAscending: "
                       + " finalAttrName=" + finalAttrName);
               }
               if (serviceResult != null) {
                   Set srvNameReturned = serviceResult.keySet();
                   // save the newly found attrs
                   // amsdk returns emptyset when attrname is not present.
                   Iterator nameIt = srvNameReturned.iterator();
                   while (nameIt.hasNext()) {
                       String attr = (String) nameIt.next();
                       Set attrValue = (Set) serviceResult.get(attr);
                       if (!attrValue.isEmpty()) {
                           finalResult.put(attr, attrValue);
                           finalAttrName.add(attr);
                       }
                   }
                   if (getDebug().messageEnabled()) {
                       getDebug().message("    getServiceAttributesAscending:"
                          + " serviceResult=" + serviceResult);
                       getDebug().message("    getServiceAttributesAscending:"
                         + " finalResult=" + finalResult);
                   }
               }
               if (finalAttrName.containsAll(attrNames)) {
                   if (getDebug().messageEnabled()) {
                       getDebug().message("exit getServiceAttributesAscending:"
                           + " finalResult=" + finalResult);
                   }
                   return(finalResult);
               }

               // find the missing attributes
               missingAttr.clear();
               Iterator it = attrNames.iterator();
               while (it.hasNext()) {
                   String attrName = (String) it.next();
                   if (!finalAttrName.contains(attrName)) {
                       missingAttr.add(attrName);
                   }
               }
           } catch (IdRepoException idrepo) {
               if (getDebug().warningEnabled()) {
                   getDebug().warning("  getServiceAttributesAscending: "
                       + "idrepoerr", idrepo);
               }
           } catch (SSOException ssoex) {
               if (getDebug().warningEnabled()) {
                   getDebug().warning("  getServiceAttributesAscending: "
                       + "ssoex", ssoex);
               }
           }

           //  go up to the parent org
           try {

               if (nextType.equals(IdType.USER) ||
                   nextType.equals(IdType.AGENT)) {
                   // try the user or agent's currect realm.
                   nextAmsdkDN = nextAmOrgName;
                   nextType = IdType.REALM;
               } else {
                   OrganizationConfigManager ocm =
                       new OrganizationConfigManager(token, nextAmOrgName);
                   OrganizationConfigManager parentOCM =
                       ocm.getParentOrgConfigManager();
                   String tmpParentName = parentOCM.getOrganizationName();
                   String parentName = DNMapper.realmNameToAMSDKName(
                       tmpParentName);
                   if (getDebug().messageEnabled()) {
                       getDebug().message("  getServiceAttributesAscending: "
                           + " tmpParentName=" + tmpParentName
                           + " parentName=" + parentName);
                   }
                   nextType = IdType.REALM;
                   if (nextAmOrgName.equalsIgnoreCase(parentName)) {
                       // at root.
                       nextName = null;
                   } else {
                       nextAmOrgName = parentName;
                   }
                   nextAmOrgName = parentName;
                   nextAmsdkDN = parentName;
               }
           } catch (SMSException smse) {
               if (getDebug().warningEnabled()) {
                   getDebug().warning("  getServiceAttributesAscending: "
                       + "smserror", smse);
               }
               nextName = null;
           }
       } while (nextName != null);

       // get the rest from global.
       if (!missingAttr.isEmpty()) {
           try {
               ServiceSchemaManager ssm =
                   new ServiceSchemaManager(serviceName, token);
               ServiceSchema schema = ssm.getDynamicSchema();
               Map gAttrs = schema.getAttributeDefaults();
               Iterator missingIt = missingAttr.iterator();
               while (missingIt.hasNext()) {
                   String missingAttrName = (String) missingIt.next();
                   finalResult.put(missingAttrName,
                       gAttrs.get(missingAttrName));
               }
           } catch (SMSException smse) {
               if (getDebug().messageEnabled()) {
                   getDebug().message(
                       "IdServicesImpl(): getServiceAttributeAscending "
                       + " Failed to get global default.", smse);
               }
           }
       }

       if (getDebug().messageEnabled()) {
           getDebug().message("exit end  getServiceAttributesAscending: "
               + " finalResult=" + finalResult);
       }
       return finalResult;
   }

   public Map getServiceAttributes(SSOToken token, IdType type, String name,
       String serviceName, Set attrNames, String amOrgName, String amsdkDN)
       throws IdRepoException, SSOException {
       return (getServiceAttributes(token, type, name, serviceName,
           attrNames, amOrgName, amsdkDN, true));
   }

   public Map getBinaryServiceAttributes(SSOToken token, IdType type,
       String name, String serviceName, Set attrNames, String amOrgName,
       String amsdkDN) throws IdRepoException, SSOException {
       return (getServiceAttributes(token, type, name, serviceName,
           attrNames, amOrgName, amsdkDN, false));
   }

   public Map getServiceAttributes(SSOToken token, IdType type, String name,
           String serviceName, Set attrNames, String amOrgName,
           String amsdkDN, boolean isString)
           throws IdRepoException, SSOException {

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, attrNames, IdOperation.READ,
               type);

       // First get the list of plugins that support the create operation.
       // use IdOperation.READ insteadof IdOperation.SERVICE. IdRepo for
       // AD doesn't support SERVICE because service object classes can't
       // exist in user entry. So IdRepo.getServiceAttributes won't get
       // user attributes. But IdRepo.getServiceAttributes will also read
       // realm service attributes. We should move the code that reads
       // ealm service attributes in IdRepo.getServiceAttributes to this class
       // later. Only after that we can use IdOperation.SERVICE.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.READ, type);
       if (configuredPluginClasses == null
               || configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       IdRepo idRepo = null;
       Set resultsSet = new HashSet();
       IdRepoException origEx = null;
       while (it.hasNext()) {
           IdRepo repo = (IdRepo) it.next();
           Map cMap = repo.getConfiguration();
           try {
               Map attrs = null;
               if (repo.getClass().getName().equals(IdConstants.AMSDK_PLUGIN)
                       && amsdkDN != null) {
                   attrs = (isString ?
                       repo.getServiceAttributes(token, type, amsdkDN,
                           serviceName, attrNames) :
                       repo.getBinaryServiceAttributes(token, type, amsdkDN,
                           serviceName, attrNames));
               } else {
                   attrs = (isString ?
                       repo.getServiceAttributes(token, type, name,
                           serviceName, attrNames) :
                       repo.getBinaryServiceAttributes(token, type, name,
                           serviceName, attrNames));
               }
               attrs = reverseMapAttributeNames(attrs, cMap);
               resultsSet.add(attrs);
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().messageEnabled()) {
                   getDebug().message(
                       "IdServicesImpl.getServiceAttributes: "
                       + "Services not supported for repository "
                       + repo.getClass().getName()
                       + " :: " + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error(
                   "IdServicesImpl.getServiceAttributes: Fatal Exception ",
                   idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.getServiceAttributes: "
                       + "Unable to get service "
                       + "attributes for the repository "
                       + idRepo.getClass().getName()
                       + " :: " + ide.getMessage());
               }
               noOfSuccess--;
               origEx = (origEx == null) ? ide :origEx;
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.getServiceAttributes: "
                   + "Unable to get service attributes for identity "
                   + type.getName() + "::" + name
                   + " in any configured data store", origEx);
           }
           throw origEx;
       } else {
           Map resultsMap = combineAttrMaps(resultsSet, isString);
           return resultsMap;
       }

   }

   public void modifyService(SSOToken token, IdType type, String name,
           String serviceName, SchemaType stype, Map attrMap,
           String amOrgName, String amsdkDN) throws IdRepoException,
           SSOException {

       // Check permission first. If allowed then proceed, else the
       // checkPermission method throws an "402" exception.
       checkPermission(token, amOrgName, name, attrMap.keySet(),
               IdOperation.SERVICE, type);
       // Get the list of plugins that support the service operation.
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName,
           IdOperation.SERVICE, type);
       if ((configuredPluginClasses == null) ||
           configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       int noOfSuccess = configuredPluginClasses.size();
       IdRepo idRepo = null;
       while (it.hasNext()) {
           IdRepo repo = (IdRepo) it.next();
           Map cMap = repo.getConfiguration();
           try {
               Map mappedAttributes = mapAttributeNames(attrMap, cMap);
               if (repo.getClass().getName().equals(IdConstants.AMSDK_PLUGIN)
                       && amsdkDN != null) {
                   repo.modifyService(token, type, amsdkDN, serviceName,
                           stype, mappedAttributes);
               } else {
                   repo.modifyService(token, type, name, serviceName, stype,
                           mappedAttributes);
               }
           } catch (IdRepoUnsupportedOpException ide) {
               if (idRepo != null && getDebug().messageEnabled()) {
                   getDebug().message("IdServicesImpl.modifyService: "
                           + "Modify Services not supported for repository "
                           + repo.getClass().getName()
                           + " :: " + ide.getMessage());
               }
               noOfSuccess--;
           } catch (IdRepoFatalException idf) {
               // fatal ..throw it all the way up
               getDebug().error(
                   "IdServicesImpl.modifyService: Fatal Exception ", idf);
               throw idf;
           } catch (IdRepoException ide) {
               if (idRepo != null && getDebug().warningEnabled()) {
                   getDebug().warning(
                       "IdServicesImpl.modifyService: "
                       + "Unable to modify service in the "
                       + "following repository "
                       + idRepo.getClass().getName() + " :: "
                       + ide.getMessage());
               }   
               noOfSuccess--;
           }
       }
       if (noOfSuccess == 0) {
           if (getDebug().warningEnabled()) {
               getDebug().warning(
                   "IdServicesImpl.modifyService: "
                   + "Unable to modify service attributes for identity "
                   + type.getName() + "::" + name
                   + " in any configured data store");
           }
           Object[] args = { IdOperation.SERVICE.toString() };
           throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                   "302", args);
       }
   }

   public Set getSupportedTypes(SSOToken token, String amOrgName)
           throws IdRepoException, SSOException {
       Set unionSupportedTypes = new HashSet();
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName);
       if (configuredPluginClasses == null
               || configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       while (it.hasNext()) {
           IdRepo repo = (IdRepo) it.next();
           Set supportedTypes = repo.getSupportedTypes();
           if (supportedTypes != null && !supportedTypes.isEmpty()) {
               unionSupportedTypes.addAll(supportedTypes);
           }
       }
       // Check if the supportedTypes is defined as supported in
       // the global schema.
       unionSupportedTypes.retainAll(IdUtils.supportedTypes);
       return unionSupportedTypes;
   }

   public Set getSupportedOperations(SSOToken token, IdType type,
           String amOrgName) throws IdRepoException, SSOException {

       // First get the list of plugins that support the create operation.
       Set unionSupportedOps = new HashSet();
       Set configuredPluginClasses = idrepoCache.getIdRepoPlugins(amOrgName);
       if (configuredPluginClasses == null
               || configuredPluginClasses.isEmpty()) {
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "301", null);
       }

       Iterator it = configuredPluginClasses.iterator();
       while (it.hasNext()) {
           IdRepo repo = (IdRepo) it.next();
           if (repo instanceof SpecialRepo) {
               continue;
           }
           Set supportedOps = repo.getSupportedOperations(type);
           if (supportedOps != null && !supportedOps.isEmpty()) {
               unionSupportedOps.addAll(supportedOps);
           }
       }
       return unionSupportedOps;
   }

   private Map combineAttrMaps(Set setOfMaps, boolean isString) {
       Map resultMap = new AMHashMap(!isString);
       Iterator it = setOfMaps.iterator();
       while (it.hasNext()) {
           Map currMap = (Map) it.next();
           if (currMap != null) {
               Iterator keyset = currMap.keySet().iterator();
               while (keyset.hasNext()) {
                   String thisAttr = (String) keyset.next();
                   if (isString) {
                       Set resultSet = (Set) resultMap.get(thisAttr);
                       Set thisSet = (Set) currMap.get(thisAttr);
                       if (resultSet != null) {
                           resultSet.addAll(thisSet);
                       } else {
                           /*
                            * create a new Set so that we do not alter the set
                            * that is referenced in setOfMaps
                            */
                           resultSet = new HashSet((Set) 
                                   currMap.get(thisAttr));
                           resultMap.put(thisAttr, resultSet);
                       }
                   } else { // binary attributes

                       byte[][] resultSet = (byte[][]) resultMap.get(thisAttr);
                       byte[][] thisSet = (byte[][]) currMap.get(thisAttr);
                       int combinedSize = thisSet.length;
                       if (resultSet != null) {
                           combinedSize = resultSet.length + thisSet.length;
                           byte[][] tmpSet = new byte[combinedSize][];
                           for (int i = 0; i < resultSet.length; i++) {
                               tmpSet[i] = (byte[]) resultSet[i];
                           }
                           for (int i = 0; i < thisSet.length; i++) {
                               tmpSet[i] = (byte[]) thisSet[i];
                           }
                           resultSet = tmpSet;
                       } else {
                           resultSet = (byte[][]) thisSet.clone();
                       }
                       resultMap.put(thisAttr, resultSet);

                   }

               }
           }
       }
       return resultMap;
   }

   private Map mapAttributeNames(Map attrMap, Map configMap) {
       if (attrMap == null || attrMap.isEmpty()) {
           return attrMap;
       }
       Map resultMap;
       Map[] mapArray = getAttributeNameMap(configMap);
       if (mapArray == null) {
           resultMap = attrMap;
       } else {
           resultMap = new CaseInsensitiveHashMap();
           Map forwardMap = mapArray[0];
           Iterator it = attrMap.keySet().iterator();
           while (it.hasNext()) {
               String curr = (String) it.next();
               if (forwardMap.containsKey(curr)) {
                   resultMap.put((String) forwardMap.get(curr), (Set) attrMap
                           .get(curr));
               } else {
                   resultMap.put(curr, (Set) attrMap.get(curr));
               }
           }
       }
       return resultMap;
   }

   private Set mapAttributeNames(Set attrNames, Map configMap) {
       if (attrNames == null || attrNames.isEmpty()) {
           return attrNames;
       }
       Map[] mapArray = getAttributeNameMap(configMap);
       Set resultSet;
       if (mapArray == null) {
           resultSet = attrNames;
       } else {
           resultSet = new CaseInsensitiveHashSet();
           Map forwardMap = mapArray[0];
           Iterator it = attrNames.iterator();
           while (it.hasNext()) {
               String curr = (String) it.next();
               if (forwardMap.containsKey(curr)) {
                   resultSet.add((String) forwardMap.get(curr));
               } else {
                   resultSet.add(curr);
               }
           }
       }
       return resultSet;
   }

   private Map reverseMapAttributeNames(Map attrMap, Map configMap) {
       if (attrMap == null || attrMap.isEmpty()) {
           return attrMap;
       }
       Map resultMap;
       Map[] mapArray = getAttributeNameMap(configMap);
       if (mapArray == null) {
           resultMap = attrMap;
       } else {
           resultMap = new CaseInsensitiveHashMap();
           Map reverseMap = mapArray[1];
           Iterator it = attrMap.keySet().iterator();
           while (it.hasNext()) {
               String curr = (String) it.next();
               if (reverseMap.containsKey(curr)) {
                   resultMap.put((String) reverseMap.get(curr), attrMap
                           .get(curr));
               } else {
                   resultMap.put(curr, attrMap.get(curr));
               }
           }
       }
       return resultMap;
   }

   private Set combineMembers(SSOToken token, Set membersSet, IdType type,
           String orgName, boolean amsdkIncluded, Set amsdkMemberships) {
       Set results = new HashSet();
       Map resultsMap = new CaseInsensitiveHashMap();
       if (amsdkIncluded) {
           if (amsdkMemberships != null) {
               Iterator it = amsdkMemberships.iterator();
               while (it.hasNext()) {
                   String m = (String) it.next();
                   String mname = DNUtils.DNtoName(m);
                   AMIdentity id = new AMIdentity(token, mname, type, orgName,
                           m);
                   results.add(id);
                   resultsMap.put(mname, id);
               }
           }
       }
       Iterator miter = membersSet.iterator();
       while (miter.hasNext()) {
           Set first = (Set) miter.next();
           if (first == null) {
               continue;
           }
           Iterator it = first.iterator();
           while (it.hasNext()) {
               String m = (String) it.next();
               String mname = DNUtils.DNtoName(m);
               // add to results, if not already there!
               if (!resultsMap.containsKey(mname)) {
                   AMIdentity id = new AMIdentity(token, mname, type, orgName,
                           null);
                   results.add(id);
                   resultsMap.put(mname, id);
               }
           }
       }
       return results;
   }

   private IdSearchResults combineSearchResults(SSOToken token,
           Object[][] arrayOfResult, int sizeOfArray, IdType type,
           String orgName, boolean amsdkIncluded, Object[][] amsdkResults) {
       Map amsdkDNs = new CaseInsensitiveHashMap();
       Map resultsMap = new CaseInsensitiveHashMap();
       int errorCode = IdSearchResults.SUCCESS;
       if (amsdkIncluded) {
           RepoSearchResults amsdkRepoRes = (RepoSearchResults) 
               amsdkResults[0][0];

           Set results = amsdkRepoRes.getSearchResults();
           Map attrResults = amsdkRepoRes.getResultAttributes();
           Iterator it = results.iterator();
           while (it.hasNext()) {
               String dn = (String) it.next();
               String name =  LDAPDN.explodeDN(dn, true)[0];
               amsdkDNs.put(name, dn);
               Set attrMaps = new HashSet();
               attrMaps.add((Map) attrResults.get(dn));
               resultsMap.put(name, attrMaps);
           }
           errorCode = amsdkRepoRes.getErrorCode();
       }
       for (int i = 0; i < sizeOfArray; i++) {
           RepoSearchResults current = (RepoSearchResults) arrayOfResult[i][0];
           Map configMap = (Map) arrayOfResult[i][1];
           Iterator it = current.getSearchResults().iterator();
           Map allAttrMaps = current.getResultAttributes();
           while (it.hasNext()) {
               String m = (String) it.next();
               String mname = DNUtils.DNtoName(m, false);
               Map attrMap = (Map) allAttrMaps.get(m);
               attrMap = reverseMapAttributeNames(attrMap, configMap);
               Set attrMaps = (Set) resultsMap.get(mname);
               if (attrMaps == null) {
                   attrMaps = new HashSet();
               }
               attrMaps.add(attrMap);
               resultsMap.put(mname, attrMaps);
           }
       }
       IdSearchResults results = new IdSearchResults(type, orgName);
       Iterator it = resultsMap.keySet().iterator();
       while (it.hasNext()) {
           String mname = (String) it.next();
           Map combinedMap = combineAttrMaps((Set) resultsMap.get(mname), 
                   true);
           AMIdentity id = new AMIdentity(token, mname, type, orgName,
                   (String) amsdkDNs.get(mname));
           results.addResult(id, combinedMap);
       }
       results.setErrorCode(errorCode);
       return results;
   }

   private Map[] getAttributeNameMap(Map configMap) {
       Set attributeMap = (Set) configMap.get(IdConstants.ATTR_MAP);

       if (attributeMap == null || attributeMap.isEmpty()) {
           return null;
       } else {
           Map returnArray[] = new Map[2];
           int size = attributeMap.size();
           returnArray[0] = new CaseInsensitiveHashMap(size);
           returnArray[1] = new CaseInsensitiveHashMap(size);
           Iterator it = attributeMap.iterator();
           while (it.hasNext()) {
               String mapString = (String) it.next();
               int eqIndex = mapString.indexOf('=');
               if (eqIndex > -1) {
                   String first = mapString.substring(0, eqIndex);
                   String second = mapString.substring(eqIndex + 1);
                   returnArray[0].put(first, second);
                   returnArray[1].put(second, first);
               } else {
                   returnArray[0].put(mapString, mapString);
                   returnArray[1].put(mapString, mapString);
               }
           }
           return returnArray;
       }
   }

   private boolean checkPermission(SSOToken token, String realm, String name,
           Set attrs, IdOperation op, IdType type) throws IdRepoException,
           SSOException {
       if (!ServiceManager.isConfigMigratedTo70()) {
           // Config not migrated to 7.0 which means this is
           // in coexistence mode. Do not perform any delegation check
           return true;
       }
       Set thisAction = null;
       if (op.equals(IdOperation.READ)) {
           // thisAction = readAction;
           // TODO This is a temporary fix where-in all users are
           // being allowed read permisions, till delegation component
           // is fixed to support "user self read" operations
           thisAction = READ_ACTION;
       } else {
           thisAction = WRITE_ACTION;
       }
       try {
           DelegationEvaluator de = new DelegationEvaluator();
           String resource = type.getName();
           if (name != null) {
               resource += "/" + name;
           }
           DelegationPermission dp = new DelegationPermission(realm,
                   IdConstants.REPO_SERVICE, "1.0", "application", resource,
                   thisAction, Collections.EMPTY_MAP);
           Map envMap = Collections.EMPTY_MAP;
           if (attrs != null) {
               envMap = new HashMap();
               envMap.put(DELEGATION_ATTRS_NAME, attrs);
           }
           if (!de.isAllowed(token, dp, envMap)) {
               Object[] args = { op.getName(), token.getPrincipal().getName() 
                       };
               throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "402",
                       args);
           }
           return true;

       } catch (DelegationException dex) {
           getDebug().error("IdServicesImpl.checkPermission " +
               "Got Delegation Exception: ", dex);
           Object[] args = { op.getName(), token.getPrincipal().getName() };
           throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "402", args);
       }
   }

   protected void clearSpecialIdentityCache() {
       specialIdentityNames = null;
       specialIdentities = null;
   }

   public void clearIdRepoPlugins() {
       idrepoCache.clearIdRepoPluginsCache();
}

   public void clearIdRepoPlugins(String orgName, String serviceComponent,
       int type) {
       idrepoCache.clearIdRepoPluginsCache();
   }

   public void reloadIdRepoServiceSchema() {
       idrepoCache.clearIdRepoPluginsCache();
   }
}
