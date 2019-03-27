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
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.utils.CrestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnDefinitions.Definition;
import com.datastax.driver.core.ColumnMetadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.datastax.driver.core.querybuilder.Update.Assignments;
import com.datastax.driver.core.querybuilder.Update.Options;
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
	public void initialize(Map configParams) throws IdRepoException  {
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
			RowIndexThread rowIndexThread=new RowIndexThread();
			rowIndexThread.run();
			timer.scheduleAtFixedRate(rowIndexThread,20*60*1000,10*60*1000);
		}catch(Exception e){
			logger.error("error",e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void shutdown() {
		super.shutdown();
		timer.cancel();
		if (session!=null && !session.isClosed())
			session.close();
	}
	
	static String rowIndexSchema="rowIndexSchema";//CREATE TABLE rowIndexSchema (id int PRIMARY KEY ,name text);
	static String rowIndexData="rowIndexData";//CREATE TABLE rowIndexData (id int,value text,key text, PRIMARY KEY (id,value,key));
	
	final Timer timer=new Timer();
	final Map<String,Integer> rowIndex=new ConcurrentSkipListMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
	class RowIndexThread extends TimerTask {
		@Override
		public void run() {
			try{
				logger.info("stat: {}",ExecuteCallback.getStat(session));
				Set<String> fields=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
				if (session.getCluster().getMetadata().getKeyspace(keyspace).getTable(rowIndexSchema)!=null && session.getCluster().getMetadata().getKeyspace(keyspace).getTable(rowIndexData)!=null){
					for (Row row : new ExecuteCallback(session, QueryBuilder.select().column("id").column("name").from(keyspace,rowIndexSchema)).execute()){
						fields.add(row.getString("name"));
						rowIndex.put(row.getString("name"),row.getInt("id"));
					}
				}
				for (String field : rowIndex.keySet()) 
					if (!fields.contains(field))
						rowIndex.remove(field);
			}catch(Throwable e){
				logger.error("{}",keyspace,e.getMessage());
			}
		}
		
	}
	
	public Integer getRowIndex(IdType type,String field){
		return rowIndex.get(MessageFormat.format("{0}.{1}", getTableName(type),field.replaceAll("\"", "")));
	}
	
	public void rowIndexDelete(Integer index,String value,String key){
		if (index!=null && value!=null && key!=null){
			final Statement deleteIndex=QueryBuilder.delete().from(keyspace,rowIndexData).where(QueryBuilder.eq("id", index)).and(QueryBuilder.eq("value", value)).and(QueryBuilder.eq("key", key));
			new ExecuteCallback(session,deleteIndex).executeAsync();
		}
	}
	void rowIndexAdd(Integer index,String value,String key,Integer ttl){
		if (index!=null && value!=null && key!=null){
			Statement addIndex=QueryBuilder.update(keyspace,rowIndexData).with(QueryBuilder.set("time", new Date())).where(QueryBuilder.eq("id", index)).and(QueryBuilder.eq("value", value)).and(QueryBuilder.eq("key", key));
			if (ttl>0)
				addIndex=((com.datastax.driver.core.querybuilder.Update.Where)addIndex).using(QueryBuilder.ttl(ttl));
			new ExecuteCallback(session, addIndex).executeAsync();
		}
	}
	
	public Set<String> rowIndexGet(Integer index,String value){
		final Set<String> res=new HashSet<String>();
		if (index==null || value==null)
			return null;
		final Statement selectIndex=QueryBuilder.select("key").from(keyspace,rowIndexData).where(QueryBuilder.eq("id", index)).and(QueryBuilder.eq("value", value)).limit(64000);
		final ResultSet rc=new ExecuteCallback(session,selectIndex).execute();
		for (Row row : rc){ 
			if (rc.getAvailableWithoutFetching() == (session.getCluster().getConfiguration().getQueryOptions().getFetchSize()-1) && !rc.isFullyFetched())
				rc.fetchMoreResults(); // this is asynchronous
			res.add(row.getString(0));
		}
		return res;
	}
	
	public Set<String> rowIndexGet(IdType type,String field,String value){
		final Integer index=getRowIndex(type, field);
		final Set<String> uids=rowIndexGet(index,value);
		final Set<String> uidsReal=new HashSet<String>();
		if (uids==null)
			return null;
		if (uids.size()>0){//test res
			final String uid=getKeyName(type);
			final String fieldName=getFieldName(type, field);
			final Statement selectIndex=QueryBuilder.select(new String[]{uid,fieldName}).from(keyspace,getTableName(type)).where(QueryBuilder.in(uid, uids.toArray()));
			final ResultSet rc=new ExecuteCallback(session,selectIndex).execute();
			for (Row row : rc){ 
				if (rc.getAvailableWithoutFetching() == (session.getCluster().getConfiguration().getQueryOptions().getFetchSize()-1) && !rc.isFullyFetched())
					rc.fetchMoreResults(); // this is asynchronous
				final Set<String> values=row.getSet(fieldName,String.class);
				if (values!=null && values.contains(value))
					uidsReal.add(row.getString(uid));
				else{
					logger.warn("remove phantom row index {} {}: {}={}",type,row.getString(uid),field,value);
					rowIndexDelete(index, value, row.getString(uid));
				}
			}
			uids.removeAll(uidsReal);
			for (String uidName : uids) {
				logger.warn("remove phantom row index {} {}: {}={}",type,uidName,field,value);
				rowIndexDelete(index, value, uidName);
			}
		}
		return uidsReal;
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
		Map<String, Set<String>> attr=getAttributes(token, type, name,new HashSet<String>(Arrays.asList(new String[]{getKeyName(type)})));
		Set<String> value=attr.get(getKeyName(type).replace("\"", ""));
		return value!=null && (value.size()!=0);
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

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Set<String>> getAttributes(SSOToken token, IdType type,String name, Set attrNames) throws IdRepoException, SSOException {
		validate(type, IdOperation.READ);
		final Map<String, Set<String>> attr=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		try{
			//coutners
			if (StringUtils.startsWith(name,"coutner-")){
				for (String attrName : (Set<String>)attrNames) {
					new ExecuteCallback(session,QueryBuilder.update(keyspace,"coutners").with(QueryBuilder.incr("generator")).where(QueryBuilder.eq("name", attrName))).execute();
					final ResultSet rc=new ExecuteCallback(session,QueryBuilder.select().column("generator").from(keyspace,"coutners").where(QueryBuilder.eq("name", attrName))).execute();
					for (Row row : rc)
						attr.put(attrName, new HashSet<String>(Arrays.asList(new String[]{new Long(row.getLong("generator")).toString()})));
				}
				return attr;
			}
			Select.Builder selectBuilder=(attrNames==null||attrNames.isEmpty())?QueryBuilder.select().all():QueryBuilder.select();
			if (attrNames!=null){
				boolean setColumns=false;
				for (Object field : attrNames.toArray()){ 
					final String fieldName=getFieldName(type, field.toString());
					if (fieldName!=null){
						selectBuilder=((Select.Selection)selectBuilder).column(fieldName);
						setColumns=true;
					}
				}
				if (!setColumns)
					return Collections.EMPTY_MAP;
			}
			final Statement statement=selectBuilder.from(keyspace,getTableName(type)).where(QueryBuilder.eq(getKeyName(type), name)).limit(1);
			final ResultSet rc=new ExecuteCallback(session,statement).execute();
			for (Row row : rc){
				if (rc.getAvailableWithoutFetching() == (session.getCluster().getConfiguration().getQueryOptions().getFetchSize()-1) && !rc.isFullyFetched())
					rc.fetchMoreResults(); // this is asynchronous
				for (Definition column: row.getColumnDefinitions()){ 
					Object value=row.getObject(column.getName());
					attr.put(column.getName(), (value instanceof String)? new LinkedHashSet<String>(Arrays.asList(new String[]{(String)value})) : (value==null)?new LinkedHashSet<String>(0):new LinkedHashSet<String>((Collection<String>)value));
				}
			}
		}catch(Throwable e){
			logger.error("getAttributes {} {} {}",type,name,attrNames,e.getMessage());
			throw new IdRepoException(e.getMessage());
		}
		return attr;
	}

	@Override
	public Map<String, byte[][]> getBinaryAttributes(SSOToken token, IdType type, String name, Set attrNames) throws IdRepoException, SSOException {
		//validate(type, IdOperation.READ);
		logger.warn("unsupported getBinaryAttributes {} {} {}",type,name,attrNames);
		throw new IdRepoUnsupportedOpException("unsupported getBinaryAttributes");
	}

	@Override
	public String create(SSOToken token, IdType type, String name, Map attrMap) throws IdRepoException, SSOException {
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
	public void setAttributes(SSOToken token, IdType type, String name, Map attributes, boolean isAdd) throws IdRepoException, SSOException {
		validate(type, IdOperation.EDIT);
		//if (isAdd || isExists(token, type, name)) //dont create if edit and not exists
			try{
				final boolean async=(attributes.remove(asyncField)!=null);
						
				if (isAdd && !attributes.containsKey(activeAttr))
					attributes.put(activeAttr, new HashSet<String>(Arrays.asList(new String[]{activeValue})));
				
				final Map<Integer,Set<String>> ttl2field=new HashMap<Integer, Set<String>>();
				for (Object field : attributes.keySet().toArray()) {
					final String fieldName=getFieldName(type, field.toString());
					if (fieldName!=null){
						Integer ttl=getTTL(type, field.toString());
						if (ttl==null)
							ttl=0;
						if (!ttl2field.containsKey(ttl))
							ttl2field.put(ttl, new TreeSet<String>(String.CASE_INSENSITIVE_ORDER));
						ttl2field.get(ttl).add(fieldName);
					}
				}

				//remove old rowIndex
				final Map<String,Integer> rowIndexFields=new TreeMap<String,Integer>(String.CASE_INSENSITIVE_ORDER);
				for (String field : ((Map<String, Set<String>>)attributes).keySet()) 
					if (!StringUtils.equalsIgnoreCase(field, getKeyName(type))){ //PK not in row Index
						final Integer index=getRowIndex(type, field);
						if (index!=null)
							rowIndexFields.put(field,index);
					}
				if (rowIndexFields.size()>0){
					final Map<String,Set<String>> oldValues=getAttributes(token, type, name, rowIndexFields.keySet());
					for (Entry<String,Set<String>> oldValue : oldValues.entrySet()) 
						for (String value : oldValue.getValue()) 
							if (value!=null){
								Set<String> newValue=((Map<String, Set<String>>)attributes).get(oldValue.getKey());
								if (newValue==null || !newValue.contains(value))
									rowIndexDelete(rowIndexFields.get(oldValue.getKey()),value,name);
							}
				}
				
				//by TTL
				for (Entry<Integer,Set<String>> ttl2fieldEntry : ttl2field.entrySet()) { 
					//update row data
					Statement statement=QueryBuilder.update(keyspace,getTableName(type)).with();
					for (String field : ttl2fieldEntry.getValue()) 
						if (!StringUtils.equalsIgnoreCase(field, getKeyName(type))) //PK already in where
							statement=((Assignments)statement).and(QueryBuilder.set(field, convert(field,((Map<String, Set<String>>)attributes).get(field.replaceAll("\"", "")))));
					
					if (ttl2fieldEntry.getKey()>0)
						statement=((Assignments)statement).using(QueryBuilder.ttl(ttl2fieldEntry.getKey()));
					
					if ((statement instanceof Options))
						statement=((Options)statement).where(QueryBuilder.eq(getKeyName(type), name));//.onlyIf(new Exists());
					else
						statement=((Assignments)statement).where(QueryBuilder.eq(getKeyName(type), name));//.onlyIf(new Exists());
					
					if (async)
						new ExecuteCallback(session, statement).executeAsync();
					else
						new ExecuteCallback(session, statement).execute();
					
					//add rowIndex
					for (String field : ttl2fieldEntry.getValue()) 
						if (!StringUtils.equalsIgnoreCase(field, getKeyName(type))){ //PK not in row Index
							final Integer index=rowIndexFields.get(field.replaceAll("\"", ""));
							if (index!=null){
								for (String value : ((Map<String, Set<String>>)attributes).get(field.replaceAll("\"", ""))) 
									rowIndexAdd(index,value,name,ttl2fieldEntry.getKey());
							}
						}
				}		
			}catch(Throwable e){
				logger.error("setAttributes {} {} {} {}",type,name,attributes,isAdd,e.getMessage());
				throw new IdRepoException(e.getMessage());
			}
	}

	@Override
	public void setBinaryAttributes(SSOToken token, IdType type, String name,Map attributes, boolean isAdd) throws IdRepoException, SSOException {
		//validate(type, IdOperation.EDIT);
		logger.warn("unsupported setBinaryAttributes {} {} {} {}",type,name,attributes,isAdd);
		throw new IdRepoUnsupportedOpException("unsupported setBinaryAttributes");
	}

	@Override
	public void removeAttributes(SSOToken token, IdType type, String name, Set attrNames) throws IdRepoException, SSOException {
		validate(type, IdOperation.EDIT);
		try{
			final boolean async=(attrNames!=null && attrNames.remove(asyncField));
			
			List<String> deleteColums=new ArrayList<String>();
			if (attrNames!=null)
				for (Object field : attrNames.toArray()){ 
					final String fieldName=getFieldName(type, field.toString());
					if (fieldName!=null)
						deleteColums.add(fieldName);
				}
			
			//remove old rowIndex
			final Map<String,Set<String>> oldValues=getAttributes(token, type, name, (attrNames==null||attrNames.isEmpty())?null:attrNames);
			final Map<String,Integer> rowIndexFields=new TreeMap<String,Integer>(String.CASE_INSENSITIVE_ORDER);
			for (String field : oldValues.keySet()) 
				if (!StringUtils.equalsIgnoreCase(field, getKeyName(type))){ //PK not in row Index
					final Integer index=getRowIndex(type, field);
					if (index!=null)
						rowIndexFields.put(field,index);
				}
			for (Entry<String,Integer> field : rowIndexFields.entrySet()){ 
				final Set<String> oldValue=oldValues.get(field.getKey());	
				if (oldValue!=null)
					for (String value : oldValue) 
						if (value!=null)
							rowIndexDelete(field.getValue(),value,name);
			}
			//remove row
			Statement statement=((attrNames==null||attrNames.isEmpty())?QueryBuilder.delete():QueryBuilder.delete(deleteColums.toArray(new String[0]))).from(keyspace,getTableName(type)).where(QueryBuilder.eq(getKeyName(type), name));
			if (async)
				new ExecuteCallback(session, statement).executeAsync();
			else
				new ExecuteCallback(session, statement).execute();
		}catch(Throwable e){
			logger.error("removeAttributes {} {} {}",type,name,attrNames,e.getMessage());
			throw new IdRepoException(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	Map<String,Set<String>> row2Map(Row row){
		final Map<String,Set<String>> attr=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		for (Definition column: row.getColumnDefinitions()){ 
			Object value=row.getObject(column.getName());
			attr.put(column.getName(), (value instanceof String)? new LinkedHashSet<String>(Arrays.asList(new String[]{(String)value})) : (value==null)?new LinkedHashSet<String>(0):(Set<String>)value );
		}
		return attr;
	}
	
	Map<String, Map<String,Set<String>>> ResultSet2Map(IdType type,ResultSet rc){
		final Map<String, Map<String,Set<String>>> users2attr =new ConcurrentHashMap<String, Map<String,Set<String>>>();
		for (Row row : rc) {
			if (rc.getAvailableWithoutFetching() == (session.getCluster().getConfiguration().getQueryOptions().getFetchSize()-1) && !rc.isFullyFetched())
				rc.fetchMoreResults(); // this is asynchronous
			users2attr.put(row.getString(getKeyName(type)), row2Map(row));
		}
		return users2attr;
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
	
	public RepoSearchResults search(SSOToken token, IdType type, String pattern, int maxTime, int maxResults, Set returnAttrs, boolean returnAllAttrs, int filterOp,Map avPairs, boolean recursive) throws IdRepoException, SSOException {
		validate(type, IdOperation.READ);
		try{
		//read returnFields
			final Set<String> returnFields=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			if (!returnAllAttrs || returnAttrs==null){
				if (returnAttrs==null)
					returnAttrs=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
				if (avPairs!=null) //add filters column to output fields
					returnAttrs.addAll(avPairs.keySet());
				for (Object field : returnAttrs.toArray()){ 
					final String fieldName=getFieldName(type, field.toString());
					if (fieldName!=null)
						returnFields.add(fieldName);
				}
			}
			returnFields.add(getKeyName(type)); //
			
			boolean repeatBySecondary=false;
			while (true){
				Statement statement=((returnAllAttrs)?QueryBuilder.select().all():QueryBuilder.select(returnFields.toArray(new String[0]))).from(keyspace,getTableName(type)).where();
			
				final Set<Entry<String,String>> 			secondaryIndex=		new HashSet<Entry<String,String>>();
				final Map<Entry<String,String>,Set<String>> rowIndexFound=		new HashMap<Entry<String,String>,Set<String>>();
				final Set<Entry<String,String>> 			rowIndexNotFound=	new HashSet<Entry<String,String>>();
				
				if (avPairs!=null && avPairs.size()>0){
					final Set<String> indexFields=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
					indexFields.addAll(avPairs.keySet());
					for (final Entry<String, Set<String>> filterEntry : ((Map<String, Set<String>>)avPairs).entrySet()){ 
						final String fieldName=getFieldName(type, filterEntry.getKey());
						if (fieldName!=null){
							for (final String value : filterEntry.getValue()){ 
								//if uid - serach only by secondary index
								if  ((StringUtils.isNotBlank(pattern)&&!StringUtils.equals(pattern, "*")) || indexFields.contains(getKeyName(type).replace("\"", ""))){
									secondaryIndex.add(new AbstractMap.SimpleEntry<String,String>(fieldName, value));
									continue;
								}
								final Set<String> uids=rowIndexGet(type, filterEntry.getKey(), value);
								if (uids==null)
									secondaryIndex.add(new AbstractMap.SimpleEntry<String,String>(fieldName, value));
								else if ((uids!=null && uids.isEmpty()) || repeatBySecondary==true) //row index empty
									rowIndexNotFound.add(new AbstractMap.SimpleEntry<String,String>(fieldName, value));
								else  
									rowIndexFound.put(new AbstractMap.SimpleEntry<String,String>(fieldName, value),uids);
							}
						}
					}
					final Set<String> uids=new HashSet<String>();
					if (!rowIndexFound.isEmpty()){ //by row index
						for (final Entry<Entry<String,String>,Set<String>> pair : rowIndexFound.entrySet())
							if (uids.isEmpty())
								uids.addAll(pair.getValue());
							else
								uids.retainAll(pair.getValue());
					}
					if (!uids.isEmpty()){
						if (StringUtils.isNotBlank(pattern)&&!StringUtils.equals(pattern, "*"))
							uids.add(pattern);
						statement=((Where)statement).and(QueryBuilder.in(getKeyName(type), uids.toArray()));
					}
					else{	//by secondary index
						//where from pattern
						if (StringUtils.isNotBlank(pattern)&&!StringUtils.equals(pattern, "*"))
							statement=((Where)statement).and(QueryBuilder.eq(getKeyName(type), pattern));
						
						secondaryIndex.addAll(rowIndexNotFound); 
						for (final Entry<String, String> pair : secondaryIndex) 
							statement=((Where)statement).and((StringUtils.equalsIgnoreCase(getKeyName(type), pair.getKey()))?QueryBuilder.eq(pair.getKey(), pair.getValue()):QueryBuilder.contains(pair.getKey(), pair.getValue()));
					}
				}else
					if (StringUtils.isNotBlank(pattern)&&!StringUtils.equals(pattern, "*"))
						statement=((Where)statement).and(QueryBuilder.eq(getKeyName(type), pattern));
				
				statement=((Where)statement).limit(Math.min(32000,Math.max(maxResults,1)));
				
			//read result
				Map<String, Map<String,Set<String>>> users2attr;
				try{
					users2attr=ResultSet2Map(type,new ExecuteCallback(session, statement).execute());
				}catch(InvalidQueryException e2){
					if (!e2.getMessage().contains("FILTERING"))
						throw e2;
					users2attr=ResultSet2Map(type,new ExecuteCallback(session, new SimpleStatement(((Select)statement).allowFiltering().toString())).execute());
				}
				for (final Entry<String, Map<String,Set<String>>> entryUid : users2attr.entrySet()) {
					if (!rowIndexNotFound.isEmpty()) //try restore row index
						for (final Entry<String,String> entryIndex : rowIndexNotFound) {
							final Set<String> values=entryUid.getValue().get(entryIndex.getKey().replace("\"", ""));
							if (values!=null && values.contains(entryIndex.getValue())){
								logger.warn("restore row index {} {}: {}={} ",type,entryUid.getKey(),entryIndex.getKey(),entryIndex.getValue());
								rowIndexAdd(getRowIndex(type, entryIndex.getKey()), entryIndex.getValue(), entryUid.getKey(), 0);
							}
						}
					if (avPairs!=null && avPairs.size()>0) //test result where
						for (final Entry<String, Set<String>> filterEntry : ((Map<String, Set<String>>)avPairs).entrySet())
							for (String value : filterEntry.getValue()) {
								final Set<String> values=entryUid.getValue().get(filterEntry.getKey());
								if (values==null || !values.contains(value)){
									logger.warn("ignore {}: {}={}",entryUid.getKey(),filterEntry.getKey(),filterEntry.getValue());
									users2attr.remove(entryUid.getKey());
								}
							}
				}
				if (!rowIndexFound.isEmpty() && users2attr.isEmpty() && repeatBySecondary==false){
					repeatBySecondary=true;
					logger.warn("restart search {} -> {}: {}",rowIndexFound,pattern,avPairs);
					continue;
				}
				return new RepoSearchResults(users2attr.keySet(),(maxResults>0&&users2attr.size()>maxResults)?RepoSearchResults.SIZE_LIMIT_EXCEEDED:RepoSearchResults.SUCCESS,users2attr,type);
			}
		}catch(Throwable e){
			logger.error("search {} {} {} {} {} {} {} {} {}",type,pattern,maxTime,maxResults,returnAttrs,returnAllAttrs,filterOp,avPairs,recursive,e.getMessage());
			throw new IdRepoException(e.getMessage());
		}
	}

	@Override
	public void modifyMemberShip(SSOToken token, IdType type, String name, 	Set members, IdType membersType, int operation) throws IdRepoException, SSOException {
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
	public void assignService(SSOToken token, IdType type, String name, String serviceName, SchemaType stype, Map attrMap) throws IdRepoException,SSOException {
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
	public Set<String> getAssignedServices(SSOToken token, IdType type, String name, Map mapOfServicesAndOCs) 	throws IdRepoException, SSOException {
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
	public void unassignService(SSOToken token, IdType type, String name, String serviceName, Map attrMap) throws IdRepoException, SSOException {
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
	public Map<String, Set<String>> getServiceAttributes(SSOToken token, IdType type, String name, String serviceName, Set attrNames) throws IdRepoException, SSOException {
		validate(type, IdOperation.SERVICE);
		return getAttributes(token, type, (type==IdType.REALM)?"ContainerDefaultTemplateRole":name,attrNames);
	}

	@Override
	public Map<String, byte[][]> getBinaryServiceAttributes(SSOToken token, IdType type, String name, String serviceName, Set attrNames) throws IdRepoException, SSOException {
		//validate(type, IdOperation.SERVICE);
		logger.warn("unsupported getBinaryServiceAttributes {} {} {} {}",type,name,serviceName,attrNames);
		throw new IdRepoUnsupportedOpException("unsupported getBinaryServiceAttributes");
	}
	
	@Override
	public void modifyService(SSOToken token, IdType type, String name, String serviceName, SchemaType sType, Map attrMap) throws IdRepoException,	SSOException {
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

	String getTableName(IdType type){
		final String res=type2table.get(type);
		return (res==null)?type.getName():res;
	}
	
	final Map<IdType,String> type2pk=new HashMap<IdType, String>();
	
	String getKeyName(IdType type){
		String res=type2pk.get(type);
		if (res==null){
			res= MessageFormat.format("\"{0}\"",session.getCluster().getMetadata().getKeyspace(keyspace).getTable(getTableName(type)).getPrimaryKey().iterator().next().getName());
			type2pk.put(type, res);
		}
		return res;
	}
	
	final Map<IdType,Map<String,String>> type2fields=new ConcurrentHashMap<IdType, Map<String,String>>();
	String getFieldName(IdType type,String name){
		if(StringUtils.isBlank(name))
			return null;
		Map<String,String> fields=type2fields.get(type);
		if (fields==null)
			synchronized (type2fields) {
				fields=type2fields.get(type);
				if (fields==null){
					type2fields.put(type, new  ConcurrentSkipListMap<String, String>(String.CASE_INSENSITIVE_ORDER));
					fields=type2fields.get(type);
				}
			}
		String res=fields.get(name);
		if (res==null && !StringUtils.equalsIgnoreCase(asyncField, name)){
			for (ColumnMetadata cm : session.getCluster().getMetadata().getKeyspace(keyspace).getTable(getTableName(type)).getColumns()) 
				if (StringUtils.equalsIgnoreCase(cm.getName(), name.replaceAll("\"", ""))){
					fields.put(name, MessageFormat.format("\"{0}\"",cm.getName()));
					break;
				}
			res=fields.get(name);
			if (res==null)
				logger.warn("{} unknown field {} for {}",keyspace, name,type);
		}
		return res;
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
	
	Object convert(String name, Set<String> values){
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
