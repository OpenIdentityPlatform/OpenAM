/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: JDBCStore.java,v 1.4 2008/06/25 05:42:36 qcheng Exp $
 *
 */
package com.sun.identity.config.pojos;

/**
 * @author Jeffrey Bermudez
 */
public class JDBCStore extends AuthenticationStore {

    private String connectionType;
    private String storeDetails;
    private String jdbcURL;
    private String jndiName;
    private String username;
    private String password;
    private String passwordField;
    private String retrievalStatement;
    private String transformationClass;


    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getStoreDetails() {
        return storeDetails;
    }

    public void setStoreDetails(String storeDetails) {
        this.storeDetails = storeDetails;
    }

    public String getJdbcURL() {
        return jdbcURL;
    }

    public void setJdbcURL(String jdbcURL) {
        this.jdbcURL = jdbcURL;
    }

    public String getJndiName() {
        return jndiName;
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordField() {
        return passwordField;
    }

    public void setPasswordField(String passwordField) {
        this.passwordField = passwordField;
    }

    public String getRetrievalStatement() {
        return retrievalStatement;
    }

    public void setRetrievalStatement(String retrievalStatement) {
        this.retrievalStatement = retrievalStatement;
    }

    public String getTransformationClass() {
        return transformationClass;
    }

    public void setTransformationClass(String transformationClass) {
        this.transformationClass = transformationClass;
    }

}
