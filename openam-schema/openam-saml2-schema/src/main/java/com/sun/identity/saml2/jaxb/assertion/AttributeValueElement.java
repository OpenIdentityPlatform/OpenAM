package com.sun.identity.saml2.jaxb.assertion;

import java.util.ArrayList;
import java.util.List;

/** Compatibility shim for JAXB 1.x AttributeValueElement (was extending AnyType). */
public class AttributeValueElement {
    private List<Object> content = new ArrayList<>();

    public List<Object> getContent() {
        return content;
    }
}
