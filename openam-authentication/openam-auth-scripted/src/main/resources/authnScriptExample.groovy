START_TIME = 9   // 9am
END_TIME   = 17  // 5pm

logger.message("Starting authentication groovy script")
logger.message("User: " + username)

// Log out current cookies in the request
if (logger.messageEnabled()) {
    cookies = requestData.getHeaders('Cookie')
    for (cookie in cookies) {
        logger.message('Cookie: ' + cookie)
    }
}

if (username != null) {
    // Make REST call to the users endpoint if username is set.
    httpClientRequest.setUri("http://openam.example.com:8080/openam/json/users/" + username)
    httpClientRequest.setMethod("GET")

    response = httpClient.perform(httpClientRequest)

    // Log out response from users REST call
    logger.message("User REST Call. Status: " + response.getStatusCode() + ", Body: " + response.getEntity())
}

now = new Date()
logger.message("Current time: " + now.getHours())
if (now.getHours() &lt; START_TIME || now.getHours() &gt; END_TIME) {
    logger.error("Login forbidden outside work hours!")
    authState = FAILED
} else {
    logger.message("Authentication allowed!")
    authState = SUCCESS
}