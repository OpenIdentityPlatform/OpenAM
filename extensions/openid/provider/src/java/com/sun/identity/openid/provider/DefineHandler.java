/* The contents of this file are subject to the terms
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
 * $Id: DefineHandler.java,v 1.1 2009/04/24 21:01:58 rparekh Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2007 Paul C. Bryan
 */

package com.sun.identity.openid.provider;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;
import javax.el.MethodExpression;
import javax.faces.component.UIComponent;

/**
 * Handles the invocation of the my:define tag, which allows a page to define
 * fields to be handled by a managed bean. This allows the page itself to define
 * what fields are to be handled by a backing bean.
 * 
 * @author pbryan
 */
public final class DefineHandler extends TagHandler {
	/** Specifies Java types of the name and type parameters of the bean method. */
	private static final Class[] PARAM_TYPES = { String.class, String.class };

	/** Expression that specifies the backing bean method to call. */
	private final TagAttribute binding;

	/** Expression that specifies the name of the field being defined. */
	private final TagAttribute name;

	/** Expression that specifies the type of the field being defined. */
	private final TagAttribute type;

	/**
	 * Creates a new instance of the my:define tag handler.
	 * 
	 * @param config
	 *            defines the document definition of the new handler.
	 */
	public DefineHandler(TagConfig config) {
		super(config);

		binding = getRequiredAttribute("binding");
		name = getRequiredAttribute("name");
		type = getRequiredAttribute("type");
	}

	/**
	 * Applies the my:define tag.
	 * 
	 * @param context
	 *            the current FaceletContext instance for this execution.
	 * @param parent
	 *            the parent UIComponent to operate on.
	 */
	public void apply(FaceletContext context, UIComponent parent) {
		MethodExpression expr = binding.getMethodExpression(context,
				Void.class, PARAM_TYPES);

		String[] args = { name.getValue(context), type.getValue(context) };

		expr.invoke(context, args);
	}
}
