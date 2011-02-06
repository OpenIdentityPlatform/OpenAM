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
 * $Id: PersistenceService.java,v 1.2 2009/06/11 05:29:44 superpat7 Exp $
 */

package org.opensso.c1demoserver.service;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;

public class PersistenceService {
    private static String DEFAULT_PU = "C1DemoServerPU";
  
    private static ThreadLocal<PersistenceService> instance = new ThreadLocal<PersistenceService>() {
        @Override
        protected PersistenceService initialValue() {
            return new PersistenceService();
        }
    };
    
    private EntityManager em;

    private UserTransaction utx;
    
    private PersistenceService() { 
        try {
            this.em = (EntityManager) new InitialContext().lookup("java:comp/env/persistence/" + DEFAULT_PU);
            this.utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        } catch (NamingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns an instance of PersistenceService.
     * 
     * @return an instance of PersistenceService
     */
    public static PersistenceService getInstance() {
        return instance.get();
    }
    

    private static void removeInstance() {
        instance.remove();
    }

    /**
     * Returns an instance of EntityManager.
     *
     * @return an instance of EntityManager
     */
    public EntityManager getEntityManager() {
        return em;
    }

    /**
     * Begins a resource transaction.
     */
    public void beginTx() {
        try {
            utx.begin();
            em.joinTransaction();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Commits a resource transaction.
     */
    public void commitTx() {
        try {
            utx.commit();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Rolls back a resource transaction.
     */
    public void rollbackTx() {
        try {
            utx.rollback();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Closes this instance.
     */
    public void close() {
        removeInstance();
    }
}
