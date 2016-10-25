/**
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
 * Copyright 2016 ForgeRock AS.
 */

import React from "react";

/**
 * A simple page header with buttons and without icon.
 * @module components/SimplePageHeader
 * @param {Object} props Properties passed to this component
 * @param {ReactNode} props.children Buttons to add within this header
 * @param {string} props.title Text to display for the header
 * @returns {ReactElement} Renderable React element
 */
const SimplePageHeader = ({ children, title }) => (
    <header className="page-header page-header-no-border clearfix">
        <div className="shallow-page-header-button-group button-group pull-right">
            { children }
        </div>

        <div className="pull-left">
            <h1 className="wordwrap">{ title }</h1>
        </div>
    </header>
);

SimplePageHeader.propTypes = {
    children: React.PropTypes.node.isRequired,
    title: React.PropTypes.string.isRequired
};

export default SimplePageHeader;
