/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2019 Open Identity Platform Community.
 */

package org.openidentityplatform.openam.cassandra;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.utils.CrestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SchemaType;

public class Repo extends IdRepo {
	final static Logger logger=LoggerFactory.getLogger(Repo.class.getName());
	
	final static Logger stat=LoggerFactory.getLogger(Repo.class.getName()+".stat");
	
	final Map<IdType,Set<IdOperation>> supportedOps = new HashMap<IdType,Set<IdOperation>>();
	final Map<IdType,String> type2table=new HashMap<IdType, String>();
	final Map<IdType,Map<String,Integer>> type2attr2ttl=new HashMap<IdType, Map<String,Integer>>();
	
	public Session session=null;
	String activeAttr="inetuserstatus";
	String activeValue="Active";
	String keyspace="test";
	String memberOf="memberOf";
	@Override
	public void initialize(Map<String, Set<String>> configParams) throws IdRepoException  {
		super.initialize(configParams);
		
		try{
			activeAttr=CollectionHelper.getMapAttr(configParams, "sun-idrepo-ldapv3-config-isactive","inetuserstatus");
			activeValue=CollectionHelper.getMapAttr(configParams, "sun-idrepo-ldapv3-config-active","Active");
			memberOf=CollectionHelper.getMapAttr(configParams, "sun-idrepo-ldapv3-config-memberof","memberOf");
	//		sunIdRepoSupportedOperations=[realm=read,create,edit,delete,service, user=read,create,edit,delete,service, group=read,create,edit,delete],
			supportedOps.clear();
			Set<String> sunIdRepoSupportedOperations=((Map<String, Set<String>>)configParams).get("sunIdRepoSupportedOperations");
			for (String str : sunIdRepoSupportedOperations!=null?sunIdRepoSupportedOperations:new HashSet<String>(Arrays.asList(new String[]{"group=read,create,edit,delete","realm=read,create,edit,delete,service","user=read,create,edit,delete,service"}))){ 
				String[] split=str.split("\\=");
				if (split.length>1){
					IdType idType=String2IdType(split[0]);
					if (!supportedOps.containsKey(idType))
						supportedOps.put(idType, new HashSet<IdOperation>());
					for (String strOp : split[1].split(",")) 
						supportedOps.get(idType).add(String2IdOperation(strOp));
				}
			}
			
	//		sunIdRepoAttributeMapping=[user=user, group=group, realm=realm], 
			type2table.clear();
			Set<String> sunIdRepoAttributeMapping=((Map<String, Set<String>>)configParams).get("sunIdRepoAttributeMapping");
			for (String str : sunIdRepoAttributeMapping!=null?sunIdRepoAttributeMapping:new HashSet<String>(Arrays.asList(new String[]{"group=group","realm=realm","user=user"}))){ 
				String[] split=str.split("\\=");
				if (split.length>1)
					type2table.put(String2IdType(split[0]), split[1]);
			}
			
	//		sun-idrepo-ldapv3-config-user-attributes=[group:attr3=86400, realm:attr1=86400, user:attr2=86400], 
			type2attr2ttl.clear();
			if (configParams.get("sun-idrepo-ldapv3-config-user-attributes")!=null)
				for (String str : ((Map<String, Set<String>>)configParams).get("sun-idrepo-ldapv3-config-user-attributes")){ 
					String[] split=str.split("\\=");
					if (split.length>1){
						String[] split2=split[0].split(":");
						IdType idType=String2IdType(split2[0]);
						if (!type2attr2ttl.containsKey(idType))
							type2attr2ttl.put(idType, new TreeMap<String,Integer>(String.CASE_INSENSITIVE_ORDER));
						type2attr2ttl.get(idType).put(split2[1], Integer.parseInt(split[1]));
					}
				}
			keyspace=CollectionHelper.getMapAttr(configParams, "sun-idrepo-ldapv3-config-organization_name","test");
			Cluster cluster=ClusterCache.getCluster(
					((Map<String, Set<String>>)configParams).get("sun-idrepo-ldapv3-config-ldap-server").toArray(new String[0]), 
					CollectionHelper.getMapAttr(configParams, "sun-idrepo-ldapv3-config-authid","cassandra"), 
					CollectionHelper.getMapAttr(configParams, "sun-idrepo-ldapv3-config-authpw","cassandra"));
			session=cluster.connect(keyspace);
		}catch(Exception e){
			logger.error("error",e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void shutdown() {
		super.shutdown();
		if (session!=null && !session.isClosed())
			session.close();
	}
	
	@Override
	public Set<IdOperation> getSupportedOperations(IdType type) {
		return (Set<IdOperation>) supportedOps.get(type);
	}

	@Override
	public Set<IdType> getSupportedTypes() {
		return supportedOps.keySet();
	}
	
	@Override
	public boolean isExists(SSOToken token, IdType type, String name) throws IdRepoException, SSOException {
		validate(type, IdOperation.READ);
		Map<String, Set<String>> attr=getAttributes(token, type, name,null);
		return attr!=null && (attr.size()!=0);
	}

	@Override
	public boolean isActive(SSOToken token, IdType type, String name) throws IdRepoException, SSOException {
		Map<String, Set<String>> attr=getAttributes(token, type, name,new HashSet<String>(Arrays.asList(new String[]{activeAttr})));
		Set<String> value=attr.get(activeAttr);
		return value!=null && (value.size()==0||value.contains(activeValue));
	}
	
	@Override
	public void setActiveStatus(SSOToken token, IdType type, String name,boolean active) throws IdRepoException, SSOException {
		validate(type, IdOperation.EDIT);
		final Map<String, Set<String>> attributes=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		attributes.put(activeAttr, new HashSet<String>(Arrays.asList(new String[]{active?activeValue:"In".concat(activeValue)})));
		setAttributes(token, type, name, attributes, false);
	}

	@Override
	public Map<String, Set<String>> getAttributes(SSOToken token, IdType type, String name) throws IdRepoException, SSOException {
		validate(type, IdOperation.READ);
		return getAttributes(token, type, name, null);
	}

	@Override
	public Map<String, Set<String>> getAttributes(SSOToken token, IdType type,String name, Set<String> attrNames) throws IdRepoException, SSOException {
		validate(type, IdOperation.READ);
		final Map<String, Set<String>> attr=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		try{
			Where select=QueryBuilder.select().from(keyspace,"values")
				.where(QueryBuilder.eq("type", type.getName()))
				.and(QueryBuilder.eq("uid", name));
			if (attrNames!=null && !attrNames.isEmpty()) {
				final Set<String> fields=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
				for (String field : attrNames)
					fields.add(field.toLowerCase());
				select=select.and(QueryBuilder.in("field", fields));
			}
			
			final ResultSet rc=new ExecuteCallback(session,select).execute();
			for (Row row : rc){
				if (rc.getAvailableWithoutFetching() == (session.getCluster().getConfiguration().getQueryOptions().getFetchSize()-1) && !rc.isFullyFetched())
					rc.fetchMoreResults(); // this is asynchronous
				final String field=row.getString("field");
				final Set<String> values=attr.getOrDefault(field, new LinkedHashSet<String>(1));
				values.add(row.getString("value"));
				if (!attr.containsKey(field))
					attr.put(field, values);
			}
		}catch(Throwable e){
			logger.error("getAttributes {} {} {}",type,name,attrNames,e.getMessage());
			throw new IdRepoException(e.getMessage());
		}
		return attr;
	}

	@Override
	public Map<String, byte[][]> getBinaryAttributes(SSOToken token, IdType type, String name, Set<String> attrNames) throws IdRepoException, SSOException {
		//validate(type, IdOperation.READ);
		logger.warn("unsupported getBinaryAttributes {} {} {}",type,name,attrNames);
		throw new IdRepoUnsupportedOpException("unsupported getBinaryAttributes");
	}

	@Override
	public String create(SSOToken token, IdType type, String name, Map<String, Set<String>> attrMap) throws IdRepoException, SSOException {
		validate(type, IdOperation.CREATE);
		setAttributes(token, type, name, attrMap,true);
		return name;
	}

	@Override
	public void delete(SSOToken token, IdType type, String name) throws IdRepoException, SSOException {
		validate(type, IdOperation.DELETE);
		removeAttributes(token, type, name, null);
	}

	final static String asyncField="save.async";
	@Override
	public void setAttributes(SSOToken token, IdType type, String name, Map<String, Set<String>> attributes, boolean isAdd) throws IdRepoException, SSOException {
		validate(type, IdOperation.EDIT);
		try{
			final boolean async=(attributes.remove(asyncField)!=null);
					
			if (isAdd && !attributes.containsKey(activeAttr))
				attributes.put(activeAttr, new HashSet<String>(Arrays.asList(new String[]{activeValue})));
			
			for (final Entry<String, Set<String>>  entry: attributes.entrySet()) {
				if (!isAdd) {//remove old values
					final Statement update=QueryBuilder.delete().from(keyspace,"values")
							.where(QueryBuilder.eq("type", type.getName()))
							.and(QueryBuilder.eq("uid", name))
							.and(QueryBuilder.eq("field", entry.getKey().toLowerCase()));
					if (async)
						new ExecuteCallback(session, update).executeAsync();
					else
						new ExecuteCallback(session, update).execute();
					
				}
				final Integer ttl=getTTL(type, entry.getKey());
				for (final String  value: convert(entry.getKey(),entry.getValue())) {
					Statement update=QueryBuilder.update(keyspace,"values").with(QueryBuilder.set("change",new Date(System.currentTimeMillis())))
							.where(QueryBuilder.eq("type", type.getName()))
							.and(QueryBuilder.eq("uid", name))
							.and(QueryBuilder.eq("field", entry.getKey().toLowerCase()))
							.and(QueryBuilder.eq("value", value));
					if (ttl!=null && ttl>0)
						update=((com.datastax.driver.core.querybuilder.Update.Where)update).using(QueryBuilder.ttl(ttl));
					if (async)
						new ExecuteCallback(session, update).executeAsync();
					else
						new ExecuteCallback(session, update).execute();
				}
			}
			
		}catch(Throwable e){
			logger.error("setAttributes {} {} {} {}",type,name,attributes,isAdd,e.getMessage());
			throw new IdRepoException(e.getMessage());
		}
	}

	@Override
	public void setBinaryAttributes(SSOToken token, IdType type, String name,Map<String, byte[][]> attributes, boolean isAdd) throws IdRepoException, SSOException {
		//validate(type, IdOperation.EDIT);
		logger.warn("unsupported setBinaryAttributes {} {} {} {}",type,name,attributes,isAdd);
		throw new IdRepoUnsupportedOpException("unsupported setBinaryAttributes");
	}

	@Override
	public void removeAttributes(SSOToken token, IdType type, String name, Set<String> attrNames) throws IdRepoException, SSOException {
		validate(type, IdOperation.EDIT);
		try{
			final boolean async=(attrNames!=null && attrNames.remove(asyncField));
			
			final Set<String> deleteColums=new HashSet<String>();
			if (attrNames!=null)
				for (String field : attrNames){ 
					deleteColums.add(field.toLowerCase());
				}
			
			com.datastax.driver.core.querybuilder.Delete.Where statement=QueryBuilder.delete().from(keyspace,"values").where(QueryBuilder.eq("type", type.getName())).and(QueryBuilder.eq("uid", name));
			if (!deleteColums.isEmpty())
				statement=statement.and(QueryBuilder.in("field", deleteColums));
			if (async)
				new ExecuteCallback(session, statement).executeAsync();
			else
				new ExecuteCallback(session, statement).execute();
		}catch(Throwable e){
			logger.error("removeAttributes {} {} {}",type,name,attrNames,e.getMessage());
			throw new IdRepoException(e.getMessage());
		}
	}


	@Override
	public  RepoSearchResults search(SSOToken token, IdType type,
             CrestQuery crestQuery, int maxTime, int maxResults,
             Set<String> returnAttrs, boolean returnAllAttrs, int filterOp,
             Map<String, Set<String>> avPairs, boolean recursive) throws SSOException, IdRepoException {
		if (crestQuery.hasQueryId()) 
			return search(token, type, crestQuery.getQueryId(), maxTime, maxResults, returnAttrs,returnAllAttrs, filterOp, avPairs, recursive);
	     throw new IdRepoException("FilesRepo.search does not support queryFilter searches");
	}
	
	public RepoSearchResults search(SSOToken token, IdType type, String pattern, int maxTime, int maxResults, Set<String> returnAttrs, boolean returnAllAttrs, int filterOp,Map<String, Set<String>> avPairs, boolean recursive) throws IdRepoException, SSOException {
		validate(type, IdOperation.READ);
		try{
		//read returnFields
			final Set<String> attrNames=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			if (!returnAllAttrs || returnAttrs==null || returnAttrs.isEmpty()){
				if (returnAttrs==null) {
					returnAttrs=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
				}
				if (avPairs!=null) { //add filters column to output fields
					returnAttrs.addAll(avPairs.keySet());
				}
				for (String field : returnAttrs) { 
					attrNames.add(field.toLowerCase());
				}
			}
			
			Map<String, Map<String,Set<String>>> result=new HashMap<String, Map<String,Set<String>>>();
			
			//search by pattern/username without filter
			if  (avPairs==null || avPairs.isEmpty()) {
				Where select=QueryBuilder.select().from(keyspace,"values")
					.where(QueryBuilder.eq("type", type.getName()));
				if (!StringUtils.equals(pattern, "*")){
					select=select.and(QueryBuilder.eq("uid", pattern));
				}
				if (attrNames!=null && !attrNames.isEmpty()) {
					select=select.and(QueryBuilder.in("field", attrNames));
				}
				final ResultSet rc=new ExecuteCallback(session,select.limit(32000).allowFiltering()).execute();
				for (Row row : rc){
					if (rc.getAvailableWithoutFetching() == (session.getCluster().getConfiguration().getQueryOptions().getFetchSize()-1) && !rc.isFullyFetched())
						rc.fetchMoreResults(); // this is asynchronous
					final String uid=row.getString("uid");
					Map<String, Set<String>> attr=result.get(uid);
					if (attr==null) {
						attr=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
						result.put(uid,attr);
					}
					final String field=row.getString("field");
					Set<String> values=attr.get(field);
					if (values==null) {
						values=new LinkedHashSet<String>(1);
						attr.put(field, values);
					}
					values.add(row.getString("value"));
				}
			}
			
			//search by filters
			if (avPairs!=null) {
				for (final Entry<String, Set<String>> filterEntry : avPairs.entrySet()){
					if (session.getCluster().getMetadata().getKeyspace(keyspace).getMaterializedView("ix_".concat(filterEntry.getKey().toLowerCase()))==null) {
						throw new RuntimeException("CREATE MATERIALIZED VIEW IF NOT EXISTS ix_"+filterEntry.getKey().toLowerCase()+" AS SELECT * FROM values WHERE type IS NOT NULL and uid IS NOT NULL and field='"+filterEntry.getKey().toLowerCase()+"' and value IS NOT NULL PRIMARY KEY (type,field,value,uid)");
					}
					
					final Map<String, Map<String,Set<String>>> users2attr=new HashMap<String, Map<String,Set<String>>>();
					Where select=QueryBuilder.select().from(keyspace,"ix_".concat(filterEntry.getKey().toLowerCase()))
							.where(QueryBuilder.eq("type", type.getName()))
							.and(QueryBuilder.eq("field", filterEntry.getKey().toLowerCase()))
							.and(QueryBuilder.in("value", filterEntry.getValue()));
					if (!StringUtils.equals(pattern, "*"))
						select=select.and(QueryBuilder.eq("uid", pattern));
					final ResultSet rc=new ExecuteCallback(session,select.limit(32000)).execute();
					for (Row row : rc){
						if (rc.getAvailableWithoutFetching() == (session.getCluster().getConfiguration().getQueryOptions().getFetchSize()-1) && !rc.isFullyFetched())
							rc.fetchMoreResults(); // this is asynchronous
						final String uid=row.getString("uid");
						Map<String, Set<String>> attr=users2attr.get(uid);
						if (attr==null) {
							attr=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
							users2attr.put(uid,attr);
						}
						final String field=row.getString("field");
						Set<String> values=attr.get(field);
						if (values==null) {
							values=new LinkedHashSet<String>(1);
							attr.put(field, values);
						}
						values.add(row.getString("value"));
					}

					//join result
					if (result.isEmpty()) {
						result=users2attr;
					}else {
						for (final Entry<String, Map<String,Set<String>>> entry : users2attr.entrySet()) {
							final Map<String,Set<String>> attr=result.get(entry.getKey());
							if (attr==null && filterOp==Repo.AND_MOD) {
								continue;
							}else if (attr==null && filterOp==Repo.OR_MOD) {
								result.put(entry.getKey(), entry.getValue());
							}else if (attr!=null) {
								attr.putAll(entry.getValue());
							}
						}
						//reverse test
						if (filterOp==Repo.AND_MOD) {
							for (final String uid : new LinkedHashSet<String>(result.keySet())) {
								if(!users2attr.containsKey(uid)){
									result.remove(uid);
								}
							}
						}
					}
					//test result
					if (filterOp==Repo.AND_MOD && (result.isEmpty())) {
						logger.debug("break search by empty query {} {}: {}",pattern,avPairs,filterEntry);
						break;
					}
				}
			}
			return new RepoSearchResults(result.keySet(),(maxResults>0&&result.size()>maxResults)?RepoSearchResults.SIZE_LIMIT_EXCEEDED:RepoSearchResults.SUCCESS,result,type);
		}catch(Throwable e){e.printStackTrace();
			logger.error("search {} {} {} {} {} {} {} {} {}",type,pattern,maxTime,maxResults,returnAttrs,returnAllAttrs,filterOp,avPairs,recursive,e.getMessage());
			throw new IdRepoException(e.getMessage());
		}
	}

	@Override
	public void modifyMemberShip(SSOToken token, IdType type, String name, 	Set<String> members, IdType membersType, int operation) throws IdRepoException, SSOException {
		validate(type, IdOperation.EDIT);
		logger.warn("unsupported modifyMemberShip {} {} {} {} {}",type,name,members,membersType,operation);
		throw new IdRepoUnsupportedOpException("unsupported modifyMemberShip");
	}

	@Override
	public Set<String> getMembers(SSOToken token, IdType type, String name, IdType membersType) throws IdRepoException, SSOException {
		validate(type, IdOperation.READ);
		logger.warn("unsupported getMembers {} {} {} {}",type,name,membersType);
		throw new IdRepoUnsupportedOpException("unsupported getMembers");
	}

	@Override
	public Set<String> getMemberships(SSOToken token, IdType type, String name, IdType membershipType) throws IdRepoException, SSOException {
		validate(type, IdOperation.READ);
		getAttributes(token, type, name, new HashSet<String>(Arrays.asList(new String[]{memberOf})));
		Map<String, Set<String>> attr=getAttributes(token, type, name,new HashSet<String>(Arrays.asList(new String[]{memberOf})));
		return (attr!=null && attr.containsKey(memberOf)) ? attr.get(memberOf) : new HashSet<String>(0);
	}

	@Override
	public void assignService(SSOToken token, IdType type, String name, String serviceName, SchemaType stype, Map<String, Set<String>>  attrMap) throws IdRepoException,SSOException {
		validate(type, IdOperation.SERVICE);
		try{
			Map<String, Set<String>> attr=getAttributes(token, type, name, new HashSet<String>(Arrays.asList(new String[]{"serviceName"})));
			if (!attr.containsKey("serviceName"))
				attr.put("serviceName", new HashSet<String>(1));
			attr.get("serviceName").add(serviceName);
			attrMap.put("serviceName", attr.get("serviceName"));
			setAttributes(token, type, name, attrMap, true);
		}catch(Throwable e){
			logger.error("assignService {} {} {} {}",type,name,serviceName,attrMap,e.getMessage());
			throw new IdRepoException(e.getMessage());
		}
	}

	@Override
	public Set<String> getAssignedServices(SSOToken token, IdType type, String name, Map<String, Set<String>>  mapOfServicesAndOCs) 	throws IdRepoException, SSOException {
		validate(type, IdOperation.SERVICE);
		try{
			Map<String, Set<String>> attr=getAttributes(token, type, name, new HashSet<String>(Arrays.asList(new String[]{"serviceName"})));
			return (attr.containsKey("serviceName"))?attr.get("serviceName"):new HashSet<String>(0);
		}catch(Throwable e){
			logger.error("getAssignedServices {} {} {}",type,name,mapOfServicesAndOCs,e.getMessage());
			throw new IdRepoException(e.getMessage());
		}
	}

	@Override
	public void unassignService(SSOToken token, IdType type, String name, String serviceName, Map<String, Set<String>> attrMap) throws IdRepoException, SSOException {
		validate(type, IdOperation.SERVICE);
		try{
			Map<String, Set<String>> attr=getAttributes(token, type, name, new HashSet<String>(Arrays.asList(new String[]{"serviceName"})));
			if (!attr.containsKey("serviceName"))
				attr.put("serviceName", new HashSet<String>(0));
			attr.get("serviceName").remove(serviceName);
			attrMap.put("serviceName", attr.get("serviceName"));
			setAttributes(token, type, name, attrMap, false);
		}catch(Throwable e){
			logger.error("unassignService {} {} {} {}",type,name,serviceName,attrMap,e.getMessage());
			throw new IdRepoException(e.getMessage());
		}
	}

	@Override
	public Map<String, Set<String>> getServiceAttributes(SSOToken token, IdType type, String name, String serviceName, Set<String> attrNames) throws IdRepoException, SSOException {
		validate(type, IdOperation.SERVICE);
		return getAttributes(token, type, (type==IdType.REALM)?"ContainerDefaultTemplateRole":name,attrNames);
	}

	@Override
	public Map<String, byte[][]> getBinaryServiceAttributes(SSOToken token, IdType type, String name, String serviceName, Set<String> attrNames) throws IdRepoException, SSOException {
		//validate(type, IdOperation.SERVICE);
		logger.warn("unsupported getBinaryServiceAttributes {} {} {} {}",type,name,serviceName,attrNames);
		throw new IdRepoUnsupportedOpException("unsupported getBinaryServiceAttributes");
	}
	
	@Override
	public void modifyService(SSOToken token, IdType type, String name, String serviceName, SchemaType sType, Map<String, Set<String>> attrMap) throws IdRepoException,	SSOException {
		validate(type, IdOperation.SERVICE);
		try{
			Map<String, Set<String>> attr=getAttributes(token, type, name, new HashSet<String>(Arrays.asList(new String[]{"serviceName"})));
			if (!attr.containsKey("serviceName"))
				attr.put("serviceName", new HashSet<String>(1));
			attr.get("serviceName").add(serviceName);
			attrMap.put("serviceName", attr.get("serviceName"));
			setAttributes(token, type, name, attrMap, true);
		}catch(Throwable e){
			logger.error("modifyService {} {} {} {}",type,name,serviceName,attrMap,e.getMessage());
			throw new IdRepoException(e.getMessage());
		}
	}

	@Override
	public int addListener(SSOToken token, IdRepoListener listener) throws IdRepoException, SSOException {
		return 0;
	}

	@Override
	public void removeListener() {

	}
	
///////////////////////////////////////////////////////////////////////////	
	void validate(IdType type,IdOperation service) throws IdRepoUnsupportedOpException{
		if (!supportedOps.containsKey(type)||!supportedOps.get(type).contains(service))
			throw new IdRepoUnsupportedOpException("operation "+service.getName()+" not supported for "+type.getName());
	}
	
	Integer getTTL(IdType type,String name){
		Map<String, Integer> attr2ttl=type2attr2ttl.get(type);
		return (attr2ttl==null)?null:attr2ttl.get(name);
	}
	

	IdType String2IdType(String str){
		if (StringUtils.equalsIgnoreCase(IdType.USER.getName(),str))
			return IdType.USER;
		else if (StringUtils.equalsIgnoreCase(IdType.ROLE.getName(),str))
			return IdType.ROLE;
		else if (StringUtils.equalsIgnoreCase(IdType.REALM.getName(),str))
			return IdType.REALM;
		else if (StringUtils.equalsIgnoreCase(IdType.GROUP.getName(),str))
			return IdType.GROUP;
		else if (StringUtils.equalsIgnoreCase(IdType.FILTEREDROLE.getName(),str))
			return IdType.FILTEREDROLE;
		else if (StringUtils.equalsIgnoreCase(IdType.AGENTONLY.getName(),str))
			return IdType.AGENTONLY;
		else if (StringUtils.equalsIgnoreCase(IdType.AGENTGROUP.getName(),str))
			return IdType.AGENTGROUP;
		else if (StringUtils.equalsIgnoreCase(IdType.AGENT.getName(),str))
			return IdType.AGENT;
		else
			throw new IllegalArgumentException("unknown IdType="+str);
	}
	
	IdOperation String2IdOperation(String str){
		if (StringUtils.equalsIgnoreCase(IdOperation.CREATE.getName(),str))
			return IdOperation.CREATE;
		else if (StringUtils.equalsIgnoreCase(IdOperation.DELETE.getName(),str))
			return IdOperation.DELETE;
		else if (StringUtils.equalsIgnoreCase(IdOperation.EDIT.getName(),str))
			return IdOperation.EDIT;
		else if (StringUtils.equalsIgnoreCase(IdOperation.READ.getName(),str))
			return IdOperation.READ;
		else if (StringUtils.equalsIgnoreCase(IdOperation.SERVICE.getName(),str))
			return IdOperation.SERVICE;
		else
			throw new IllegalArgumentException("unknown IdOperation="+str);
	}
	
	Set<String> convert(String name, Set<String> values){
		if (StringUtils.containsIgnoreCase(name, "userpassword"))
			for (Object value : values.toArray()) 
				if (!value.toString().matches("\\{.+\\}.+"))
					try {
						values.add(SSHA.getSaltedPassword(value.toString().getBytes()));
						values.remove(value);
					} catch (NoSuchAlgorithmException e) {
						logger.error("convert",e);
					}
		return values;
	}
}
