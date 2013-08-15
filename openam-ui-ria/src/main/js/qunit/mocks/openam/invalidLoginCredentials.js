/*global require, define*/
define(function () {

    return function (server) {
    
        server.respondWith(
            "GET",   
            "locales/en-US/translation.json",
            [
                200, 
                { 
                    "Date": "Tue, 06 Aug 2013 19:15:34 GMT",
                    "Last-Modified": "Sat, 27 Jul 2013 00:48:57 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "ETag": "&quot;807ce9-5162-4e2739fcd7040&quot;",
                    "Content-Type": "application/json",
                    "Connection": "Keep-Alive",
                    "Accept-Ranges": "bytes",
                    "Keep-Alive": "timeout=5, max=75",
                    "Content-Length": "20834"
                },
                "{\n    \"config\" : {\n        \"dates\" : {\n            \"monthNames\" : \"January, February, March, April, May, June, July, August, September, October, November, December\",\n            \"monthNamesShort\" : \"Jan., Feb., Mar., Apr., May., Jun., Jul., Aug., Sep., Oct., Nov., Dec.\",\n            \"dayNames\" : \"Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday\",\n            \"dayNamesShort\" : \"Sun. , Mon., Tue., Wed., Thu., Fri., Sat.\"\n        },\n        \"messages\" : {\n            \"UserMessages\" : {\n                \"invalidCredentials\" : \"Login/password combination is invalid.\",\n                \"serviceUnavailable\" : \"Service unavailable\",\n                \"changedPassword\" : \"Password has been changed\",\n                \"unknown\" : \"Unknown error. Please contact with administrator\",\n                \"profileUpdateFailed\" : \"Problem during profile update\",\n                \"profileUpdateSuccessful\" : \"Profile has been updated\",\n                \"userNameUpdated\" : \"Username has been modified succesfully.\",\n                \"afterRegistration\" : \"User has been registered successfully\",\n                \"loggedIn\" : \"You have been successfully logged in.\",\n                \"loginTimeout\" : \"Login processed timed out. Restarting...\",\n                \"errorFetchingData\" : \"Error fetching user data\",\n                \"loggedOut\" : \"You have been logged out.\",\n                \"siteIdentificationChanged\" : \"Site identification image has been changed\",\n                \"securityDataChanged\" : \"Security data has been changed\",\n                \"unauthorized\" : \"Unauthorized access or session timeout\",\n                \"userAlreadyExists\" : \"User already exists\",\n                \"internalError\" : \"Internal server error\",\n                \"forbiddenError\" : \"Forbidden request error.\",\n                \"notFoundError\" : \"Not found error.\",\n                \"badRequestError\" : \"Bad request error.\",\n                \"conflictError\" : \"Detected conflict in request.\",\n                \"errorDeletingNotification\" : \"Error deleting notification.\",\n                \"errorFetchingNotifications\" : \"Error Fetching notifications.\",\n                \"incorrectRevisionError\" : \"Cannot update the record because the version is not the latest.\"\n            },\n            \"AdminMessages\" : {\n                \"cannotDeleteYourself\" : \"You can't delete yourself\",\n                \"userDeleted\" : \"User has been deleted\",\n                \"userDeleteError\" : \"Error when deleting user\",\n                \"userApplicationsUpdate\" : \"Application settings have been changed.\",\n                \"completedTask\" : \"Task has been completed.\",\n                \"claimedTask\" : \"Task has been claimed.\",\n                \"unclaimedTask\" : \"Task has been unclaimed.\",\n                \"startedProcess\" : \"Process has been started\"\n            }\n        },\n        \"AppConfiguration\" : {\n            \"Navigation\" : {\n                \"links\" : {\n                    \"dashboard\" : \"Dashboard\",\n                    \"users\" : \"Users\",\n                    \"apps\" : \"Applications\",\n                    \"allApps\" : \"All applications\",\n                    \"addMore\" : \"Add more apps\",\n                    \"groups\" : \"Groups\",\n                    \"tasksMenu\" : \"Tasks\",\n                    \"allTasks\" : \"Tasks that are in my group's queue\",\n                    \"myTasks\" : \"My tasks\",\n                    \"startProcess\" : \"Start process\",\n                    \"oauthtokens\" : \"OAuth 2 Token Manager\"\n                }\n            }\n        }\n    },\n    \"templates\" : {\n        \"user\" : {\n            \"LoginDialog\" : {\n                \"refreshOnLogin\" : \"Should refresh on login\" \n            },\n            \"LoginTemplate\" : {\n                \"loginRemember\" : \"Remember my username\",\n                \"noAccountQuestion\" : \"Don't have an account?\",\n                \"registerAccount\" : \"Register your account\",\n                \"problemLoggingQuestion\" : \"Having trouble logging in?\",\n                \"resetPassword\" : \"Reset your password\",\n                \"siteIdentificationHeader\": \"Site Identification\",\n                \"siteIdentificationExplanation\": \"Please enter your login name.<br />Your site image and pass phrase will be displayed here.\"\n            },\n            \"404\" : {\n                \"pageNotFound\" : \"Page not found\",\n                \"requestedPageCouldNotBeFound\" : \"The requested page could not be found.\"\n            },\n            \"DefaultBaseTemplate\" : {\n                \"orPhone\" : \", or phone\"\n            },\n            \"ChangeSecurityDataDialogTemplate\" : {\n                \"securityDataChange\" : \"Security data change\",\n                \"enterNewPasswordToChange\" : \"Please enter new password in the fields below to change your password.\",\n                \"selectQuestionAndAnserIfYouWant\" : \"Please select question and enter your answer if you want to set up a one.\",\n                \"securityQuestionAndAnserExplanation\" : \"The Security Question selected and corresponding Answer provided by you will be used to help identify you in the event that you forget your password.\"\n            },\n            \"ChangeSiteIdentificationDialogTemplate\" : {\n                \"siteImageAndPhaseExplanation\" : \"The Site Image and Phrase selected will be shown to you upon login to confirm that you are logging in to the genuine site\"\n            },\n            \"EnterOldPasswordDialog\" : {\n                \"passwordAndSecurityQuestionChange\" : \"Password and security question change\",\n                \"enterYourOldPassword\" : \"Please enter your old password\"\n            },\n            \"ForgottenPasswordTemplate\" : {\n                \"forgottenPasswordQuestion\" : \"Forgotten password?\",\n                \"enterLogin\" : \"Please enter your login in the field below to continue.\"\n            },\n            \"TermsOfUseTemplate\" : {\n                \"termsOfUse\" : \"Terms of use\",\n                \"termsOfUseContent\" : \"<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris idvulputate urna. Quisque tristique dictum sodales. Nullam tempus eleifend erat,vel facilisis nibh euismod non. Phasellus vitae elit velit, in sollicitudindiam. Maecenas ornare nunc non ipsum ornare vehicula id sed dolor. Duispellentesque congue sapien, dictum adipiscing est posuere sit amet. Suspendissein lectus lorem. Suspendisse vitae metus sapien, sit amet luctus libero.Integer nec nunc sed massa pellentesque pulvinar. Integer vulputate nunc atnunc lacinia eu luctus neque elementum. Aenean ultricies arcu sed enimultricies blandit placerat ut massa. Donec at dolor sit amet magna vulputateelementum sit amet in sapien. Donec sit amet erat neque.</p><p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris idvulputate urna. Quisque tristique dictum sodales. Nullam tempus eleifend erat,vel facilisis nibh euismod non. Phasellus vitae elit velit, in sollicitudindiam. Maecenas ornare nunc non ipsum ornare vehicula id sed dolor. Duispellentesque congue sapien, dictum adipiscing est posuere sit amet. Suspendissein lectus lorem. Suspendisse vitae metus sapien, sit amet luctus libero.Integer nec nunc sed massa pellentesque pulvinar. Integer vulputate nunc atnunc lacinia eu luctus neque elementum. Aenean ultricies arcu sed enimultricies blandit placerat ut massa. Donec at dolor sit amet magna vulputateelementum sit amet in sapien. Donec sit amet erat neque.</p><p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris idvulputate urna. Quisque tristique dictum sodales. Nullam tempus eleifend erat,vel facilisis nibh euismod non. Phasellus vitae elit velit, in sollicitudindiam. Maecenas ornare nunc non ipsum ornare vehicula id sed dolor. Duispellentesque congue sapien, dictum adipiscing est posuere sit amet. Suspendissein lectus lorem. Suspendisse vitae metus sapien, sit amet luctus libero.Integer nec nunc sed massa pellentesque pulvinar. Integer vulputate nunc atnunc lacinia eu luctus neque elementum. Aenean ultricies arcu sed enimultricies blandit placerat ut massa. Donec at dolor sit amet magna vulputateelementum sit amet in sapien. Donec sit amet erat neque.</p><p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris idvulputate urna. Quisque tristique dictum sodales. Nullam tempus eleifend erat,vel facilisis nibh euismod non. Phasellus vitae elit velit, in sollicitudindiam. Maecenas ornare nunc non ipsum ornare vehicula id sed dolor. Duispellentesque congue sapien, dictum adipiscing est posuere sit amet. Suspendissein lectus lorem. Suspendisse vitae metus sapien, sit amet luctus libero.Integer nec nunc sed massa pellentesque pulvinar. Integer vulputate nunc atnunc lacinia eu luctus neque elementum. Aenean ultricies arcu sed enimultricies blandit placerat ut massa. Donec at dolor sit amet magna vulputateelementum sit amet in sapien. Donec sit amet erat neque.</p>\"\n            },\n            \"UserProfileTemplate\" : {\n                \"changeSecurityData\" : \"Change Security Data\",\n                \"changeSiteIdentification\" : \"Change Site Identification\"\n            },\n            \"UserRegistrationTemplate\" : {\n                \"registerYourAccount\" : \"Register your account\",\n                \"logIn\" : \"Log in\",\n                \"haveAccoutQuestion\" : \"Already have an account?\",\n                \"agreeToTerms\" : \"I agree to the %sterms of use%s\"\n            }\n        },\n        \"admin\" : {\n            \"tasks\" : {\n                \"ShowUserProfile\" : {\n                    \"requestInitiator\" : \"Request initiated by\"\n                },\n                \"TasksDashboardTemplate\" : {\n                    \"tasksFromGroupsQueue\" : \"Tasks that are in my group's queue\"\n                },\n                \"ProcessUserTaskTableTemplate\" : {\n                    \"requestCount\" : \"__count__  request\",\n                    \"requestCount_plural\" : \"__count__  requests\"\n                }\n            },\n            \"ChangeUserPasswordDialogTemplate\" : {\n                \"securityDataChange\" : \"Security data change\",\n                \"enterNewPasswordToChange\" : \"Please enter new password in the fields below to change user's password.\"\n            },\n            \"UsersTemplate\" : {\n                \"remainingUsers\" : \"remaining users\"\n            }\n        },\n        \"oauth\": {\n            \"clientID\" : \"Client ID\",\n            \"tokenList\" : \"Token List\",\n            \"remainingTokens\" : \"remaining tokens\",\n            \"tokenID\" : \"Token ID\",\n            \"expireDate\" : \"Expire Date\",\n            \"scope\" : \"Scope\",\n            \"tokenType\" : \"Token Type\",\n            \"parent\" : \"Parent\",\n            \"username\" : \"Username\",\n            \"redirecturi\" : \"Redirect URI\",\n            \"refreshToken\" : \"Refresh Token\",\n            \"issued\" : \"Issued\",\n            \"realm\" : \"Realm\",\n            \"tokenInfo\": \"TokenInfo\"\n        }\n    },\n    \"common\" : {\n         \"form\" : {\n            \"true\" : \"True\",\n            \"false\" : \"False\",\n            \"update\" : \"Update\",\n            \"save\" : \"Save\",\n            \"create\" : \"Create\",\n            \"cancel\" : \"Cancel\",\n            \"reset\" : \"Reset\",\n            \"close\" : \"Close\",\n            \"logout\" : \"Log out\",\n            \"register\" : \"Register\",\n            \"home\" : \"Home\",\n            \"start\" : \"Start\",\n            \"continue\" : \"Continue\",\n            \"back\" : \"Back\",\n            \"delete\" : \"Delete\",\n            \"details\" : \"Details\",\n            \"pleaseSelect\" : \"Please Select\",\n            \"addUser\" : \"Add user\",\n            \"decision\" : \"Decision\",\n            \"accept\" : \"Accept\",\n            \"reject\" : \"Reject\",\n            \"createdAt\" : \"Created at\",\n            \"search\" : \"Search\",\n            \"submit\" : \"Submit\",\n            \"actions\" : \"Actions\",\n            \"complete\" : \"Complete\",\n            \"copyright\" : \"Copyright (c) 2010-12 ForgeRock, all rights reserved.\",\n            \"sessionExpired\" : \"Session Expired\",\n            \"deleteSelected\" : \"Delete Selected\",\n            \"validation\" : {\n                \"VALID_PHONE_FORMAT\": \"Contains invalid characters\",\n                \"VALID_NAME_FORMAT\": \"Contains invalid characters\",\n                \"VALID_EMAIL_ADDRESS_FORMAT\": \"Not a valid email address\",\n                \"AT_LEAST_X_CAPITAL_LETTERS\" : \"At least __numCaps__ capital letters\",\n                \"AT_LEAST_X_NUMBERS\" : \"At least __numNums__ numbers\",\n                \"CANNOT_CONTAIN_OTHERS\" : \"Cannot contain values from: __disallowedFields__\",\n                \"MIN_LENGTH\" : \"At least __minLength__ characters\",\n                \"REQUIRED\" : \"Cannot be blank\",\n                \"UNIQUE\" : \"Already exists\",\n                \"REAUTH_REQUIRED\" : \"\",\n                \"formContainsErrors\" : \"Form contains validation errors\",\n                \"atLeastOneCapitalLetter\" : \"At least one capital letter\",\n                \"atLeastOneNumber\" : \"At least one number\",\n                \"atLeast8Characters\" : \"At least 8 characters\",\n                \"cannotMatchLogin\" : \"Cannot match login\",\n                \"confirmationMatchesPassword\" : \"Confirmation matches password\",\n                \"usernameExists\" : \"Username already exists\",\n                \"emailAddressAlreadyExists\" : \"Email address already exists\",\n                \"onlyAlphabeticCharacters\" : \"Only alphabetic characters\",\n                \"onlyNumbersAndSpecialCharacters\" : \"Only numbers and special characters\",\n                \"cannotMatchOldPassword\" : \"Cannot match old password\",\n                \"minimum4Characters\" : \"Minimum 4 characters\",\n                \"acceptanceRequiredForRegistration\" : \"Acceptance required for registration\",\n                \"incorrectPassword\" : \"Incorrect password\",\n                \"incorrectSecurityAnswer\" : \"Incorrect answer\",\n                \"required\" : \"Required\",\n                \"emailNotValid\" : \"Not a valid email address.\",\n                \"emailExists\" : \"Email address already exists.\",\n                \"shouldBeLong\" : \"Should be long value\",\n                \"wrongDateFormat\" : \"Wrong format\"\n            }\n         },\n         \"user\" : {\n            \"user\" : \"User\",\n            \"login\" : \"Login\",\n            \"profile\" : \"Profile\",\n            \"myProfile\" : \"My Profile\",\n            \"userProfile\" : \"User profile\",\n            \"username\" : \"Username\",\n            \"emailAddress\" : \"Email address\",\n            \"givenName\" : \"First Name\",\n            \"familyName\" : \"Last Name\",\n            \"changePassword\" : \"Change password\",\n            \"accountStatus\" : \"Account status\",\n            \"active\" : \"Active\",\n            \"inactive\" : \"Inactive\",\n            \"address\" : \"Address\",\n            \"address1\" : \"Address 1\",\n            \"address2\" : \"Address 2\",\n            \"country\" : \"Country\",\n            \"city\" : \"City\",\n            \"role\" : \"Admin Role\",\n            \"stateProvince\" : \"State/Province\",\n            \"phoneNumber\" : \"Mobile Phone\",\n            \"postalCode\" : \"Postal Code\",\n            \"lastPasswordSet\" : \"Last password set\",\n            \"siteImage\" : \"Site Image\",\n            \"sitePhrase\" : \"Site Phrase\",\n            \"securityQuestion\" : \"Security question\",\n            \"securityAnswer\" : \"Security answer\",\n            \"adaptiveAuthMethod\" : \"Adaptive auth method\",\n            \"SMS\" : \"SMS\",\n            \"OAuth\" : \"OAuth\",\n            \"createNewAccount\" : \"Create new account\",\n            \"password\" : \"Password\",\n            \"confirmPassword\" : \"Confirm Password\",\n            \"newPassword\" : \"New password\",\n            \"oldPassword\" : \"Old password\",\n            \"confirmNewPassword\" : \"Confirm new password\",\n            \"usersList\" : \"Users list\",\n            \"addUsers\" : \"Add users\",\n            \"name\" : \"Name\",\n            \"status\" : \"Status\",\n            \"email\" : \"Email\",\n            \"system\" : \"System\",\n            \"changePhoto\" : \"Change photo\",\n            \"SKYPE\" : \"SKYPE\",\n            \"googlePlus\" : \"Google+\"\n         }, \n         \"task\" : {\n            \"unassigned\" : \"Unassigned\",\n            \"assignToMe\" : \"Assign to me\",\n            \"claim\" : \"Claim\",\n            \"unclaim\" : \"Unclaim\",\n            \"approve\" : \"Approve\",\n            \"deny\" : \"Deny\",\n            \"requeue\" : \"Requeue\",\n            \"taskName\" : \"Task Name\",\n            \"processName\" : \"Process Name\",\n            \"startProcess\" : \"Start Process\",\n            \"assignee\" : \"Assignee\",\n            \"taskDetails\" : \"Task Details\",\n            \"myTasks\" : \"My tasks\",\n            \"tasksList\" : \"Tasks list\",\n            \"processes\" : \"Processes\"\n         },\n         \"application\" : {\n            \"applicationName\" : \"Application Name\",\n            \"requestedBy\" : \"Requested by\",\n            \"yourFrequentlyUsedApplications\" : \"Your frequently used apps\",\n            \"applicationsYouHaveAdded\" : \"Apps you've added\",\n            \"defaultApplications\" : \"Default apps\",\n            \"addMoreApplications\" : \"Add more apps\",\n            \"allAvailableApps\" : \"All available apps\",\n            \"dropApplicationsHere\" : \"drop apps here\"\n         },\n         \"notification\" : {\n            \"notifications\" : \"Notifications\",\n            \"deleteThisMessage\" : \"Delete this message\",\n            \"type\" : \"Notification type\",\n            \"message\" : \"Message\",\n            \"types\" : {\n                \"info\" : \"Info\",\n                \"warning\" : \"Warning\",\n                \"error\" : \"Error\"\n            }\n         }\n    },\n    \"openam\" : {\n        \"apps\" : {\n            \"header\" : \"My Applications\",\n            \"noneFound\": \"You have no applications assigned to you\"\n        },\n        \"authentication\": {\n            \"input\": {\n                \"name\": \"Username\",\n                \"password\": \"Password\"\n            },\n            \"unavailable\": \"Unable to login to OpenAM\"\n            \n        }\n    },    \n    \"openidm\" : {\n        \"ui\" : {\n            \"common\" : {\n                \"components\" : {\n                    \"LineTableView\" : {\n                        \"seeMoreItems\" : \"see more\",\n                        \"noItems\" : \"No items\"\n                    }\n                }\n            },\n            \"admin\" : {\n                \"tasks\" : {\n                    \"TasksMenuView\" : {\n                        \"noTasksAssigned\" : \"You do not have any tasks assigned to you right now.\",\n                        \"noTasksInGroupQueue\" : \"You do not have any tasks in your's group's queue now.\",\n                        \"denyDefaultReason\" : \"Enter a reason for denying this request\",\n                        \"acceptanceForm\" : {\n                            \"for\" : \"For\",\n                            \"application\" : \"Application\",\n                            \"requested\" : \"Requested\",\n                            \"actions\" : \"Actions\"                            \n                        },\n                        \"headers\" : {\n                            \"initiator\" : \"Initiator\",\n                            \"key\" : \"Key\",\n                            \"requested\" : \"Requested\",\n                            \"inQueue\" : \"In queue\",\n                            \"actions\" : \"Actions\"    \n                        }   \n                    },\n                    \"TasksWithMenuView\" : {\n                        \"chooseTask\" : \"Choose a task\"\n                    },\n                    \"StartProcessDashboardView\" : {\n                        \"chooseProcess\" : \"Please choose a process to start\",\n                        \"noProcesses\": \"No processes\",\n                        \"noDataRequired\": \"No data required\"\n                    }\n                },\n                \"users\" : {\n                    \"UsersView\" : {\n                        \"display\" : \"Display\",\n                        \"perPage\" : \"per page\"\n                    },\n                    \"AdminUserProfileView\" : {\n                        \"profileOwnership\" : \"%s' profile\",\n                        \"profileWillBeDeleted\" : \"%s account will be deleted.\"\n                    },\n                    \"ChangeUserPasswordDialog\" : {\n                        \"securityDataChangeForWhom\" : \"Security data change for %s\" \n                    }\n                }\n            },\n            \"apps\" : {\n                \"dashboard\" : {\n                    \"NotificationsView\" : {\n                        \"noNotifications\" : \"You have no notifications right now.\",\n                        \"seeMoreNotifications\" : \"see more notifications\"\n                    }\n                }, \n                \"BaseApplicationsView\" : {\n                    \"noApplicationsHere\" : \"Your have no apps here\",\n                    \"noDefaultApplications\" : \"Your have no default apps\",\n                    \"noFrequentlyUsedApplications\" : \"Your have no frequently used apps\"\n                },\n                \"UsersApplicationsView\" : {\n                    \"noApplications\" : \"You have no applications\"\n                },\n                \"FrequentlyUsedApplicationsView\" : {\n                    \"clickHereToAdd\" : \"Click %shere%s to add.\"\n                }\n            }\n        }\n    }\n}"
            ]
        );
    
        server.respondWith(
            "GET",   
            "locales/en/translation.json",
            [
                404, 
                { 
                    "Date": "Tue, 06 Aug 2013 19:15:34 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=85",
                    "Content-Length": "238",
                    "Content-Type": "text/html; charset=iso-8859-1"
                },
                "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n<html><head>\n<title>404 Not Found</title>\n</head><body>\n<h1>Not Found</h1>\n<p>The requested URL /openam-debug/locales/en/translation.json was not found on this server.</p>\n</body></html>\n"
            ]
        );
    
        server.respondWith(
            "GET",   
            "locales/dev/translation.json",
            [
                404, 
                { 
                    "Date": "Tue, 06 Aug 2013 19:15:34 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=74",
                    "Content-Length": "239",
                    "Content-Type": "text/html; charset=iso-8859-1"
                },
                "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n<html><head>\n<title>404 Not Found</title>\n</head><body>\n<h1>Not Found</h1>\n<p>The requested URL /openam-debug/locales/dev/translation.json was not found on this server.</p>\n</body></html>\n"
            ]
        );
    
        server.respondWith(
            "POST",   
            "/openam/json/users/?_action=idFromSession",
            [
                401, 
                { 
                    "Date": "Tue, 06 Aug 2013 19:15:34 GMT",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=73",
                    "Content-Length": "69",
                    "Content-Type": "application/json;charset=ISO-8859-1"
                },
                "{ \"error\": 401, \"reason\": \"Unauthorized\", \"detail\": \"Access denied\" }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/identity/json/getcookienamefortoken",
            [
                200, 
                { 
                    "Pragma": "no-cache",
                    "Date": "Tue, 06 Aug 2013 19:15:34 GMT",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Type": "application/json;charset=UTF-8",
                    "Cache-Control": "no-store, no-cache, must-revalidate, max-age=0",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=72",
                    "Content-Length": "32"
                },
                "{\"string\":\"iPlanetDirectoryPro\"}"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openam/json/auth/1/authenticate?locale=en-US",
            [
                200, 
                { 
                    "Date": "Tue, 06 Aug 2013 19:15:34 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=71",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json"
                },
                "{ \"authId\": \"eyAiYWxnIjogIkhTMjU2IiwgInR5cCI6ICJKV1QiIH0=.eyAib3RrIjogIjcyb3FuaGNjNHFhZjV0aDBzcWczM2J1dTkyIiwgInJlYWxtIjogImRjPW9wZW5hbSxkYz1mb3JnZXJvY2ssZGM9b3JnIiwgInNlc3Npb25JZCI6ICJBUUlDNXdNMkxZNFNmY3hPX3I2VWpMejc1UE1WV2xfSzA4dDNyblF1UTFMNktkWS4qQUFKVFNRQUNNREVBQWxOTEFCTTNNemMxTWpFeE1qa3lOVFF4TWpZME1USXkqIiB9.P0VTnEHGpRyVKIMNn6ODmqkpYpsQ/VjodTtm7rPYxgo=\", \"template\": \"\", \"stage\": \"DataStore1\", \"callbacks\": [ { \"type\": \"NameCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" User Name: \" } ], \"input\": [ { \"name\": \"IDToken1\", \"value\": \"\" } ] }, { \"type\": \"PasswordCallback\", \"output\": [ { \"name\": \"prompt\", \"value\": \" Password: \" } ], \"input\": [ { \"name\": \"IDToken2\", \"value\": \"\" } ] } ] }"
            ]
        );
    
        server.respondWith(
            "GET",   
            "templates/openam/authn/DataStore1.html",
            [
                200, 
                { 
                    "Date": "Tue, 06 Aug 2013 19:15:34 GMT",
                    "Last-Modified": "Sat, 27 Jul 2013 00:48:58 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "ETag": "&quot;807cef-3a6-4e2739fdcb280&quot;",
                    "Content-Type": "text/html",
                    "Connection": "Keep-Alive",
                    "Accept-Ranges": "bytes",
                    "Keep-Alive": "timeout=5, max=70",
                    "Content-Length": "934"
                },
                "<style>\n#header{\n    width: {{theme.settings.lessVars.login-container-width}};\n}\n</style>\n<div class=\"container-shadow\" id=\"login-container\">\n    <div class=\"column-layout\" style=\"padding-top: 30px;\">\n        <div>\n            <form action=\"\" method=\"post\" class=\"form\">\n                <fieldset>\n    \n                    {{#each reqs.callbacks}}\n                    \n                    {{#if isSubmit}}\n                    <div class=\"field field-checkbox\">\n                        <label><input type=\"checkbox\" name=\"loginRemember\" />{{t \"templates.user.LoginTemplate.loginRemember\"}}</label>\n                    </div>\n                    {{/if}}\n                    \n                    <div class=\"field {{#if isSubmit}}field-submit{{/if}}\">\n                        {{callbackRender}}\n                    </div>\n                    {{/each}}\n    \n                </fieldset>\n            </form>\n        </div>\n    </div>\n</div>"
            ]
        );
    
        server.respondWith(
                "GET",   
                "themeConfig.json",
                [
                    200, 
                    { 
                        "Date": "Tue, 13 Aug 2013 18:07:01 GMT",
                        "Last-Modified": "Tue, 13 Aug 2013 18:04:00 GMT",
                        "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                        "ETag": "&quot;861826-c1d-4e3d810bb6400&quot;",
                        "Content-Type": "application/json",
                        "Connection": "Keep-Alive",
                        "Accept-Ranges": "bytes",
                        "Keep-Alive": "timeout=5, max=74",
                        "Content-Length": "3101"
                    },
                    "{\n        \"themes\": [\n            {\n                \"name\": \"default\",\n                \"path\": \"\",\n                \"realms\": [\".*\"],\n                \"regex\": true,\n                \"icon\": \"favicon.ico\",\n                \"settings\": {\n                    \"logo\": {\n                        \"src\": \"images/logo.png\",\n                        \"title\": \"ForgeRock\",\n                        \"alt\": \"ForgeRock\",\n                        \"height\": \"60\",\n                        \"width\": \"226\"\n                    },\n                    \"lessVars\": {\n                        \"background-color\": \"#f2f2f2\",\n                        \"background-image\": \"url('')\",\n                        \"background-repeat\": \"no-repeat\",\n                        \"background-position\": \"left top\",\n                        \"footer-background-color\": \"#f2f2f2\",\n                        \"background-font-color\": \"#5a646d\",\n                        \"column-padding\": \"160px\",\n                        \n                        \"login-container-width\": \"480px\",\n                        \"login-container-label-align\": \"right\",\n            \n                        \"message-background-color\": \"#F9F9F9\",\n                        \"content-background\": \"#ffffff\",\n                        \"font-color\": \"#5a646d\",\n                        \"font-size\": \"14px\",\n                        \"font-family\": \"Arial, Helvetica, sans-serif\",\n                        \"site-width\": \"960px\",\n                        \"line-height\": \"18px\",\n            \n                        \"color-active\": \"#f36e09\",\n                        \"color-inactive\": \"#626d75\",\n            \n                        \"active-menu-color\": \"#fc8a28\",\n                        \"inactive-menu-color\": \"#5d6871\",\n                        \"button-hover-lightness\": \"4%\",   \n            \n                        \"href-color\": \"#0e99b8\",\n                        \"color-error\": \"red\",\n                        \"color-warning\": \"yellow\",\n                        \"color-success\": \"green\",\n                        \"color-info\": \"blue\",\n                        \"color-inactive\": \"gray\",\n            \n                        \"input-border-basic\": \"#DBDBDB\",\n                        \"header-border-color\": \"#5D5D5D\",\n            \n            \n                        \"footer-height\": \"126px\"\n                    },\n                    \"footer\": {\n                        \"mailto\": \"info@forgerock.com\",\n                        \"phone\": \"+47 21520108\"\n                    }\n                }\n            },\n            {\n                \"name\": \"test\",\n                \"path\": \"\",\n                \"realms\": [\"test.*\"],\n                \"regex\": true,\n                \"settings\": {\n                    \"logo\": {\n                        \"src\": \"images/new_logo.png\",\n                        \"title\": \"ForgeRock\",\n                        \"alt\": \"ForgeRock_test\",\n                        \"height\": \"80\",\n                        \"width\": \"\"\n                    },\n                    \"footer\": {\n                        \"mailto\": \"test@test.com\"\n                    }\n                }\n            }\n        ]\n}"
                ]
            );
        
        server.respondWith(
            "GET",   
            "templates/user/LoginBaseTemplate.html",
            [
                200, 
                { 
                    "Date": "Tue, 06 Aug 2013 19:15:42 GMT",
                    "Last-Modified": "Sat, 27 Jul 2013 00:46:59 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "ETag": "&quot;82c404-1a2-4e27398c4e6c0&quot;",
                    "Content-Type": "text/html",
                    "Connection": "Keep-Alive",
                    "Accept-Ranges": "bytes",
                    "Keep-Alive": "timeout=5, max=92",
                    "Content-Length": "418"
                },
                "<div id=\"header\" class=\"container\" style=\"width:{{theme.settings.lessVars.login-container-width}}\">\n    <div id=\"logo\" class=\"float-left\">\n           <a href=\"#\" title=\"{{theme.settings.logo.title}}\"><img src=\"{{theme.settings.logo.src}}\" width=\"{{theme.settings.logo.width}}\" height=\"{{theme.settings.logo.height}}\" alt=\"{{theme.settings.logo.alt}}\" /> </a>\n    </div>\n</div>\n\n<div id=\"content\" class=\"content\"></div>"
            ]
        );
    
        server.respondWith(
            "GET",   
            "templates/common/NavigationTemplate.html",
            [
                200, 
                { 
                    "Date": "Tue, 06 Aug 2013 19:15:42 GMT",
                    "Last-Modified": "Thu, 11 Jul 2013 17:53:56 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "ETag": "&quot;153d92-18-4e14013f7b500&quot;",
                    "Content-Type": "text/html",
                    "Connection": "Keep-Alive",
                    "Accept-Ranges": "bytes",
                    "Keep-Alive": "timeout=5, max=90",
                    "Content-Length": "24"
                },
                "<ul class=\"menu\">\n</ul>\n"
            ]
        );
    
        server.respondWith(
            "GET",   
            "templates/common/FooterTemplate.html",
            [
                200, 
                { 
                    "Date": "Tue, 06 Aug 2013 19:15:42 GMT",
                    "Last-Modified": "Sat, 27 Jul 2013 00:46:59 GMT",
                    "Server": "Apache/2.2.22 (Unix) DAV/2 PHP/5.3.15 with Suhosin-Patch mod_ssl/2.2.22 OpenSSL/0.9.8x",
                    "ETag": "&quot;82c3ef-137-4e27398c4e6c0&quot;",
                    "Content-Type": "text/html",
                    "Connection": "Keep-Alive",
                    "Accept-Ranges": "bytes",
                    "Keep-Alive": "timeout=5, max=89",
                    "Content-Length": "311"
                },
                "<div class=\"container center\">\n    <p class=\"center\">\n        <a href=\"mailto: {{theme.settings.footer.mailto}}\">{{theme.settings.footer.mailto}}</a>\n        {{t 'templates.user.DefaultBaseTemplate.orPhone'}} {{theme.settings.footer.phone}}.\n        <br />\n        {{t \"common.form.copyright\"}}\n    </p>\n</div>\n"
            ]
        );
        
        server.respondWith(
                "POST",   
                "/openam/json/auth/1/authenticate",
                [
                    401, 
                    { 
                        "Date": "Thu, 08 Aug 2013 18:12:50 GMT",
                        "Server": "Apache-Coyote/1.1",
                        "Connection": "Keep-Alive",
                        "Keep-Alive": "timeout=5, max=100",
                        "Transfer-Encoding": "chunked",
                        "Content-Type": "application/json"
                    },
                    "{ \"errorMessage\": \"Invalid Password!!\" }"
                ]
            );
    
        server.respondWith(
            "GET",   
            "/openam/json/serverinfo/cookieDomains",
            [
                200, 
                { 
                    "Date": "Mon, 12 Aug 2013 22:30:12 GMT",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;0&quot;",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json;charset=UTF-8",
                    "Cache-Control": "no-cache",
                    "Connection": "Keep-Alive",
                    "Keep-Alive": "timeout=5, max=75"
                },
                "{\"domains\":[\"test.forgerock.com\",\"foo.bar.com\",\".forgerock.com\"]}"
            ]
        );

    };

});
