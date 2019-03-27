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

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openidentityplatform.openam.cassandra.Repo;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

import org.openidentityplatform.openam.cassandra.embedded.Server;

public class IdRepoTest {

	static{
//		SystemProperties.initializeProperties(Repo.class.getName()+".minIdle","1");
//		System.setProperty(Repo.class.getName()+".url","jdbc:postgresql://wsso-idm1.inside.mts.ru/openam");
	}

	static Repo repo=null;
	
	static Server cassandra;
	
	
	@BeforeClass
	public static void init() throws SSOException, IdRepoException{
		cassandra=new Server();
		cassandra.run();
		
		HashMap<String, Set<String>> configParams=new HashMap<String, Set<String>>();
		//System.setProperty("DC", "DC1");
		configParams.put("sun-idrepo-ldapv3-config-organization_name", new HashSet<String>(Arrays.asList(new String[] {"test_b2c_users"})));
		configParams.put("sun-idrepo-ldapv3-config-ldap-server", new HashSet<String>(Arrays.asList(new String[] {"localhost"})));
		configParams.put("sun-idrepo-ldapv3-config-authid", new HashSet<String>(Arrays.asList(new String[] {"cassandra"})));
		configParams.put("sun-idrepo-ldapv3-config-user-attributes", new HashSet<String>(Arrays.asList(new String[] {"user:SunidentitymsisdnnumbeR=600","user:userPassword=700","user:cn=700"})));
		repo=new Repo();
		repo.initialize(configParams);
	}
	
	@AfterClass
	public static void destory() throws SSOException, IdRepoException{
		repo.shutdown();
	}
	
	@Test
	public void delete_test() throws SSOException, IdRepoException{
		repo.delete(null, IdType.USER, "9170000000");
		assertFalse(repo.isExists(null, IdType.USER, "9170000000"));
		
	}
	
