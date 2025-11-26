# OpenAM Alternative Frontend SDK

OpenAM is a robust access management solution, but integrating it with modern frontend applications can be complex and time-consuming. This SDK aims to simplify that process by providing pre-built React components and a flexible, modular setup, saving developers significant time while ensuring secure, seamless integration with OpenAM.

This project is intended to provide an alternative frontend SDK for interacting with Open Identity Platform's OpenAM authentication services. It is built using modern web technologies and aims to simplify the integration process for developers.

## Features
- **Ease of Use**: Pre-configured React components ready for integration.
- **Modular & Flexible**: Easily swap components and customize the SDK to suit your needs.
- **TypeScript Support**: Enhance development experience with type safety and better code completion.
- **Seamless Integration**: Easily integrate OpenAM with minimal configura

# Prerequisites
- Node.js 22 LTS and newer
- OpenAM 14 and newer

## Installation

Clone and build the source code

```bash
git clone https://github.com/OpenIdentityPlatform/openam-js-sdk.git
```

```bash
cd openam-js-sdk
npm install
npm run build
```

## Usage

### As an Application

Copy the contents of the `dist/app` folder into your OpenAM WAR file (or the extracted WAR contents in your web container), e.g., into a directory like `extui`, so it could be accessible in your OpenAM context path, for example, http://openam.example.org:8080/openam/extui

You can also run the application in a standalone server. The only condition, the servers shold be on the same subdomain, so OpenAM's cookies could be sent from the frontend application.


## As an SDK library

To install the SDK, use npm or yarn:

```bash
npm install <path to openam-js-sdk folder> #for example /home/user/projects/openam-js-sdk
# or
yarn add <path to openam-js-sdk folder>
``` 
## Usage
Here's a basic example of how to use the SDK in a React application:

```tsx
import React from 'react';
import OpenAMUI from 'openam-js-sdk';

const App = () => {
  return (
      <OpenAMUI />
  );
};
```

## Customization

You can customize the SDK by providing your own UI components and styles. 

To customize the application behaviour, customise the following settings:

```ts
export interface Config {
    openamServer: string; //OpenAM server host, for example http://openam.example.org:8080
    openamContextPath: string; //OpenAM context path, for example /openam
    loginForm: LoginForm; //LoginForm interface implementation
    userForm: UserForm; //UserForm interface implementation
    errorForm: ErrorForm; //ErrorForm interface implementation
    callbackElement: CallbackElement; //CallbackElement interface implementation
    actionElements: ActionElements; //ActionElements interface implementation
    redirectOnSuccessfulLogin: boolean; //redirects user on successful login to the target URL, otherwise shows a profile.
    getOpenAmUrl: () => string; //returns a full OpenAM URL, for example http://openam.example.org:8080/openam
}
```

for example

```tsx
//update the default configuration
import { setConfig } from 'openam-js-sdk'

setConfig({
  openamServer: 'https://openam.example.org:443',
  openamContextPath: '/am',
  errorForm: ({ error, resetError }) => {
    return <div>
      <h1>An error occurred</h1>
      <p>{error?.message}</p>
      <input type="button" value="Retry" onClick={() => resetError()} />
    </div>
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <OpenAMUI />
  </StrictMode>,
)
```

There are components you can override:

```tsx
// renders a login form with callbacks
export type LoginForm = React.FC<{
  authData: AuthData,
  setCallbackValue: (i: number, val: string) => void,
  doLogin: (action: string) => void
}> 

// renders a callback such as NameCallback, PasswordCallback and so on
export type CallbackElement = React.FC<{
    callback: Callback
    setCallbackValue: (val: string) => void
}>

// renders a user profile form
export type UserForm = React.FC<{
  userAuthData: UserAuthData;
  userService: UserService;
}>

// renders an authentication error form
export type ErrorForm = React.FC<{
    error: AuthError,
    resetError: () => void
}>

// renders submit buttons, if there are no ConfirmationCallback in the callbacks array, renders the default button
export type ActionElements = React.FC<{callbacks: Callback[]}>
```


## Contributing
Contributions are welcome! Please fork the repository and submit a pull request with your changes. Make sure to follow the coding standards and include tests for any new features.

