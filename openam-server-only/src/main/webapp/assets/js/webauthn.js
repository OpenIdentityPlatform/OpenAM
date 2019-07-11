function bufferDecode(value) {
    return Uint8Array.from(atob(value), c => c.charCodeAt(0));
}

function bufferEncode(value) {
    return Base64.fromByteArray(value)
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=/g, "");
}

function processRegistrationChallenge() {
    var challengeStr = document.querySelector(".TextOutputCallback_0").innerText;
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

function register(credential) {
    document.getElementById("IDToken1").value = credential.id;
    document.getElementById('IDToken2').value = credential.type;
    document.getElementById('IDToken3').value = bufferEncode( new Uint8Array(credential.response.attestationObject));
    document.getElementById('IDToken4').value = bufferEncode( new Uint8Array(credential.response.clientDataJSON));

    document.querySelector("form").submit();
}

function processAuthenticationChallenge() {
    var credentialsStr = document.querySelector(".TextOutputCallback_0").innerText;
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
    var rawId = new Uint8Array(assertion.rawId);

    document.getElementById("IDToken1").value = assertion.id;
    document.getElementById('IDToken2').value = bufferEncode( new Uint8Array(authenticatorData));
    document.getElementById('IDToken3').value = bufferEncode( new Uint8Array(clientDataJSON));
    document.getElementById('IDToken4').value = bufferEncode( new Uint8Array(signature));
    document.getElementById('IDToken5').value = bufferEncode(userHandle);

    document.querySelector("form").submit();

}