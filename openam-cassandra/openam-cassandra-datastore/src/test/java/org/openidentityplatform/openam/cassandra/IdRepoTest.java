package org.openidentityplatform.openam.cassandra;
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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.IdRepoDuplicateObjectException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

import org.openidentityplatform.openam.cassandra.embedded.Server;

public class IdRepoTest {

	static Repo repo=null;
	static Server cassandra;
	
	@BeforeClass
	public static void init() throws SSOException, IdRepoException{
		System.setProperty("datastax-java-driver.advanced.auth-provider.class","PlainTextAuthProvider");
		System.setProperty("datastax-java-driver.advanced.auth-provider.username","cassandra");
		System.setProperty("datastax-java-driver.advanced.auth-provider.password","cassandra");
		
		cassandra=new Server();
		cassandra.run();

		HashMap<String, Set<String>> configParams=new HashMap<String, Set<String>>();
		//System.setProperty("DC", "DC1");
		configParams.put("sun-idrepo-ldapv3-config-organization_name", new HashSet<String>(Arrays.asList(new String[] {"realm_name"})));
		configParams.put("sun-idrepo-ldapv3-config-ldap-server", new HashSet<String>(Arrays.asList(new String[] {"localhost"})));
		configParams.put("sun-idrepo-ldapv3-config-authid", new HashSet<String>(Arrays.asList(new String[] {"cassandra"})));
		configParams.put("sun-idrepo-ldapv3-config-user-attributes", new HashSet<String>(Arrays.asList(new String[] {"user:SunidentitymsisdnnumbeR=600","user:userPassword=700","user:cn=700"})));
		repo=new Repo();
		repo.initialize(configParams);
	}
	
	@AfterClass
	public static void destory() throws SSOException, IdRepoException{
		if (repo!=null) {
			repo.shutdown();
			repo=null;
		}
	}
	
	@Test 
	public void create_test() throws SSOException, IdRepoException{
		repo.delete(null, IdType.USER, "9170000000");
		final Map<String, Set<String>> param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		repo.create(null,  IdType.USER, "9170000000", param);
		try {
			repo.create(null,  IdType.USER, "9170000000", param);
			assertFalse("need throw IdRepoDuplicateObjectException",true);
		}catch (IdRepoDuplicateObjectException e) {}
	}
	
	@Test 
	public void delete_test() throws SSOException, IdRepoException{
		repo.delete(null, IdType.USER, "9170000000");
		repo.removeAttributes(null, IdType.USER, "9170000000", new HashSet<String>(Arrays.asList(new String[] {"uid","cn"})));
		assertFalse(repo.isExists(null, IdType.USER, "9170000000"));
		
	}
	
