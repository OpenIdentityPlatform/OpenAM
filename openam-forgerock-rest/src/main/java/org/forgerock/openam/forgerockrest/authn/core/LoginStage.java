package org.forgerock.openam.forgerockrest.authn.core;

/**
 * The possible stages of the Login process.
 *
 * Can only be in a stage where there are requirements(callbacks) to be submitted or all requirements have been
 * submitted and the login process has completed and the status of the login process will need to be check to
 * determine the actual outcome of the login process.
 */
public enum LoginStage {

    REQUIREMENTS_WAITING,
    COMPLETE;
}
