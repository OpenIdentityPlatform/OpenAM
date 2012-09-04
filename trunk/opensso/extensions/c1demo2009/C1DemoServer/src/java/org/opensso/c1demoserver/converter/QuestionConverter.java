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
 * $Id: QuestionConverter.java,v 1.2 2009/06/11 05:29:42 superpat7 Exp $
 */

package org.opensso.c1demoserver.converter;

import java.net.URI;
import org.opensso.c1demoserver.model.Question;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.persistence.EntityManager;

@XmlRootElement(name = "question")
public class QuestionConverter {
    private Question entity;
    private URI uri;
    private int expandLevel;
  
    /** Creates a new instance of NotificationConverter */
    public QuestionConverter() {
        entity = new Question();
    }

    /**
     * Creates a new instance of NotificationConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param expandLevel indicates the number of levels the entity graph should be expanded
     * @param isUriExtendable indicates whether the uri can be extended
     */
    public QuestionConverter(Question entity, URI uri, int expandLevel, boolean isUriExtendable) {
        this.entity = entity;
        this.uri = uri;
        this.expandLevel = expandLevel;
    }

    /**
     * Creates a new instance of NotificationConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param expandLevel indicates the number of levels the entity graph should be expanded
     */
    public QuestionConverter(Question entity, URI uri, int expandLevel) {
        this(entity, uri, expandLevel, false);
    }

    /**
     * Getter for messageText.
     *
     * @return value for messageText
     */
    @XmlElement
    public String getQuestionText() {
        return (expandLevel > 0) ? entity.getQuestionText() : null;
    }

    /**
     * Setter for messageText.
     *
     * @param value the value to set
     */
    public void setQuestionText(String value) {
        entity.setQuestionText(value);
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
     * Returns the Notification entity.
     *
     * @return an entity
     */
    @XmlTransient
    public Question getEntity() {
        if (entity.getQuestionText() == null) {
            QuestionConverter converter = UriResolver.getInstance().resolve(QuestionConverter.class, uri);
            if (converter != null) {
                entity = converter.getEntity();
            }
        }
        return entity;
    }

    /**
     * Returns the resolved Notification entity.
     *
     * @return an resolved entity
     */
    public Question resolveEntity(EntityManager em) {
        return entity;
    }
}
