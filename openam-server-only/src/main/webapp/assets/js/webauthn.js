function WebAuthn() {
    function bufferDecode(value) {
        return Uint8Array.from(atob(value), c => c.charCodeAt(0));
    }

    function bufferEncode(bytes) {
        let binary = ''
        const len = bytes.byteLength;
        for (let i = 0; i < len; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return window.btoa(binary).replace(/\+/g, "-")
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
        if (isXUI()) {
            querySelector = "#callback_1";
        }
        return document.querySelector(querySelector).innerText;
    }

    function isXUI() {
        return !!window.requirejs;
    }

    function register(credential) {
        var idToken1Sel = "IDToken1";
        var buttonSel = "[name='Login.Submit']";
        if (isXUI()) {
            idToken1Sel = "idToken1";
            buttonSel = "#loginButton_0";
        }

        var credentials = {
            credentialId: credential.id,
            attestationObject: bufferEncode(new Uint8Array(credential.response.attestationObject)),
            clientDataJSON: bufferEncode(new Uint8Array(credential.response.clientDataJSON)),
        }

        document.getElementById(idToken1Sel).value = JSON.stringify(credentials);
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
        var buttonSel = "[name='Login.Submit']";
        if (isXUI()) {
            idToken1Sel = "idToken1";
            buttonSel = "#loginButton_0";
        }

        var credentials = {
            assertionId: assertion.id,
            authenticatorData: bufferEncode(new Uint8Array(authenticatorData)),
            clientDataJSON: bufferEncode(new Uint8Array(clientDataJSON)),
            signature: bufferEncode(new Uint8Array(signature)),
            userHandle: bufferEncode(userHandle),
        }

        document.getElementById(idToken1Sel).value = JSON.stringify(credentials);
        document.querySelector(buttonSel).click();

    }

    function getAuthenticationChallenge() {
        var querySelector = ".TextOutputCallback_0";
        if (isXUI()) {
            querySelector = "#callback_1";
        }
        return document.querySelector(querySelector).innerText;
    }
    return {
        processRegistrationChallenge: processRegistrationChallenge,
        processAuthenticationChallenge: processAuthenticationChallenge,
    }
}