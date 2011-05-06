/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AttributePersistenceFactory.java,v 1.1 2009/04/24 21:01:57 rparekh Exp $
 *
 */ 

package com.sun.identity.openid.persistence;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.identity.openid.provider.Config;

public class AttributePersistenceFactory implements AttributePersistor {

	private static AttributePersistenceFactory instance;
	private static final String DEFAULT_PERSISTENCE_CLASS = "com.sun.identity.openid.persistence.LDAPPersistor";

	private Class persistenceImpl = null;

	public static AttributePersistenceFactory getInstance(){
		if(instance==null){
			instance = new AttributePersistenceFactory();
		}
		return instance;
	}

	/*
	 * The constructor is kept private to force the use of the singleton getInstance()
	 */
	private AttributePersistenceFactory() {
		String pi = Config.getString(Config.PERSISTENCE_IMPL);
		if (pi == null || pi.equals("")) {
			pi = DEFAULT_PERSISTENCE_CLASS;
			
			Logger.getLogger(AttributePersistenceFactory.class.getName()).log(
					Level.WARNING,
					"No Attribute Persistor specified in Provider.properties via property [" + Config.PERSISTENCE_IMPL + "]\n"+
					"Default class " + pi + " will be used.");
		}
		
		
		try {
			persistenceImpl = this.getClass().getClassLoader()
					.loadClass(pi);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (Exception e){
			e.printStackTrace();
		}
		


		if (persistenceImpl == null) {
			Logger.getLogger(AttributePersistenceFactory.class.getName()).log(
					Level.WARNING,"Could not load AttributePersistor with getClass().getClassLoader()\nTrying Class.forName()");
			
			try {
				persistenceImpl = Class.forName(pi);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e){
				e.printStackTrace();
			}

		}

		if (persistenceImpl == null) {
			Logger.getLogger(AttributePersistenceFactory.class.getName()).log(
					Level.SEVERE,
					"Cannot load class " + pi
							+ "\nAttribute Persistence will be disabled");
		}

	}

	private AttributePersistor getAttributePersistor() {
		AttributePersistor persistor = null;
		try {
			persistor = (AttributePersistor) persistenceImpl.newInstance();

		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return persistor;
	}

//	@Override
	public Map<String,String> getAttributes(String uid, String rp) throws BackendException {
		if (persistenceImpl == null) {
			return null;
		}
		return getAttributePersistor().getAttributes(uid, rp);

	}

//	@Override
	public void setAttributes(String uid, String rp, Map<String,String> attributes) throws BackendException {
		if (persistenceImpl == null) {
			return ;
		}
		getAttributePersistor().setAttributes(uid, rp, attributes);

	}

}
