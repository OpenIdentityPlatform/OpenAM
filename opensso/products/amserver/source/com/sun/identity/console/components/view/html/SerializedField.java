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
 * $Id: SerializedField.java,v 1.4 2008/08/19 19:09:06 veiming Exp $
 *
 */

package com.sun.identity.console.components.view.html;
import com.iplanet.jato.model.Model;
import com.iplanet.jato.util.Encoder;
import com.iplanet.jato.view.ContainerView;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.html.HtmlDisplayFieldBase;
import java.io.IOException;
import java.io.Serializable;


/**
 * This class takes a serializable object, serialized it when
 * adding as a hidden field in a form (for information tracking
 * between pages) and deserialized it when retrieving during
 * form submission.
 * This component is proprietary to OpenSSO Administration Console
 */
public class SerializedField extends HtmlDisplayFieldBase
{
    /**
     * Constructs  a minimal instance using the parent's default model
     * and the field's name as its bound name
     *
     * @param parent view of this object
     * @param name and model bound name. 
     * @param value - serializable object 
     */
    public SerializedField(
        ContainerView parent,
        String name,
        Serializable value
    ) {
        super(parent, parent.getDefaultModel(), name, name, null);
        setValue(value, false);
    }

    /**
     * Constructs an instance using the specified model and the
     * field's name as its bound name
     *
     * @param parent view of this object
     * @param model to which this <code>DisplayField</code> is bound
     * @param name and model bound name. 
     * @param value - serializable object
     */
    public SerializedField(
        View parent,
        Model model,
        String name,
        Serializable value
    ) {
        super(parent, model, name, name, null);
        setValue(value, false);
    }


    /**
     * Constructs this component directly
     *
     * @param parent view of this object
     * @param model to which this <code>DisplayField</code> is bound
     * @param name  this view's name. 
     * @param boundName  name of the model field to which this 
     *        <code>DisplayField</code> is bound
     * @param value - serializable object
     */
    public SerializedField(
        View parent,
        Model model,
        String name, 
        String boundName,
        Serializable value
    ) {
        super(parent, model, name, boundName, null);
        setValue(value, false);
    }

    /**
     * Gets serializable object
     *
     * @return serializable object
     */
    public Object getSerializedObj() {
        Object obj = null;
        String serializedStr = (String)super.getValue();

        if (serializedStr != null) {
            try {
                obj = Encoder.deserialize(Encoder.decode(serializedStr), true);
            } catch (IOException ioe) {
                obj = null;
            } catch (ClassNotFoundException cnfe) {
                obj = null;
            }
        }

        return obj;
    }

    /**
     * Set serializable object
     *
     * @param obj  serializable object
     */
    public void setValue(Object obj) {
        setValue(obj, true);
    }

    /**
     * Set serialized string.
     *
     * @param str Serizalized String.
     */
    public void setSerializedString(String str) {
        super.setValue(str);
    }

    /**
     * Set serializable object
     *
     * @param obj  serializable object
     * @param overwrite - true to overwrite existing value
     */
    public void setValue(Object obj, boolean overwrite) {
        if (!overwrite) {
            Object object = getValue();
            if (object == null) {
                overwrite = true;
            }
        }

        if (overwrite) {
            if (obj != null) {
                try {
                    super.setValue(Encoder.encode(
                        Encoder.serialize((Serializable)obj, true)));
                } catch (IOException ioe) {
                    super.setValue(null);
                }
            } else {
                super.setValue(null);
            }
        }
    }
}
