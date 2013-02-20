/*
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
 */

package org.forgerock.identity.authentication.modules.common;

import org.forgerock.identity.authentication.modules.common.config.AttributeNameMapping;

public class SampleConfigurationClass {
	
	@AttributeNameMapping("a")
	private Long sampleLongAttr;
	
	@AttributeNameMapping("b")
	private String sampleStringAttr;
	
	@AttributeNameMapping("c")
	private Boolean sampleBooleanAttr;
	
	@AttributeNameMapping("d")
	private Integer sampleIntegerAttr;
	
	@AttributeNameMapping("e")
	private Integer sampleNullIntegerAttr;
	
	public Long getSampleLongAttr() {
		return sampleLongAttr;
	}
	public void setSampleLongAttr(Long sampleLongAttr) {
		this.sampleLongAttr = sampleLongAttr;
	}
	public String getSampleStringAttr() {
		return sampleStringAttr;
	}
	public void setSampleStringAttr(String sampleStringAttr) {
		this.sampleStringAttr = sampleStringAttr;
	}
	public Integer getSampleIntegerAttr() {
		return sampleIntegerAttr;
	}
	public void setSampleIntegerAttr(Integer sampleIntegerAttr) {
		this.sampleIntegerAttr = sampleIntegerAttr;
	}
	public Boolean getSampleBooleanAttr() {
		return sampleBooleanAttr;
	}
	public void setSampleBooleanAttr(Boolean sampleBooleanAttr) {
		this.sampleBooleanAttr = sampleBooleanAttr;
	}
	public Integer getSampleNullIntegerAttr() {
		return sampleNullIntegerAttr;
	}
	public void setSampleNullIntegerAttr(Integer sampleNullIntegerAttr) {
		this.sampleNullIntegerAttr = sampleNullIntegerAttr;
	}
	@Override
	public String toString() {
		return "SampleConfigurationClass [sampleLongAttr=" + sampleLongAttr
				+ ", sampleStringAttr=" + sampleStringAttr
				+ ", sampleIntegerAttr=" + sampleIntegerAttr
				+ ", sampleBooleanAttr=" + sampleBooleanAttr
				+ ", sampleNullIntegerAttr=" + sampleNullIntegerAttr + "]";
	}
	
}
