# OpenAM MCP Server

OpenAM MCP Server is a lightweight management service for OpenAM user accounts.
It allows administrators to create, update, delete, and reset passwords for users.

## Prerequisites
* JDK 17+
* [OpenAM](http://github.com/OpenIdentityPlatform/OpenAM) installed

## Quick Start
Set the `OPENAM_URL` environment variable, i.e., http://openam.example.org:8080/openam
Set the `OPENAM_ADMIN_USERNAME` (i.e., `amadmin`) and the `OPENAM_ADMIN_PASSWORD` environment variables (i.e., `passw0rd`).

```bash
export OPENAM_URL=http://openam.example.org:8080/openam
export OPENAM_ADMIN_USERNAME=amadmin
export OPENAM_ADMIN_PASSWORD=passw0rd
```

Clone the source code:

Run from the source code:

```bash
mvn spring-boot:run
```
Build and run:

```bash
cd openam-mcp-server
mvn package -DskipTests=true && java -jar ./target/openam-mcp-server-*.jar
```

## Advanced Authentication

> [!IMPORTANT]  
> Using administrative credentials directly in the MCP server may be insecure, so this server supports OpenAM's OAuth 2.0 protocol.
protocol.

This approach requires additional OpenAM configuration:

### OpenAM OAuth2.0 Service Configuration

Go to the OpenAM admin console, select the root realm, then select **Configure OAuth Provider** → **Configure OAuth2.0**.
Leave the settings unchanged and click the Create button. 

Set the following settings for the OAuth2.0 Provider:

| Setting                                                          | Value   |
|------------------------------------------------------------------|---------|
| Use Stateless Access & Refresh Tokens                            | enabled |
| User Profile Attribute(s) the Resource Owner is Authenticated On | uid     |
| Supported Scopes                                                 | profile |
| OAuth2 Token Signing Algorithm                                   | RS256   |
| Allow Open Dynamic Client Registration                           | enabled |

See more in the OpenAM OAuth2.0 documentation: https://doc.openidentityplatform.org/openam/admin-guide/chap-oauth2

### Authentication Chain Configuration
Create the OpenAM OAuth 2.0 authentication chain, so that the MCP server can exchange an access token for an SSO token to manage identities.
In the administrator console, select the root realm. Then in the left menu select **Authentication** → **Modules** and create a new module with name  `oidc` and type `OpenID Connect id_token bearer`.

Set the following settings for the `oidc` authentication module:

| Setting                                            | Value                                                                                                                                    |
|----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| OpenID Connect validation configuration value      | OpenAM's .well-known/openid-configuration endpoint, i.e.,  http://openam.example.org:8080/openam/oauth2/.well-known/openid-configuration |
| Name of OpenID Connect ID Token Issuer             | http://openam.example.org:8080/openam/oauth2                                                                                             |
| Mapping of jwt attributes to local LDAP attributes | sub=uid                                                                                                                                  |
| Audience name                                      | Your MCP client's client ID, for example openam-mcp-server                                                                               |

Create an authentication chain with the `oidc` module:

In the administrator console select the root realm then in the left menu select **Authentication** → **Chains** and create a new chain with name `oidc` and the following settings:

| Module | Criteria   |
|--------|------------|
| oidc   | REQUISITE  |

Set the MCP server environment variable `OPENAM_USE_OAUTH` to `true`


## Available MCP Server Tools

### Authentication Service Tools

```java
@Tool(name = "get_auth_modules", description = "Returns OpenAM authentication modules list")
public List<AuthModule> getAuthModules(@ToolParam(required = false, description = "If not set, uses root realm") String realm)

@Tool(name = "get_auth_chains", description = "Returns OpenAM authentication chains with modules")
public List<AuthChain> getOpenAMAuthChains(@ToolParam(required = false, description = "If not set, uses root realm") String realm)

@Tool(name = "get_available_modules", description = "Returns all avialable authenticaion modules")
public List<CoreAuthModule> getAvailableModuleList() 


```

### Realm Tools

```java
@Tool(name = "get_realms", description = "Returns OpenAM realm list")
public List<Realm> getRealms()
```

### User Tools

```java
@Tool(name = "get_users", description = "Returns OpenAM user list from the default (root) realm")
public List<User> getUsers(@ToolParam(required = false, description = "If not set, uses root realm") String realm,
                           @ToolParam(required = false, description = "Username filter") String filter)

@Tool(name = "set_user_attribute", description = "Sets the attribute value for a user")
public User setUserAttribute(@ToolParam(required = false, description = "If not set, uses root realm") String realm,
                             @ToolParam(description = "username") String username,
                             @ToolParam(description = "user attribute name") String attribute,
                             @ToolParam(description = "user attribute value") String value)

@Tool(name = "set_user_password", description = "Sets the password for a user")
public User setUserPassword(@ToolParam(required = false, description = "If not set, uses root realm") String realm,
                            @ToolParam(description = "username") String username,
                            @ToolParam(description = "user password") String password)

@Tool(name = "create_user", description = "Creates a new user")
public User createUser(@ToolParam(required = false, description = "If not set, uses root realm") String realm,
                        @ToolParam(description = "Username (login)") String userName,
                        @ToolParam(description = "Password (min length 8)") String password,
                        @ToolParam(required = false, description = "User family name") String familyName,
                        @ToolParam(required = false, description = "User given name") String givenName,
                        @ToolParam(required = false, description = "Name") String name,
                        @ToolParam(required = false, description = "Email") String mail,
                        @ToolParam(required = false, description = "Phone number") String phone)

@Tool(name = "delete_user", description = "Deletes a user")
public Map<String, String> deleteUser(@ToolParam(required = false, description = "If not set, uses root realm") String realm,
                                      @ToolParam(description = "Username (login)") String username)

```

In JSON-RPC format:
```json
{
  "tools": [
    {
      "name": "set_user_password",
      "description": "Sets the password for a user",
      "inputSchema": {
        "type": "object",
        "properties": {
          "arg0": {
            "type": "string",
            "description": "If not set, uses root realm"
          },
          "arg1": {
            "type": "string",
            "description": "username"
          },
          "arg2": {
            "type": "string",
            "description": "user password"
          }
        },
        "required": [
          "arg1",
          "arg2"
        ],
        "additionalProperties": false
      }
    },
    {
      "name": "set_user_attribute",
      "description": "Sets the attribute value for a user",
      "inputSchema": {
        "type": "object",
        "properties": {
          "arg0": {
            "type": "string",
            "description": "If not set, uses root realm"
          },
          "arg1": {
            "type": "string",
            "description": "username"
          },
          "arg2": {
            "type": "string",
            "description": "user attribute name"
          },
          "arg3": {
            "type": "string",
            "description": "user attribute value"
          }
        },
        "required": [
          "arg1",
          "arg2",
          "arg3"
        ],
        "additionalProperties": false
      }
    },
    {
      "name": "get_realms",
      "description": "Returns OpenAM realm list",
      "inputSchema": {
        "type": "object",
        "properties": {},
        "required": [],
        "additionalProperties": false
      }
    },
    {
      "name": "get_users",
      "description": "Returns OpenAM user list from the default (root) realm",
      "inputSchema": {
        "type": "object",
        "properties": {
          "arg0": {
            "type": "string",
            "description": "If not set, uses root realm"
          },
          "arg1": {
            "type": "string",
            "description": "Username filter"
          }
        },
        "required": [],
        "additionalProperties": false
      }
    },
    {
      "name": "get_auth_modules",
      "description": "Returns OpenAM authentication modules list",
      "inputSchema": {
        "type": "object",
        "properties": {
          "arg0": {
            "type": "string",
            "description": "If not set, uses root realm"
          }
        },
        "required": [],
        "additionalProperties": false
      }
    },
    {
      "name": "create_user",
      "description": "Creates a new user",
      "inputSchema": {
        "type": "object",
        "properties": {
          "arg0": {
            "type": "string",
            "description": "If not set, uses root realm"
          },
          "arg1": {
            "type": "string",
            "description": "Username (login)"
          },
          "arg2": {
            "type": "string",
            "description": "Password (min length 8)"
          },
          "arg3": {
            "type": "string",
            "description": "User family name"
          },
          "arg4": {
            "type": "string",
            "description": "User given name"
          },
          "arg5": {
            "type": "string",
            "description": "Name"
          },
          "arg6": {
            "type": "string",
            "description": "Email"
          },
          "arg7": {
            "type": "string",
            "description": "Phone number"
          }
        },
        "required": [
          "arg1",
          "arg2"
        ],
        "additionalProperties": false
      }
    },
    {
      "name": "delete_user",
      "description": "Deletes a user",
      "inputSchema": {
        "type": "object",
        "properties": {
          "arg0": {
            "type": "string",
            "description": "If not set, uses root realm"
          },
          "arg1": {
            "type": "string",
            "description": "Username (login)"
          }
        },
        "required": [
          "arg1"
        ],
        "additionalProperties": false
      }
    },
    {
      "name": "get_available_modules",
      "description": "Returns all avialable authenticaion modules",
      "inputSchema": {
        "type": "object",
        "properties": {},
        "required": [],
        "additionalProperties": false
      }
    },
    {
      "name": "get_auth_chains",
      "description": "Returns OpenAM authentication chains with modules",
      "inputSchema": {
        "type": "object",
        "properties": {
          "arg0": {
            "type": "string",
            "description": "If not set, uses root realm"
          }
        },
        "required": [],
        "additionalProperties": false
      }
    }
  ]
}
```
