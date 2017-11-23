/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.idm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdServices;
import com.sun.identity.idm.IdType;
import com.sun.identity.sm.SchemaType;

import javax.security.auth.callback.Callback;
import java.util.Map;
import java.util.Set;

/**
 * Decorator pattern base class for {@link com.sun.identity.idm.IdServices} implementations. Provides default
 * implementations of all required methods, allowing sub-classes to just override those methods that they are interested
 * in. All default implementations simply forward method calls on to the delegate.
 *
 * @since 12.0.0
 */
public class IdServicesDecorator implements IdServices {
    private final IdServices delegate;

    /**
     * Constructs the decorator using the given delegate implementation.
     *
     * @param delegate a non-null IdServices implementation to delegate calls to.
     * @throws NullPointerException if the delegate is null.
     */
    protected IdServicesDecorator(final IdServices delegate) {
        checkNotNull(delegate, null);
        this.delegate = delegate;
    }

    protected IdServices getDelegate() {
        return delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authenticate(String orgName, Callback[] credentials) throws IdRepoException, AuthLoginException {
        return delegate.authenticate(orgName, credentials);
    }

    @Override
    public boolean authenticate(String orgName, Callback[] credentials, IdType idType) throws IdRepoException, AuthLoginException {
        return delegate.authenticate(orgName, credentials, idType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AMIdentity create(SSOToken token, IdType type, String name, Map attrMap, String amOrgName)
            throws IdRepoException, SSOException {
        return delegate.create(token, type, name, attrMap, amOrgName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(SSOToken token, IdType type, String name, String orgName, String amsdkDN)
            throws IdRepoException, SSOException {
        delegate.delete(token, type, name, orgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getAttributes(SSOToken token, IdType type, String name, Set attrNames, String amOrgName, String amsdkDN,
                             boolean isString) throws IdRepoException, SSOException {
        return delegate.getAttributes(token, type, name, attrNames, amOrgName, amsdkDN, isString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getAttributes(SSOToken token, IdType type, String name, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException {
        return delegate.getAttributes(token, type, name, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set getMembers(SSOToken token, IdType type, String name, String amOrgName, IdType membersType,
                          String amsdkDN) throws IdRepoException, SSOException {
        return delegate.getMembers(token, type, name, amOrgName, membersType, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set getMemberships(SSOToken token, IdType type, String name, IdType membershipType, String amOrgName,
                              String amsdkDN) throws IdRepoException, SSOException {
        return delegate.getMemberships(token, type, name, membershipType, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExists(SSOToken token, IdType type, String name, String amOrgName)
            throws SSOException, IdRepoException {
        return delegate.isExists(token, type, name, amOrgName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive(SSOToken token, IdType type, String name, String amOrgName, String amsdkDN)
            throws SSOException, IdRepoException {
        return delegate.isActive(token, type, name, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setActiveStatus(SSOToken token, IdType type, String name, String amOrgName, String amsdkDN,
                                boolean active) throws SSOException, IdRepoException {
        delegate.setActiveStatus(token, type, name, amOrgName, amsdkDN, active);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyMemberShip(SSOToken token, IdType type, String name, Set members, IdType membersType,
                                 int operation, String amOrgName) throws IdRepoException, SSOException {
        delegate.modifyMemberShip(token, type, name, members, membersType, operation, amOrgName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAttributes(SSOToken token, IdType type, String name, Set attrNames, String amOrgName,
                                 String amsdkDN) throws IdRepoException, SSOException {
        delegate.removeAttributes(token, type, name, attrNames, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdSearchResults search(SSOToken token, IdType type, String pattern, IdSearchControl ctrl, String amOrgName)
            throws IdRepoException, SSOException {
        return delegate.search(token, type, pattern, ctrl, amOrgName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttributes(SSOToken token, IdType type, String name, Map attributes, boolean isAdd, String amOrgName,
                              String amsdkDN, boolean isString) throws IdRepoException, SSOException {
        delegate.setAttributes(token, type, name, attributes, isAdd, amOrgName, amsdkDN, isString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changePassword(SSOToken token, IdType type, String name, String oldPassword, String newPassword,
                               String amOrgName, String amsdkDN) throws IdRepoException, SSOException {
        delegate.changePassword(token, type, name, oldPassword, newPassword, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set getAssignedServices(SSOToken token, IdType type, String name, Map mapOfServiceNamesAndOCs,
                                   String amOrgName, String amsdkDN) throws IdRepoException, SSOException {
        return delegate.getAssignedServices(token, type, name, mapOfServiceNamesAndOCs, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void assignService(SSOToken token, IdType type, String name, String serviceName, SchemaType stype,
                              Map attrMap, String amOrgName, String amsdkDN) throws IdRepoException, SSOException {
        delegate.assignService(token, type, name, serviceName, stype, attrMap, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unassignService(SSOToken token, IdType type, String name, String serviceName, Map attrMap,
                                String amOrgName, String amsdkDN) throws IdRepoException, SSOException {
        delegate.unassignService(token, type, name, serviceName, attrMap, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getServiceAttributes(SSOToken token, IdType type, String name, String serviceName, Set attrNames,
                                    String amOrgName, String amsdkDN) throws IdRepoException, SSOException {
        return delegate.getServiceAttributes(token, type, name, serviceName, attrNames, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getBinaryServiceAttributes(SSOToken token, IdType type, String name, String serviceName, Set attrNames,
                                          String amOrgName, String amsdkDN) throws IdRepoException, SSOException {
        return delegate.getBinaryServiceAttributes(token, type, name, serviceName, attrNames, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getServiceAttributesAscending(SSOToken token, IdType type, String name, String serviceName,
                                             Set attrNames, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException {
        return delegate.getServiceAttributesAscending(token, type, name, serviceName, attrNames, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyService(SSOToken token, IdType type, String name, String serviceName, SchemaType stype,
                              Map attrMap, String amOrgName, String amsdkDN) throws IdRepoException, SSOException {
        delegate.modifyService(token, type, name, serviceName, stype, attrMap, amOrgName, amsdkDN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set getSupportedTypes(SSOToken token, String amOrgName) throws IdRepoException, SSOException {
        return delegate.getSupportedTypes(token, amOrgName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set getSupportedOperations(SSOToken token, IdType type, String amOrgName)
            throws IdRepoException, SSOException {
        return delegate.getSupportedOperations(token, type, amOrgName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearIdRepoPlugins() {
        delegate.clearIdRepoPlugins();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearIdRepoPlugins(String orgName, String serviceComponent, int type) {
        delegate.clearIdRepoPlugins(orgName, serviceComponent, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reloadIdRepoServiceSchema() {
        delegate.reloadIdRepoServiceSchema();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reinitialize() {
        delegate.reinitialize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set getFullyQualifiedNames(SSOToken token, IdType type, String name, String orgName)
            throws IdRepoException, SSOException {
        return delegate.getFullyQualifiedNames(token, type, name, orgName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdSearchResults getSpecialIdentities(SSOToken token, IdType type, String orgName)
            throws IdRepoException, SSOException {
        return delegate.getSpecialIdentities(token, type, orgName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdServicesDecorator)) return false;

        IdServicesDecorator that = (IdServicesDecorator) o;

        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return "IdServicesDecorator{" +
                "delegate=" + delegate +
                '}';
    }

   /**
     * Throws a {@code NullPointerException} if the <tt>object</tt> parameter is
     * null, returns the object otherwise.
     *
     * @param <T>
     *            The type of object to test.
     * @param object
     *            the object to test
     * @param message
     *            a custom exception message to use
     * @return the object
     * @throws NullPointerException
     *             if {@code object} is null
     */
   private <T> T checkNotNull(final T object, final String message) {
       if (object == null) {
           throw new NullPointerException(message);
       }
       return object;
   }
}
