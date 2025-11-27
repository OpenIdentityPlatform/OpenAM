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
 * Copyright 2025 3A Systems LLC.
 */

import type { ActionElements } from "./types";

const DefaultActionElements: ActionElements = ({ callbacks }) => {

    const defaultAction = <input type="submit" className="primary-button" value="Log In" name="IDButton" />
    const callbackIdx = callbacks.findIndex((cb) => (cb.type === 'ConfirmationCallback'));
    if (callbackIdx < 0) {
        return defaultAction;
    }
    const opts = callbacks[callbackIdx].output.find((o) => (o.name === 'options'))?.value;
    if (!Array.isArray(opts)) {
        return defaultAction;
    }

    return <>{opts.map((o, i) =>
        <input className={i == 0 ? "primary-button" : "secondary-button"} key={"IDButton" + i}
            name={"IDButton" + i} type="submit" value={o} />)}
    </>;
}

export default DefaultActionElements