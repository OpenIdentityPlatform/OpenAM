package org.openidentityplatform.openam.authentication.modules;

import java.security.Principal;

public class QRPrincipal implements Principal, java.io.Serializable {
	private static final long serialVersionUID = 8178887762192702526L;
	private String name;

    public QRPrincipal(String name) {
        if(name == null) {
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }

    public String getName() {
    	return this.name;
    }

    public String toString() {
        return(getClass().getSimpleName().concat(":  ").concat(name));
    }

    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }
        if(this == o) {
            return true;
        }
        if(!(o instanceof QRPrincipal)) {
            return false;
        }
        QRPrincipal that = (QRPrincipal)o;

        if(this.getName().equals(that.getName())) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return name.hashCode();
    }
}
