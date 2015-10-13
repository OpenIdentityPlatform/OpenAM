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
 * Copyright 2015 ForgeRock AS.
 */

define("org/forgerock/openam/server/util/QRCodeReader", [
    "jquery",
    "qrcode"
], function ($, QRCodeReader) {

    var obj = {},
        getCode = function (options) {
            var qr = new QRCodeReader(options.version || 4, options.code || "M");
            qr.addData(options.text);
            qr.make();

            return qr.createImgTag();
        };

    /**
     * Creates QRCode and places it on in the target id.
     *
     * This method is called by <strong>org.forgerock.openam.utils.qr</strong> via the
     * ScriptTextOutputCallback in the RestLoginView.
     *
     * @param  {Object} options takes the 4 params below:
     * @param  {String} version - used to generate QR code.
     * @param  {String} code - used to generate QR code.
     * @param  {String} text - used to generate QR code.
     * @param  {String} id - used to select target.
     */
    obj.createCode = function (options) {
        var code = getCode(options),
            element = $("<div class='text-center'/>");
        element.append(code);

        $("#" + options.id).append(element);

    };

    return obj;
});
