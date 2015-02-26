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
 * $Id: AccountConverter.java,v 1.2 2009/06/11 05:29:42 superpat7 Exp $
 */

package org.opensso.c1demoserver.converter;

import java.net.URI;
import org.opensso.c1demoserver.model.Account;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.ws.rs.core.UriBuilder;
import javax.persistence.EntityManager;
import java.util.Collection;
import org.opensso.c1demoserver.model.Phone;

@XmlRootElement(name = "account")
public class AccountConverter {
    private Account entity;
    private URI uri;
    private int expandLevel;
  
    /** Creates a new instance of AccountConverter */
    public AccountConverter() {
        entity = new Account();
    }

    /**
     * Creates a new instance of AccountConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param expandLevel indicates the number of levels the entity graph should be expanded@param isUriExtendable indicates whether the uri can be extended
     */
    public AccountConverter(Account entity, URI uri, int expandLevel, boolean isUriExtendable) {
        this.entity = entity;
        this.uri = (isUriExtendable) ? UriBuilder.fromUri(uri).path(entity.getAccountNumber() + "/").build() : uri;
        this.expandLevel = expandLevel;
        getPhoneCollection();
    }

    /**
     * Creates a new instance of AccountConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param expandLevel indicates the number of levels the entity graph should be expanded
     */
    public AccountConverter(Account entity, URI uri, int expandLevel) {
        this(entity, uri, expandLevel, false);
    }

    /**
     * Getter for accountNumber.
     *
     * @return value for accountNumber
     */
    @XmlElement
    public String getAccountNumber() {
        return (expandLevel > 0) ? entity.getAccountNumber() : null;
    }

    /**
     * Setter for accountNumber.
     *
     * @param value the value to set
     */
    public void setAccountNumber(String value) {
        entity.setAccountNumber(value);
    }

    /**
     * Getter for billToAddressLine1.
     *
     * @return value for billToAddressLine1
     */
    @XmlElement
    public String getBillToAddressLine1() {
        return (expandLevel > 0) ? entity.getBillToAddressLine1() : null;
    }

    /**
     * Setter for billToAddressLine1.
     *
     * @param value the value to set
     */
    public void setBillToAddressLine1(String value) {
        entity.setBillToAddressLine1(value);
    }

    /**
     * Getter for billToAddressLine2.
     *
     * @return value for billToAddressLine2
     */
    @XmlElement
    public String getBillToAddressLine2() {
        return (expandLevel > 0) ? entity.getBillToAddressLine2() : null;
    }

    /**
     * Setter for billToAddressLine2.
     *
     * @param value the value to set
     */
    public void setBillToAddressLine2(String value) {
        entity.setBillToAddressLine2(value);
    }

    /**
     * Getter for billToCity.
     *
     * @return value for billToCity
     */
    @XmlElement
    public String getBillToCity() {
        return (expandLevel > 0) ? entity.getBillToCity() : null;
    }

    /**
     * Setter for billToCity.
     *
     * @param value the value to set
     */
    public void setBillToCity(String value) {
        entity.setBillToCity(value);
    }

    /**
     * Getter for billToState.
     *
     * @return value for billToState
     */
    @XmlElement
    public String getBillToState() {
        return (expandLevel > 0) ? entity.getBillToState() : null;
    }

    /**
     * Setter for billToState.
     *
     * @param value the value to set
     */
    public void setBillToState(String value) {
        entity.setBillToState(value);
    }

    /**
     * Getter for billToZip.
     *
     * @return value for billToZip
     */
    @XmlElement
    public String getBillToZip() {
        return (expandLevel > 0) ? entity.getBillToZip() : null;
    }

    /**
     * Setter for billToZip.
     *
     * @param value the value to set
     */
    public void setBillToZip(String value) {
        entity.setBillToZip(value);
    }

    /**
     * Getter for planMinutes.
     *
     * @return value for planMinutes
     */
    @XmlElement
    public Integer getPlanMinutes() {
        return (expandLevel > 0) ? entity.getPlanMinutes() : null;
    }

    /**
     * Setter for planMinutes.
     *
     * @param value the value to set
     */
    public void setPlanMinutes(Integer value) {
        entity.setPlanMinutes(value);
    }

    /**
     * Getter for planId.
     *
     * @return value for planId
     */
    @XmlElement
    public Integer getPlanId() {
        return (expandLevel > 0) ? entity.getPlanId() : null;
    }

    /**
     * Setter for planId.
     *
     * @param value the value to set
     */
    public void setPlanId(Integer value) {
        entity.setPlanId(value);
    }

    /**
     * Getter for phoneCollection.
     *
     * @return value for phoneCollection
     */
    @XmlElement
    public PhonesConverter getPhoneCollection() {
        if (expandLevel > 0) {
            if (entity.getPhoneCollection() != null) {
                return new PhonesConverter(entity.getPhoneCollection(), uri.resolve("phoneCollection/"), expandLevel - 1);
            }
        }
        return null;
    }

    /**
     * Setter for phoneCollection.
     *
     * @param value the value to set
     */
    public void setPhoneCollection(PhonesConverter value) {
        entity.setPhoneCollection((value != null) ? value.getEntities() : null);
    }

    /**
     * Returns the URI associated with this converter.
     *
     * @return the uri
     */
    @XmlAttribute
    public URI getUri() {
        return uri;
    }

    /**
     * Sets the URI for this reference converter.
     *
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Returns the Account entity.
     *
     * @return an entity
     */
    @XmlTransient
    public Account getEntity() {
        if (entity.getAccountNumber() == null) {
            AccountConverter converter = UriResolver.getInstance().resolve(AccountConverter.class, uri);
            if (converter != null) {
                entity = converter.getEntity();
            }
        }
        return entity;
    }

    /**
     * Returns the resolved Account entity.
     *
     * @return an resolved entity
     */
    public Account resolveEntity(EntityManager em) {
        Collection<Phone> phoneCollection = entity.getPhoneCollection();
        Collection<Phone> newphoneCollection = new java.util.ArrayList<Phone>();
        for (Phone item : phoneCollection) {
            newphoneCollection.add(em.getReference(Phone.class, item.getPhoneNumber()));
        }
        entity.setPhoneCollection(newphoneCollection);
        return entity;
    }
}
