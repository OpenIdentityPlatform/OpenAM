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
 * $Id: ChallengeConverter.java,v 1.2 2009/06/11 05:29:42 superpat7 Exp $
 */

package org.opensso.c1demoserver.converter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opensso.c1demoserver.model.Phone;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.ws.rs.core.UriBuilder;

@XmlRootElement(name = "challenge")
public class ChallengeConverter {
    private Phone entity;
    private URI uri;
    private int expandLevel;
  
    /** Creates a new instance of ChallengeConverter */
    public ChallengeConverter() {
        entity = new Phone();
    }

    /**
     * Creates a new instance of ChallengeConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param expandLevel indicates the number of levels the entity graph should be expanded@param isUriExtendable indicates whether the uri can be extended
     */
    public ChallengeConverter(Phone entity, URI uri, int expandLevel, boolean isUriExtendable) {
        this.entity = entity;
        this.uri = (isUriExtendable) ? UriBuilder.fromUri(uri).path(entity.getPhoneNumber() + "/").build() : uri;
        this.expandLevel = expandLevel;
    }

    /**
     * Creates a new instance of ChallengeConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param expandLevel indicates the number of levels the entity graph should be expanded
     */
    public ChallengeConverter(Phone entity, URI uri, int expandLevel) {
        this(entity, uri, expandLevel, false);
    }

    /**
     * Getter for challengeQuestion.
     *
     * @return value for challengeQuestion
     */
    @XmlElement
    public List<String> getChallengeQuestion() {
        if ( expandLevel <= 0 ) {
            return null;
        }

        ArrayList<String> challengeQuestions = new ArrayList<String>();

        String challengeQuestion = entity.getAccountNumber().getChallengeQuestion();

        if ( challengeQuestion == null ) {
            // No question on account - ask for credit card details
            challengeQuestions.add("Enter the last four digits of the credit card used to open this account");
            challengeQuestions.add("Enter the verification number of the credit card used to open this account");
        } else {
            challengeQuestions.add(challengeQuestion);
        }

        return challengeQuestions;
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
     * Returns the Phone entity.
     *
     * @return an entity
     */
    @XmlTransient
    public Phone getEntity() {
        if (entity.getPhoneNumber() == null) {
            ChallengeConverter converter = UriResolver.getInstance().resolve(ChallengeConverter.class, uri);
            if (converter != null) {
                entity = converter.getEntity();
            }
        }
        return entity;
    }
}
