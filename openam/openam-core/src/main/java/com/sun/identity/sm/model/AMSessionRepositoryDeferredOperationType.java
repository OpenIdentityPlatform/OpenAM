package com.sun.identity.sm.model;

/**
 * Type of AM Session Repository Deferred Operation to be performed.
 *
 * @author  jeff.schenk@forgerock.com
 */
public enum AMSessionRepositoryDeferredOperationType {

    READ(0),
    WRITE(1),
    UPDATE(2),
    STORE(3),
    DELETE(4),
    STATUS(5);

    private final int index;

    private AMSessionRepositoryDeferredOperationType(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public String indexString() {
        return Integer.toString(index);
    }

}
