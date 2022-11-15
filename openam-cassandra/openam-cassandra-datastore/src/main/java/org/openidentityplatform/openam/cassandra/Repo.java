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

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.utils.CrestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoDuplicateObjectException;
import com.sun.identity.idm.IdRepoErrorCode;
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
	final Set<String> disableCaseSensitive=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	
	final String profile="repo";
	public CqlSession session=null;
	String activeAttr="inetuserstatus";
	String activeValue="Active";
	String memberOf="memberOf";
	final String uniqueMember="uniqueMember";
	final static String created = "_created";
	final static String updated = "_updated";

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
					if (!supportedOps.containsKey(idType)) {
						supportedOps.put(idType, new HashSet<IdOperation>());
					}
					for (String strOp : split[1].split(",")) {
						supportedOps.get(idType).add(String2IdOperation(strOp));
					}
				}
			}
			
	//		sunIdRepoAttributeMapping=[user=user, group=group, realm=realm], 
			type2table.clear();
			Set<String> sunIdRepoAttributeMapping=((Map<String, Set<String>>)configParams).get("sunIdRepoAttributeMapping");
			for (String str : sunIdRepoAttributeMapping!=null?sunIdRepoAttributeMapping:new HashSet<String>(Arrays.asList(new String[]{"group=group","realm=realm","user=user"}))){ 
				String[] split=str.split("\\=");
				if (split.length>1) {
					type2table.put(String2IdType(split[0]), split[1]);
				}
			}
			
	//		sun-idrepo-ldapv3-config-user-attributes=[group:attr3=86400, realm:attr1=86400, user:attr2=86400], 
			type2attr2ttl.clear();
			if (configParams.get("sun-idrepo-ldapv3-config-user-attributes")!=null)
				for (String str : ((Map<String, Set<String>>)configParams).get("sun-idrepo-ldapv3-config-user-attributes")){ 
					String[] split=str.split("\\=");
					if (split.length>1){
						String[] split2=split[0].split(":");
						IdType idType=String2IdType(split2[0]);
						if (!type2attr2ttl.containsKey(idType)) {
							type2attr2ttl.put(idType, new TreeMap<String,Integer>(String.CASE_INSENSITIVE_ORDER));
						}
						type2attr2ttl.get(idType).put(split2[1], Integer.parseInt(split[1]));
					}
				}
			//disable-case-sensitive=mail,iplanet-am-user-alias-list
			disableCaseSensitive.clear();
			if (configParams.get("disable-case-sensitive")!=null) {
				for (String str : ((Map<String, Set<String>>)configParams).get("disable-case-sensitive")){ 
					disableCaseSensitive.add(str);
				}
			}else {
				disableCaseSensitive.addAll(Arrays.asList(new String[]{"uid","mail","iplanet-am-user-alias-list"}));
			}
				
			final String keyspace=CollectionHelper.getMapAttr(configParams, "sun-idrepo-ldapv3-config-organization_name","test");
			final String[] servers=((Map<String, Set<String>>)configParams).get("sun-idrepo-ldapv3-config-ldap-server").toArray(new String[0]);
			final String username=CollectionHelper.getMapAttr(configParams, "sun-idrepo-ldapv3-config-authid",null);
			final String password=CollectionHelper.getMapAttr(configParams, "sun-idrepo-ldapv3-config-authpw",null);
			
			logger.info("create session {}/{}",username,servers);
			CqlSessionBuilder builder=CqlSession.builder()
					.withApplicationName("OpenAM datastore: "+keyspace)
					.withConfigLoader(DriverConfigLoader.fromDefaults(Repo.class.getClassLoader()))
					.withKeyspace(keyspace);
			if (StringUtils.isNotBlank(username)&&StringUtils.isNotBlank(password)) {
				builder=builder.withAuthCredentials(username, password);
			}
			if (servers!=null && servers.length>0) {
				for (String address : servers) {
					try {
						builder=builder.addContactPoint(new InetSocketAddress(address,SystemProperties.getAsInt("cassandra.native_transport_port", 9042)));
					}catch (Throwable e) {
						logger.error("bad address {}: {}",address,e.getMessage());
					}
				}
			}
			session=builder.build();
			statement_select_by_type=session.prepare("select uid,field,value,change from values where type=:type limit 64000 allow filtering");
			statement_select_by_uid=session.prepare("select uid,field,value,change from values where type=:type and uid=:uid");
			statement_select_by_fields=session.prepare("select uid,field,value,change from values where type=:type and uid=:uid and field in :fields limit 64000");
			statement_select_created_updated_by_uid=session.prepare("select max(change) as updated, min(change) as created from values where type=:type and uid=:uid");
			statement_delete_by_uid=session.prepare("delete from values where type=:type and uid=:uid");
			statement_delete_by_fields=session.prepare("delete from values where type=:type and uid=:uid and field in :fields");
			statement_delete_by_field_value=session.prepare("delete from values where type=:type and uid=:uid and field=:field and value=:value");
			statement_add_value=session.prepare("insert into values (type,uid,field,value,change) values (:type,:uid,:field,:value,toTimestamp(now()))");
			statement_add_value_ttl=session.prepare("insert into values (type,uid,field,value,change) values (:type,:uid,:field,:value,toTimestamp(now())) using ttl :ttl");
			
			statement_add_value_exist=session.prepare("insert into values (type,uid,field,value,change) values (:type,:uid,:field,:value,toTimestamp(now())) IF NOT EXISTS");
			statement_add_value_ttl_exist=session.prepare("insert into values (type,uid,field,value,change) values (:type,:uid,:field,:value,toTimestamp(now())) IF NOT EXISTS using ttl :ttl");
		}catch(Exception e){
			logger.error("error",e);
			throw new RuntimeException(e);
		}
	}
	PreparedStatement statement_select_by_type;
	PreparedStatement statement_select_by_uid;
	PreparedStatement statement_select_by_fields;
	PreparedStatement statement_delete_by_uid;
	PreparedStatement statement_delete_by_fields;
	PreparedStatement statement_delete_by_field_value;
	PreparedStatement statement_add_value;
	PreparedStatement statement_add_value_ttl;
	PreparedStatement statement_add_value_exist;
	PreparedStatement statement_add_value_ttl_exist;
	PreparedStatement statement_select_created_updated_by_uid;
	
	@Override
	public void shutdown() {
		super.shutdown();
		if (session!=null && !session.isClosed()) {
			session.close();
		}
		session=null;
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
		final Map<String, Set<String>> attr=getAttributes(token, type, name,new HashSet<String>(Arrays.asList(new String[]{"uid"})));
		return attr!=null && attr.size()!=0;
	}

	@Override
	public boolean isActive(SSOToken token, IdType type, String name) throws IdRepoException, SSOException {
		final Map<String, Set<String>> attr=getAttributes(token, type, name,new HashSet<String>(Arrays.asList(new String[]{activeAttr})));
		final Set<String> value=attr.get(activeAttr);
		if (value!=null && !value.isEmpty() && value.contains(activeValue)) { //activeAttr is set and activeAttr==activeValue
			return true; 
		}else if ((value==null || value.isEmpty()) && isExists(token, type, name)){ //activeAttr is not set and isExists==true
			return true; 
		}
		return false;
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
		if (attrNames==null || attrNames.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("overhead: please set attrNames",new IllegalArgumentException());
			}
			assert (token==null): "overhead: please set attrNames";
		}
		final Map<String, Set<String>> attr= new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		try{
			boolean requestCreatedUpdatedAttrs = false;
			final Set<String> fields = new HashSet<>();
			if (attrNames!=null && !attrNames.isEmpty()) {
				for (String field : attrNames) {
					if(field.equals(created) || field.equals(updated)) { //ignore _created and _updated attrs, fetch later
						requestCreatedUpdatedAttrs = true;
						continue;
					}
					fields.add(field.toLowerCase().replace("update-", ""));
				}
			}
			if(requestCreatedUpdatedAttrs) {
				attr.putAll(getCreatedUpdatedAttributes(type, name, attrNames));
			}
			BoundStatement statement=((fields.isEmpty())?statement_select_by_uid:statement_select_by_fields).bind()
					.setString("type", type.getName())
					.setString("uid",  disableCaseSensitive.contains("uid")?name.toLowerCase():name);
			if (!fields.isEmpty()) {
				statement=statement.setList("fields",new ArrayList<String>(fields),String.class);
			}
			final ResultSet rc=new ExecuteCallback(profile,session,statement).execute();
			for (Row row : rc){
				final String field=row.getString("field");
				final Set<String> values=attr.getOrDefault(field, new LinkedHashSet<String>(1));
				values.add(row.getString("value"));
				if (!attr.containsKey(field)) {
					attr.put(field, values);
					if (attrNames!=null && attrNames.contains("update-"+field)) {
						attr.put("update-"+field, Collections.singleton(instantToString(row.getInstant("change"))));
					}
				}
			}
		}catch(Throwable e){
			logger.error("getAttributes {} {}",type,name,attrNames,e);
			throw new IdRepoException(e.getMessage());
		}
		return attr;
	}

    private String instantToString(Instant instant) {
        return Long.toString(instant.toEpochMilli());
    }

	private Map<String, Set<String>> getCreatedUpdatedAttributes(IdType type,String name, Set<String> attrNames) {
		final Map<String, Set<String>> attr = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		if(!attrNames.contains(created) && !attrNames.contains(updated)) {
			return attr;
		}
		BoundStatement statement=statement_select_created_updated_by_uid.bind()
				.setString("type", type.getName())
				.setString("uid",  disableCaseSensitive.contains("uid")?name.toLowerCase():name);
		final ResultSet rc=new ExecuteCallback(profile,session,statement).execute();
		for (Row row : rc) {
			if(attrNames.contains(created) && row.getInstant("created") != null) {
				attr.put(created, Collections.singleton(instantToString(row.getInstant("created"))));
			}
			if(attrNames.contains(updated)  && row.getInstant("updated") != null) {
				attr.put(updated, Collections.singleton(instantToString(row.getInstant("updated"))));
			}
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
	public void setAttributes(SSOToken token, IdType type, String name, Map<String, Set<String>> attributes_in, boolean isAdd) throws IdRepoException, SSOException {
		validate(type, IdOperation.EDIT);
		try{
			final Map<String, Set<String>> attributes=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
			if (attributes_in!=null) {
				attributes.putAll(attributes_in);
			}
			
			final boolean async=(attributes.remove(asyncField)!=null);
			
			if (!attributes.containsKey("uid")) { //Always create uid field 
				if (isAdd || !isExists(token, type, name)) {
					attributes.put("uid", new HashSet<String>(Arrays.asList(new String[]{name})));		
				}
			}
			if (!attributes.containsKey(activeAttr)) {
				if (isAdd || !isExists(token, type, name)) {
					attributes.put(activeAttr, new HashSet<String>(Arrays.asList(new String[]{activeValue})));
				}
			}
			if (isAdd) { //test if exist
				final Integer ttl=getTTL(type, "uid");
				final BoundStatement statement=((ttl!=null && ttl>0)?statement_add_value_ttl_exist:statement_add_value_exist).bind()
						.setString("type", type.getName())
						.setString("uid", disableCaseSensitive.contains("uid")?name.toLowerCase():name)
						.setString("field", "uid")
						.setString("value", disableCaseSensitive.contains("uid")?name.toLowerCase():name)
						;
				final ResultSet rc=new ExecuteCallback(profile,session, (ttl!=null && ttl>0)?statement.setInt("ttl", ttl):statement).execute();
				for (Row row : rc){
					if (!row.getBoolean("[applied]")) {
						throw IdRepoDuplicateObjectException.nameAlreadyExists(name);
					}
				}
				attributes.remove("uid");
			}
			BatchStatement statements=BatchStatement.newInstance(DefaultBatchType.LOGGED); 
			for (final Entry<String, Set<String>>  entry: attributes.entrySet()) {
				final Set<String> oldValues=new HashSet<String>();
				if (!isAdd) { //get old values
					BoundStatement statement=statement_select_by_fields.bind()
							.setString("type", type.getName())
							.setString("uid",  disableCaseSensitive.contains("uid")?name.toLowerCase():name)
							.setList("fields",Arrays.asList(new String[] {entry.getKey().toLowerCase()}),String.class);
					final ResultSet rc=new ExecuteCallback(profile,session,statement).execute();
					for (Row row : rc){
						oldValues.add(row.getString("value"));
					}
				}
				final Integer ttl=getTTL(type, entry.getKey());
				final Set<String> values=convert(entry.getKey(),entry.getValue());
				for (String  value: values) {
					if (disableCaseSensitive.contains(entry.getKey())) {
						value=value.toLowerCase();
					}
					if (!isAdd) { 
						if (oldValues.remove(value)) {//remove re-write value from delete
							if (!(ttl!=null && ttl>0)) { //Don't rewrite persistent value (without ttl)
								continue;
							}
						}
					}
					final BoundStatement statement=((ttl!=null && ttl>0)?statement_add_value_ttl:statement_add_value).bind()
							.setString("type", type.getName())
							.setString("uid", disableCaseSensitive.contains("uid")?name.toLowerCase():name)
							.setString("field", entry.getKey().toLowerCase())
							.setString("value", value)
							;
					statements=statements.add((ttl!=null && ttl>0)?statement.setInt("ttl", ttl):statement);
				}
				if (!isAdd) {//remove old values
					for (String value : oldValues) { 
						final BoundStatement statement=statement_delete_by_field_value.bind()
								.setString("type", type.getName())
								.setString("uid", disableCaseSensitive.contains("uid")?name.toLowerCase():name)
								.setString("field", entry.getKey().toLowerCase())
								.setString("value", value)
								;
						statements=statements.add(statement);
					}
				}
			}
			if (statements.size()>0) {
				if (async)
					new ExecuteCallback(profile,session, statements).executeAsync();
				else
					new ExecuteCallback(profile,session, statements).execute();
			}
		}catch (IdRepoException e) {
			throw e;
		}catch(Throwable e){
			logger.error("setAttributes {} {} {} {}",type,name,attributes_in,isAdd,e.getMessage());
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
			if (attrNames!=null) {
				for (String field : attrNames){ 
					deleteColums.add(field.toLowerCase());
				}
			}
			BoundStatement statement=((deleteColums.isEmpty())?statement_delete_by_uid:statement_delete_by_fields).bind()
					.setString("type", type.getName())
					.setString("uid", disableCaseSensitive.contains("uid")?name.toLowerCase():name)
					;
			if (!deleteColums.isEmpty()) {
				statement=statement.setList("fields",new ArrayList<String>(deleteColums),String.class);
			}
			if (async)
				new ExecuteCallback(profile,session, statement).executeAsync();
			else
				new ExecuteCallback(profile,session, statement).execute();
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
		if (!crestQuery.hasQueryId() && !crestQuery.hasQueryFilter()) {
			throw new IdRepoException("CassandraRepo.search does not support an empty search");
		}
		if(crestQuery.hasQueryFilter()) {
			if(avPairs == null) {
				avPairs = new HashMap<>();
			}
			CassandraFilter cassandraFilter = crestQuery.getQueryFilter().accept(new CassandraQueryFilterVisitor(), null);
            avPairs.putAll(cassandraFilter.getFilter());
            filterOp = cassandraFilter.getFilterOp();
		}
		return search(token, type, crestQuery.getQueryId(), maxTime, maxResults, returnAttrs,returnAllAttrs, filterOp, avPairs, recursive);
	}
	
	final String getIndexName(String field) {
		final String table="ix_".concat(field.toLowerCase()).replaceAll("-", "_");
		return table;
	}
	
	final Cache<String, PreparedStatement> indexByValue=CacheBuilder.newBuilder()
			.maximumSize(2048)
			.expireAfterAccess(5,TimeUnit.MINUTES)
			.build();
	final PreparedStatement getIndexByValue(String field) throws ExecutionException {
		final String table=getIndexName(field);
		return indexByValue.get(table,new Callable<PreparedStatement>() {
			@Override
			public PreparedStatement call() throws Exception {
				return session.prepare("select uid,field,value from "+table+" where type=:type and field=:field and value in :values limit 64000");
			}
		});
	}
	
	final Cache<String, PreparedStatement> indexByValueAndUID=CacheBuilder.newBuilder()
			.maximumSize(2048)
			.expireAfterAccess(5,TimeUnit.MINUTES)
			.build();
	final PreparedStatement getIndexByValueAndUID(String field) throws ExecutionException {
		final String table=getIndexName(field);
		return indexByValueAndUID.get(table,new Callable<PreparedStatement>() {
			@Override
			public PreparedStatement call() throws Exception {
				return session.prepare("select uid,field,value from "+table+" where type=:type and field=:field and value in :values and uid=:uid limit 64000");
			}
		});
	}
	
	public RepoSearchResults search(SSOToken token, IdType type, String pattern, int maxTime, int maxResults, Set<String> returnAttrs, boolean returnAllAttrs, int filterOp,Map<String, Set<String>> avPairs, boolean recursive) throws IdRepoException, SSOException {
		validate(type, IdOperation.READ);
		try{
			String filterUID=pattern;
			//returnFields
			final Set<String> returnAttrsNames=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			if (!returnAllAttrs && returnAttrs!=null){
				for (final String field : returnAttrs) { 
					returnAttrsNames.add(field.toLowerCase());
				}
			}
			//avPairs
			final Map<String, Set<String>> filterFields= new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			if (avPairs!=null) {
				for (final Entry<String, Set<String>> filterEntry: avPairs.entrySet()) {
					filterFields.put(filterEntry.getKey().toLowerCase(), filterEntry.getValue());
				}
			}

			
			Map<String, Map<String,Set<String>>> result= new HashMap<>();
			
			//search by pattern/username without filter
			if  (filterFields.isEmpty()) {
				BoundStatement statement;
				if (StringUtils.equals(filterUID, "*")){
					statement=statement_select_by_type.bind()
							.setString("type", type.getName());
				}else {
					statement=((returnAttrsNames.isEmpty())?statement_select_by_uid:statement_select_by_fields).bind()
							.setString("type", type.getName())
							.setString("uid", disableCaseSensitive.contains("uid")?filterUID.toLowerCase():filterUID);
					if (!returnAttrsNames.isEmpty()) {
						statement=statement.setList("fields",new ArrayList<String>(returnAttrsNames),String.class);
					}else{
						if (logger.isDebugEnabled()) {
							logger.debug("overhead: please set attrNames",new IllegalArgumentException());
						}
						assert (token==null): "overhead: please set attrNames";
					}
				}
				final ResultSet rc=new ExecuteCallback(profile,session,statement).execute();
				for (Row row : rc){
					final String uid=row.getString("uid");
					Map<String, Set<String>> attr=result.get(uid);
					if (attr==null) {
						attr= new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
						result.put(uid,attr);
					}
					final String field=row.getString("field");
					Set<String> values=attr.get(field);
					if (values==null) {
						values= new LinkedHashSet<>(1);
						attr.put(field, values);
					}
					values.add(row.getString("value"));
				}
			}else{ //search by filters
				for (final Entry<String, Set<String>> filterEntry : filterFields.entrySet()){
					final Map<String, Map<String,Set<String>>> users2attr= new HashMap<>();
					final Set<String> valuesLowerCase= new LinkedHashSet<>(filterEntry.getValue().size());
					if (disableCaseSensitive.contains(filterEntry.getKey())) {
						for (String string : filterEntry.getValue()) {
							valuesLowerCase.add(string.toLowerCase());
						}
					}
          try {
            BoundStatement statement=((StringUtils.equals(filterUID, "*") || filterUID == null)
                ? getIndexByValue(filterEntry.getKey())
                : getIndexByValueAndUID(filterEntry.getKey())).bind()
                  .setString("type", type.getName())
                  .setString("field", filterEntry.getKey().toLowerCase())
                  .setList("values", new ArrayList<>(
                      disableCaseSensitive.contains(filterEntry.getKey()) ? valuesLowerCase : filterEntry.getValue()),String.class);
            if (!StringUtils.equals(filterUID, "*") && filterUID != null) {
              statement=statement.setString("uid", disableCaseSensitive.contains("uid")?filterUID.toLowerCase():filterUID);
            }
            final ResultSet rc=new ExecuteCallback(profile,session,statement).execute();
            for (Row row : rc){
              final String uid=row.getString("uid");
              Map<String, Set<String>> attr=users2attr.get(uid);
              if (attr==null) {
                attr= new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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
					}catch(UncheckedExecutionException e) {
						logger.debug("unknown index {}: {} {}",session.getKeyspace().get(),filterEntry.getKey(),e.getCause().toString());
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
		}catch(Throwable e){
			logger.error("search {} {} {} {} {} {} {} {} {}: {}",type,pattern,maxTime,maxResults,returnAttrs,returnAllAttrs,filterOp,avPairs,recursive,logger.isDebugEnabled()?e:e.getMessage());
			e.printStackTrace();
			throw new IdRepoException(e.getMessage());
		}
	}

	@Override
	public void modifyMemberShip(SSOToken token, IdType type, String name, 	Set<String> members, IdType membersType, int operation) throws IdRepoException, SSOException {
		validate(type, IdOperation.EDIT);
		//check group
		final Boolean groupExist=isExists(token,type,name);
		
		if (ADDMEMBER==operation) {
			if (!groupExist) {
				throw new IdRepoException("group not exist");
			}
			if (members!=null) {
				//getMemberships
				for (String member :  new HashSet<String>(members)) {
					if (isExists(token, membersType, member)) {
						final Set<String> values=getMemberships(token, membersType, member, type);
						if (values.add(name)) {
							final Map<String, Set<String>> attr=new HashMap<String, Set<String>>(1);
							attr.put(memberOf, values);
							setAttributes(token, membersType, member, attr, false);
						}
					}else {
						members.remove(member);
					}
				}
				//getMembers
				final Set<String> values=getMembers(token, type, name, membersType);
				if (values.addAll(members)) {
					final Map<String, Set<String>> attr=new HashMap<String, Set<String>>(1);
					attr.put(uniqueMember, values);
					setAttributes(token, type, name, attr, false);
				}
			}
		}else {
			//getMembers
			if (groupExist) {
				final Set<String> values=getMembers(token, type, name, membersType);
				if (values.removeAll(members)) {
					final Map<String, Set<String>> attr=new HashMap<String, Set<String>>(1);
					attr.put(uniqueMember, values);
					setAttributes(token, type, name, attr, false);
				}
			}
			if (members!=null) {
				//getMemberships
				for (String member :  members) {
					if (isExists(token, membersType, member)) {
						final Set<String> values=getMemberships(token, membersType, member, type);
						if (values.remove(name)) {
							final Map<String, Set<String>> attr=new HashMap<String, Set<String>>(1);
							attr.put(memberOf, values);
							setAttributes(token, membersType, member, attr, false);
						}
					}
				}
			}
		}
	}

	@Override
	public Set<String> getMembers(SSOToken token, IdType type, String name, IdType membersType) throws IdRepoException, SSOException {
		validate(type, IdOperation.READ);
		final Map<String, Set<String>> attr=getAttributes(token, type, name,new HashSet<String>(Arrays.asList(new String[]{uniqueMember})));
		return (attr!=null && attr.containsKey(uniqueMember)) ? attr.get(uniqueMember) : new HashSet<String>(0);
	}

	@Override
	public Set<String> getMemberships(SSOToken token, IdType type, String name, IdType membershipType) throws IdRepoException, SSOException {
		validate(type, IdOperation.READ);
		final Map<String, Set<String>> attr=getAttributes(token, type, name,new HashSet<String>(Arrays.asList(new String[]{memberOf})));
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
			setAttributes(token, type, name, attrMap, false);
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
			setAttributes(token, type, name, attrMap, false);
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
		if (StringUtils.containsIgnoreCase(name, "userpassword")) {
			for (Object value : values.toArray()) {
				if (!value.toString().matches("\\{.+\\}.+")) {
					try {
						final Set<String> res=new HashSet<String>(values);
						res.remove(value);
						res.add(SSHA256.getSaltedPassword(value.toString().getBytes()));
						return res;
					} catch (NoSuchAlgorithmException e) {
						logger.error("convert",e);
					}
				}
			}
		}
		return values;
	}

	@Override
	public boolean supportsAuthentication() {
		return true;
	}
	
	@Override
	public boolean authenticate(Callback[] credentials) throws IdRepoException, AuthLoginException {
        logger.trace("authenticate invoked");
        String userName = null;
        String password = null;
        for (Callback callback : credentials) {
            if (callback instanceof NameCallback) {
                userName = ((NameCallback) callback).getName();
            } else if (callback instanceof PasswordCallback) {
                password = new String(((PasswordCallback) callback).getPassword());
            }
        }
        if (userName == null || password == null) {
            throw new IdRepoException(IdRepoErrorCode.UNABLE_TO_AUTHENTICATE,Repo.class.getName());
        }
        try {
        	Map<String, Set<String>> res=getAttributes(null, IdType.USER, userName, new HashSet<String>(Arrays.asList(new String[] {"userpassword"})));
        	if (res!=null && res.containsKey("userpassword") && res.get("userpassword").size()>0) {
        		final String storedHash=res.get("userpassword").iterator().next();
        		Boolean result=false;
        		try {
	        		if (storedHash.startsWith("{SSHA256}")){
	        			result=SSHA256.verifySaltedPassword(password.getBytes("UTF-8"), storedHash);
	            	}else if (storedHash.startsWith("{SSHA}")){
            			result=SSHA.verifySaltedPassword(password.getBytes("UTF-8"), storedHash);
            			try {
		            		if (result && SystemProperties.getAsBoolean("SSHA_SSHA256", false)) {
		            			res.put("userpassword", new HashSet<String>(Arrays.asList(new String[] {SSHA256.getSaltedPassword(password.getBytes("UTF-8"))})));
		            			setAttributes(null, IdType.USER, userName, res, false);
		            		}
            			}catch (Exception e) {}
	            	}else {
	            		result=(storedHash.replace("{CLEAR}", "").equals(password));
	            	}
            	}catch (UnsupportedEncodingException e) {}
        		return result;
        	}
		} catch (SSOException e) {
			throw new AuthLoginException(e);
		}
		return false;
	}
}