	@Test @Ignore
	public void index_test() throws SSOException, IdRepoException, InterruptedException{
		Map<String, Set<String>> param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		
		repo.delete(null, IdType.USER, "9170000000");
		repo.delete(null, IdType.USER, "9170000001");
		
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		param.put("userPassword", new HashSet<String>(Arrays.asList(new String[] {"{CLEAR}5155"})));
		repo.create(null, IdType.USER, "9170000000",param);
		assertTrue(repo.rowIndexGet(IdType.USER,"sunidentitymsisdnnumbeR", "9170000000").size()>0);
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000+"})));
		repo.setAttributes(null,IdType.USER, "9170000000", param, false);
		Thread.sleep(1000);
		assertTrue(repo.rowIndexGet(IdType.USER,"sunidentitymsisdnnumbeR", "9170000000+").size()>0);
		assertTrue(repo.rowIndexGet(IdType.USER,"sunidentitymsisdnnumbeR", "9170000000").size()==0);
		
		repo.delete(null, IdType.USER, "9170000000");
		assertTrue(repo.rowIndexGet(IdType.USER,"sunidentitymsisdnnumbeR", "9170000000+").size()==0);
		
		
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		param.put("userPassword", new HashSet<String>(Arrays.asList(new String[] {"{CLEAR}5155"})));
		repo.create(null, IdType.USER, "9170000000",param);
		
		//by UID
		assertTrue(repo.search(null, IdType.USER, "9170000000", 0, 1, null, true, Repo.AND_MOD, null, false).getSearchResults().size()==1);
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		param.put("uid", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 2, new HashSet<String>(Arrays.asList(new String[] {"uid"})), true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		//not found by secondary index
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("Cn", new HashSet<String>(Arrays.asList(new String[] {"BAD"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, false, Repo.AND_MOD, param, false).getSearchResults().size()==0);
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("uid", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		// found by secondary index
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("Cn", new HashSet<String>(Arrays.asList(new String[] {"ssss"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		//not found by row index
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"BAD"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==0);
		
		// found by row index
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		//not found by secondary index
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("Cn", new HashSet<String>(Arrays.asList(new String[] {"BAD"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==0);
		
		//not found by secondary index
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("Cn", new HashSet<String>(Arrays.asList(new String[] {"ssss"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"BAD"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==0);
		
		//not found by secondary index
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("Cn", new HashSet<String>(Arrays.asList(new String[] {"BAD"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"BAD"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==0);
		
		//found by secondary + row index
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("Cn", new HashSet<String>(Arrays.asList(new String[] {"ssss"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		//restore row index
		repo.rowIndexDelete(repo.getRowIndex(IdType.USER, "sunidentitymsisdnnumber"), "9170000000", "9170000000");
		Thread.sleep(1000);
		assertTrue(repo.rowIndexGet(IdType.USER, "sunidentitymsisdnnumber", "9170000000").size()==0);
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		Thread.sleep(1000);
		assertTrue(repo.rowIndexGet(IdType.USER, "sunidentitymsisdnnumber", "9170000000").size()==1);
		
		//test parital row index
		param.clear();
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss2"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		repo.create(null, IdType.USER, "9170000001",param);
		Thread.sleep(1000);
		assertTrue(repo.rowIndexGet(IdType.USER, "sunidentitymsisdnnumber", "9170000000").size()==2);
		
		repo.rowIndexDelete(repo.getRowIndex(IdType.USER, "sunidentitymsisdnnumber"), "9170000000", "9170000001");
		Thread.sleep(1000);
		assertTrue(repo.rowIndexGet(IdType.USER, "sunidentitymsisdnnumber", "9170000000").size()==1);
		
		param.clear();
		param.put("UID", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		param.put("UID", new HashSet<String>(Arrays.asList(new String[] {"9170000001"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		assertTrue(repo.rowIndexGet(IdType.USER, "sunidentitymsisdnnumber", "9170000000").size()==1);
		
		param.clear();
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss2"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		Thread.sleep(1000);
		assertTrue(repo.rowIndexGet(IdType.USER, "sunidentitymsisdnnumber", "9170000000").size()==2);
		
		repo.delete(null, IdType.USER, "9170000000");
		repo.delete(null, IdType.USER, "9170000001");
		assertTrue(repo.rowIndexGet(IdType.USER, "sunidentitymsisdnnumber", "9170000000").size()==0);
		assertTrue(repo.rowIndexGet(IdType.USER, "sunidentitymsisdnnumber", "9170000001").size()==0);
		assertTrue(repo.rowIndexGet(IdType.USER, "sunidentitymsisdnnumber", "9170000000+").size()==0);
	}
	
	@Test
	public void complex_test() throws SSOException, IdRepoException{
		Map<String, Set<String>> param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		
		repo.delete(null, IdType.USER, "9170000000");
		repo.delete(null, IdType.USER, "9170000001");
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertEquals(0, repo.search(null, IdType.USER, "*", 5, 2, null, true, Repo.AND_MOD, param, false).getSearchResults().size());
		
		assertFalse(repo.isExists(null, IdType.USER, "9170000000"));
		assertFalse(repo.isActive(null, IdType.USER, "9170000000"));
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		//param.put("inetuserstatus", "qqqqq");
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		param.put("userPassword", new HashSet<String>(Arrays.asList(new String[] {"{CLEAR}5155"})));
		repo.create(null, IdType.USER, "9170000000",param);
		
		assertTrue(repo.isExists(null, IdType.USER, "9170000000"));
		assertTrue(repo.isActive(null, IdType.USER, "9170000000"));
		
		repo.setActiveStatus(null, IdType.USER, "9170000000", false);
		assertFalse(repo.isActive(null, IdType.USER, "9170000000"));
		
		Set<String> fields=new HashSet<String>(Arrays.asList(new String[]{"CN","sdfsdf","o"}));
		System.out.println(repo.getAttributes(null, IdType.USER, "9170000000",fields));
		System.out.println(repo.getAttributes(null, IdType.USER, "9170000000"));
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("DISPLAYNAME", new HashSet<String>(Arrays.asList(new String[] {"explicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly casts"})));
		repo.setAttributes(null, IdType.USER, "9170000000", param, false);
		System.out.println(repo.getAttributes(null, IdType.USER, "9170000000"));
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		System.out.println(repo.search(null, IdType.USER, "*", 5, 2, null, true, Repo.AND_MOD, param, false).getSearchResults());
		System.out.println(repo.search(null, IdType.USER, "91700*", 5, 2, null, true, Repo.AND_MOD, param, false).getSearchResults());
		System.out.println(repo.search(null, IdType.USER, "9170000000", 5, 2, null, true, Repo.AND_MOD, null, false).getSearchResults());
		System.out.println(repo.search(null, IdType.USER, "91700*", 5, 2, null, false, Repo.AND_MOD, null, false).getSearchResults());
//		try{
//			repo.create(null, IdType.USER, "9170000000",param);
//			fail("exists ?");
//		}catch (IdRepoException e) {}
	}
}
