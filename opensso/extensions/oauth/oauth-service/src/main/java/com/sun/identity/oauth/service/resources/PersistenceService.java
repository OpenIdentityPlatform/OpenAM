/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.resources;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.UserTransaction;

/**
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */
public class PersistenceService {
    private static String DEFAULT_PU = "TokenServicePU";

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


////////
    /**
     * Refreshes the state of the given entity from the database.
     *
     * @param entity the entity to refresh
     */
    public void refreshEntity(Object entity) {
        em.refresh(entity);
    }

    /**
     * Merges the state of the given entity into the current persistence context.
     *
     * @param entity the entity to merge
     * @return the merged entity
     */
    public <T> T mergeEntity(T entity) {
        return (T) em.merge(entity);
    }

    /**
     * Makes the given entity managed and persistent.
     *
     * @param entity the entity to persist
     */
    public void persistEntity(Object entity) {
        em.persist(entity);
    }

    /**
     * Removes the entity instance.
     *
     * @param entity the entity to remove
     */
    public void removeEntity(Object entity) {
        em.remove(entity);
    }

    /**
     * Resolves the given entity to the actual entity instance in the current persistence context.
     *
     * @param entity the entity to resolve
     * @return the resolved entity
     */
    public <T> T resolveEntity(T entity) {
        entity = mergeEntity(entity);
        em.refresh(entity);

        return entity;
    }

    /**
     * Returns an instance of Query for executing a named query.
     *
     * @param query the named query
     * @return an instance of Query
     */
    public Query createNamedQuery(String query) {
        return em.createNamedQuery(query);
    }

    /**
     * Returns an instance of Query for executing a query.
     *
     * @param query the query string
     * @return an instance of Query
     */
    public Query createQuery(String query) {
        return em.createQuery(query);
    }

    /**
     * Returns an instance of Query for executing a query.
     *
     * @param query the query string
     * @return an instance of Query
     */
    public Query createNativeQuery(String query) {
        return em.createNativeQuery(query);
    }

////////





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