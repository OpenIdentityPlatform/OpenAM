package com.sun.identity.authentication.callbacks;

import java.io.Serializable;

import javax.security.auth.callback.Callback;

public class NameValueOutputCallback implements Callback, Serializable {

	private static final long serialVersionUID = -521675595179132274L;

	private String name;
	
	private String value;
	
	public NameValueOutputCallback(String name, String value) {
        this.name = name;
        this.value = value;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}