	@Test 
	public void index_test() throws SSOException, IdRepoException, InterruptedException, AuthLoginException{
		Map<String, Set<String>> param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		
		repo.delete(null, IdType.USER, "9170000000");
		repo.delete(null, IdType.USER, "9170000001");
		
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		param.put("userPassword", new HashSet<String>(Arrays.asList(new String[] {"test"})));
		repo.create(null, IdType.USER, "9170000000",param);
		NameCallback user=new NameCallback("9170000000");
		user.setName("9170000000");
		PasswordCallback pass=new PasswordCallback("sss",false);
		pass.setPassword("test".toCharArray());
		assertTrue( repo.authenticate(new Callback[] {user,pass}));
		pass.setPassword("test2".toCharArray());
		assertFalse( repo.authenticate(new Callback[] {user,pass}));
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000+"})));
		repo.setAttributes(null,IdType.USER, "9170000000", param, false);
		
		repo.delete(null, IdType.USER, "9170000000");
		
		
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		param.put("iplanet-AM-user-alias-list", new HashSet<String>(Arrays.asList(new String[] {"aBC"})));
		param.put("userPassword", new HashSet<String>(Arrays.asList(new String[] {"{CLEAR}5155"})));
		param.put("UID", new HashSet<String>(Arrays.asList(new String[] {"9170000000S"})));
		repo.create(null, IdType.USER, "9170000000S",param);
		
		//by UID
		assertEquals(1,repo.search(null, IdType.USER, "9170000000S", 0, 1, null, true, Repo.AND_MOD, null, false).getSearchResults().size());
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		assertEquals(1,repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size());
		
		param.put("uid", new HashSet<String>(Arrays.asList(new String[] {"9170000000s"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertEquals(1,repo.search(null, IdType.USER, "*", 0, 2, new HashSet<String>(Arrays.asList(new String[] {"uid"})), true, Repo.AND_MOD, param, false).getSearchResults().size());
		assertEquals(1,repo.search(null, IdType.USER, "9170000000S", 0, 2, new HashSet<String>(Arrays.asList(new String[] {"uid"})), false, Repo.AND_MOD, null, false).getSearchResults().size());
		param.remove("sunidentitymsisdnnumber");
		assertEquals(1,repo.search(null, IdType.USER, "*", 0, 2, new HashSet<String>(Arrays.asList(new String[] {"uid"})), false, Repo.AND_MOD, param, false).getSearchResults().size());
		assertEquals(1,repo.search(null, IdType.USER, "*", 0, 2, new HashSet<String>(Arrays.asList(new String[] {"uid"})), false, Repo.AND_MOD, null, false).getSearchResults().size());
		assertEquals(1,repo.search(null, IdType.USER, "*", 0, 2, new HashSet<String>(Arrays.asList(new String[] {"uid","unknown"})), false, Repo.AND_MOD, null, false).getSearchResults().size());
		assertEquals(1,repo.search(null, IdType.USER, "*", 0, 2, new HashSet<String>(), false, Repo.AND_MOD, param, false).getSearchResults().size());
		assertEquals(1,repo.search(null, IdType.USER, "*", 0, 2, null, false, Repo.AND_MOD, param, false).getSearchResults().size());
		
		//not found by secondary index
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("iplanet-AM-user-alias-lisT", new HashSet<String>(Arrays.asList(new String[] {"BAD"})));
		assertEquals(0,repo.search(null, IdType.USER, "*", 0, 1, null, false, Repo.AND_MOD, param, false).getSearchResults().size());
		param.put("iplanet-AM-user-alias-lisT", new HashSet<String>(Arrays.asList(new String[] {"Abc"})));
		assertEquals(1,repo.search(null, IdType.USER, "*", 0, 1, null, false, Repo.AND_MOD, param, false).getSearchResults().size());
		
		//not found by secondary index
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("Cn", new HashSet<String>(Arrays.asList(new String[] {"BAD"})));
		assertEquals(0,repo.search(null, IdType.USER, "*", 0, 1, null, false, Repo.AND_MOD, param, false).getSearchResults().size());
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("uid", new HashSet<String>(Arrays.asList(new String[] {"9170000000s"})));
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
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		//test parital row index
		param.clear();
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss2"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		param.put("iplanet-am-user-alias-list", new HashSet<String>(Arrays.asList(new String[] {"iplanet-am-user-alias-list"})));
		repo.create(null, IdType.USER, "9170000001",param);
		
		param.clear();
		param.put("iplanet-am-user-alias-list", new HashSet<String>(Arrays.asList(new String[] {"iplanet-am-user-alias-list"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		param.clear();
		param.put("UID", new HashSet<String>(Arrays.asList(new String[] {"9170000000S"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		param.put("UID", new HashSet<String>(Arrays.asList(new String[] {"9170000002"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==0);
		
		param.clear();
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss2"})));
		assertTrue(repo.search(null, IdType.USER, "*", 0, 1, null, true, Repo.AND_MOD, param, false).getSearchResults().size()==1);
		
		repo.delete(null, IdType.USER, "9170000000");
		repo.delete(null, IdType.USER, "9170000001");
	}
	
	@Test 
	public void unknown_index_test() throws SSOException, IdRepoException{
		Map<String, Set<String>> param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("bad_field", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		assertEquals(0, repo.search(null, IdType.USER, "*", 5, 2, null, true, Repo.AND_MOD, param, false).getSearchResults().size());
	}
	
	@Test 
	public void members_test() throws SSOException, IdRepoException{
		repo.delete(null, IdType.GROUP, "group");
		repo.delete(null, IdType.USER, "user");
		
		assertEquals(0,repo.getMembers(null, IdType.GROUP, "group", IdType.USER).size());
		assertEquals(0,repo.getMemberships(null, IdType.USER, "user", IdType.GROUP).size());
		
		Map<String, Set<String>> param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"group"})));
		repo.create(null, IdType.GROUP, "group",param);
		assertTrue(repo.isExists(null, IdType.GROUP, "group"));
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"user"})));
		repo.create(null, IdType.USER, "user",param);
		assertTrue(repo.isExists(null, IdType.USER, "user"));
		
		repo.modifyMemberShip(null, IdType.GROUP, "group", new HashSet<String>(Arrays.asList(new String[] {"user"})), IdType.USER, Repo.ADDMEMBER);
		assertEquals(1,repo.getMembers(null, IdType.GROUP, "group", IdType.USER).size());
		assertEquals("user",repo.getMembers(null, IdType.GROUP, "group", IdType.USER).iterator().next());
		assertEquals(1,repo.getMemberships(null, IdType.USER, "user", IdType.GROUP).size());
		assertEquals("group",repo.getMemberships(null, IdType.USER, "user", IdType.GROUP).iterator().next());
		
		repo.modifyMemberShip(null, IdType.GROUP, "group", new HashSet<String>(Arrays.asList(new String[] {"user"})), IdType.USER, Repo.REMOVEMEMBER);
		assertEquals(0,repo.getMembers(null, IdType.GROUP, "group", IdType.USER).size());
		assertEquals(0,repo.getMemberships(null, IdType.USER, "user", IdType.GROUP).size());
		
		repo.delete(null, IdType.GROUP, "group");
		repo.delete(null, IdType.USER, "user");
	}
	
	@Test 
	public void complex_test() throws SSOException, IdRepoException{
		Map<String, Set<String>> param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		
		repo.delete(null, IdType.USER, "9170000000");
		repo.delete(null, IdType.USER, "9170000001");
		repo.delete(null, IdType.USER, "9170000000S");
		repo.delete(null, IdType.USER, "9170000000s");

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
		for (String field : param.keySet()) {
			assertTrue(field,repo.getAttributes(null, IdType.USER, "9170000000").containsKey(field));
		}
		assertEquals("9170000000",repo.getAttributes(null, IdType.USER, "9170000000",new HashSet<String>(Arrays.asList(new String[]{"uid"}))).get("uid").iterator().next());
		assertEquals("9170000000",repo.getAttributes(null, IdType.USER, "9170000000").get("uid").iterator().next());
		
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("DISPLAYNAME", new HashSet<String>(Arrays.asList(new String[] {"explicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly casts"})));
		repo.setAttributes(null, IdType.USER, "9170000000", param, false);
		
		param.put("DISPLAYNAME", new HashSet<String>(Arrays.asList(new String[] {"explicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitlyexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly castsexplicitly casts"})));
		repo.setAttributes(null, IdType.USER, "9170000000", param, false);
		
		System.out.println(repo.getAttributes(null, IdType.USER, "9170000000"));
		assertEquals(1,repo.getAttributes(null, IdType.USER, "9170000000",param.keySet()).get("displayname").size());
		
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
	
	@Test 
	public void update_field_test() throws SSOException, IdRepoException{
		Map<String, Set<String>> param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		
		repo.delete(null, IdType.USER, "9170000000");
		
		param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		//param.put("inetuserstatus", "qqqqq");
		param.put("CN", new HashSet<String>(Arrays.asList(new String[] {"ssss"})));
		param.put("sunidentitymsisdnnumber", new HashSet<String>(Arrays.asList(new String[] {"9170000000"})));
		param.put("userPassword", new HashSet<String>(Arrays.asList(new String[] {"{CLEAR}5155"})));
		repo.create(null, IdType.USER, "9170000000",param);
		
		assertTrue(repo.isExists(null, IdType.USER, "9170000000"));
		
		Set<String> fields=new HashSet<String>(Arrays.asList(new String[]{"update-uid","update-sunidentitymsisdnnumber"}));
		assertTrue(repo.getAttributes(null, IdType.USER, "9170000000",fields).containsKey("update-sunidentitymsisdnnumber"));
		assertTrue(repo.getAttributes(null, IdType.USER, "9170000000",fields).containsKey("update-uid"));
		System.out.println(repo.getAttributes(null, IdType.USER, "9170000000",fields));
	}
	
	@Test
	public void test_isAdd_false() throws SSOException, IdRepoException {
		repo.delete(null,IdType.USER, "9170000000");
		
		TreeMap<String, Set<String>> param=new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);
		param.put("xxx", new HashSet<String>(Arrays.asList(new String[] {"9170000000+"})));
		
		repo.setAttributes(null,IdType.USER, "9170000000", param, false);
		assertTrue(repo.isExists(null, IdType.USER, "9170000000"));
		assertTrue(repo.isActive(null, IdType.USER, "9170000000"));
		
		repo.removeAttributes(null,IdType.USER, "9170000000",new HashSet<String>(Arrays.asList(new String[] {"inetuserstatus"})));
		assertTrue(repo.isExists(null, IdType.USER, "9170000000"));
		assertTrue(repo.isActive(null, IdType.USER, "9170000000"));
		
		repo.removeAttributes(null,IdType.USER, "9170000000",new HashSet<String>(Arrays.asList(new String[] {"uid"})));
		assertFalse(repo.isExists(null, IdType.USER, "9170000000"));
		assertFalse(repo.isActive(null, IdType.USER, "9170000000"));
		
		repo.setAttributes(null,IdType.USER, "9170000000", param, false);
		assertTrue(repo.isExists(null, IdType.USER, "9170000000"));
		assertTrue(repo.isActive(null, IdType.USER, "9170000000"));
	}

	@Test
	public void updated_created_fields_test() throws SSOException, IdRepoException{
		repo.delete(null, IdType.USER, "9170000000");

		Map<String, Set<String>> param = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		param.put("CN", new HashSet<>(Arrays.asList("ssss")));
		param.put("sunidentitymsisdnnumber", new HashSet<>(Arrays.asList("9170000000")));
		param.put("userPassword", new HashSet<>(Arrays.asList("{CLEAR}5155")));
		repo.create(null, IdType.USER, "9170000000",param);

		assertTrue(repo.isExists(null, IdType.USER, "9170000000"));

		Set<String> fields= new HashSet<>(Arrays.asList("_created", "_updated"));
		Map<String, Set<String>> fieldsValues = repo.getAttributes(null, IdType.USER, "9170000000", fields);
		assertTrue(fieldsValues.containsKey("_created"));
		assertTrue(fieldsValues.containsKey("_updated"));
		long created = Long.parseLong(fieldsValues.get("_created").stream().findFirst().get());
		long updated = Long.parseLong(fieldsValues.get("_updated").stream().findFirst().get());
		assertTrue(created < updated);
		assertEquals(System.currentTimeMillis() / 1000, created / 1000);
		assertEquals(System.currentTimeMillis() / 1000, updated / 1000);

		System.out.println(repo.getAttributes(null, IdType.USER, "9170000000",fields));
	}
}
