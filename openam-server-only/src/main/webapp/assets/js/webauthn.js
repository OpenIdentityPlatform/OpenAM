function bufferDecode(value) {
    return Uint8Array.from(atob(value), c => c.charCodeAt(0));
}

function bufferEncode(bytes) {
    let binary = ''
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
        binary += String.fromCharCode( bytes[ i ] );
    }
    return window.btoa( binary ).replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=/g, "")
}

function processRegistrationChallenge() {
    var challengeStr = getRegistrationChallenge();
    var challenge = JSON.parse(challengeStr);
    challenge.challenge = bufferDecode(challenge.challenge.value);
    challenge.user.id = bufferDecode(challenge.user.id);
    navigator.credentials.create({
        publicKey: challenge,
    }).then((credential) => {
        register(credential);
    }).catch((e) => {
            console.log(e.toString());
        }
    );
}

function getRegistrationChallenge() {
    var querySelector = ".TextOutputCallback_0";
    if(isXUI()) {
        querySelector = "#callback_3";
    }
    return document.querySelector(querySelector).innerText;
}

function isXUI() {
    return !!window.requirejs;
}

function register(credential) {
    var idToken1Sel = "IDToken1";
    var idToken2Sel = "IDToken2";
    var idToken3Sel = "IDToken3";
    var buttonSel = "[name='Login.Submit']";
    if(isXUI()) {
        idToken1Sel = "idToken1";
        idToken2Sel = "idToken2";
        idToken3Sel = "idToken3";
        buttonSel = "#loginButton_0";
    }
    document.getElementById(idToken1Sel).value = credential.id;
    document.getElementById(idToken2Sel).value = bufferEncode( new Uint8Array(credential.response.attestationObject));
    document.getElementById(idToken3Sel).value = bufferEncode( new Uint8Array(credential.response.clientDataJSON));
    document.querySelector(buttonSel).click();
}


function processAuthenticationChallenge() {
    var credentialsStr = getAuthenticationChallenge();
    var credentials = JSON.parse(credentialsStr);
    credentials.challenge = bufferDecode(credentials.challenge.value);
    credentials.allowCredentials.forEach(function (allowCredential, i) {
            allowCredential.id = bufferDecode(allowCredential.id);
        }
    );

    navigator.credentials.get({
        publicKey: credentials,
    }).then((assertion) => {
        assert(assertion);
    }).catch((e) => {
        console.log(e.toString());
    });
}

function assert(assertion) {

    var authenticatorData = new Uint8Array(assertion.response.authenticatorData);
    var clientDataJSON = new Uint8Array(assertion.response.clientDataJSON);
    var signature = new Uint8Array(assertion.response.signature);
    var userHandle = new Uint8Array(assertion.response.userHandle);

    var idToken1Sel = "IDToken1";
    var idToken2Sel = "IDToken2";
    var idToken3Sel = "IDToken3";
    var idToken4Sel = "IDToken4";
    var idToken5Sel = "IDToken5";
    var buttonSel = "[name='Login.Submit']";
    if(isXUI()) {
        idToken1Sel = "idToken1";
        idToken2Sel = "idToken2";
        idToken3Sel = "idToken3";
        idToken4Sel = "idToken4";
        idToken5Sel = "idToken5";
        buttonSel = "#loginButton_0";
    }

    document.getElementById(idToken1Sel).value = assertion.id;
    document.getElementById(idToken2Sel).value = bufferEncode( new Uint8Array(authenticatorData));
    document.getElementById(idToken3Sel).value = bufferEncode( new Uint8Array(clientDataJSON));
    document.getElementById(idToken4Sel).value = bufferEncode( new Uint8Array(signature));
    document.getElementById(idToken5Sel).value = bufferEncode(userHandle);

    document.querySelector(buttonSel).click();

}

function getAuthenticationChallenge() {
    var querySelector = ".TextOutputCallback_0";
    if(isXUI()) {
        querySelector = "#callback_5";
    }
    return document.querySelector(querySelector).innerText;
}