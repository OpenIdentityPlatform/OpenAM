package org.forgerock.openam.oauth2.exceptions;


public class TokenTypeNotFoundException extends Throwable {

    public TokenTypeNotFoundException() {
        super();
    }

    public TokenTypeNotFoundException(String s) {
        super(s);
    }

    public TokenTypeNotFoundException(String s, Throwable e) {
        super(s, e);
    }

}
