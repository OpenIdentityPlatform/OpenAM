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
 * A block.
 * @module components/Block
 * @param {Object} props Properties passed to this component
 * @param {ReactNode[]} props.children Children to add within this component
 * @param {string} props.header Text to display for the block header
 * @param {string} [props.description] Text to display for the block description
 * @returns {ReactElement} Renderable React element
 */
const Block = ({ children, header, description }) => {

    const blockDescription = description ? <p className="block-description">{ description }</p> : undefined;

    return (
        <div className="block clearfix">
            <h3 className="block-header">{ header }</h3>
            { blockDescription }
            { children }
        </div>
    );
};

Block.propTypes = {
    children: React.PropTypes.arrayOf(React.PropTypes.node).isRequired,
    description: React.PropTypes.string,
    header: React.PropTypes.string.isRequired
};

export default Block;
