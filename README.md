# Keycloak Get User IDP Token

This is a simple keycloak extension that allows a user to get another users idP token from Keycloak without the need to
impersonate that user.

As it stands right now in Keycloak to get another users idP token you need to impersonate that user, this is actually a
pain to do as you have to go through the process of doing token-exchanges etc... Long and boring.

This extension will instead expose an endpoint that will allow you to get another users idP token by providing the users
ID and the idP alias.

The user/service-account making the request must have the `read-users-idp-tokens` realm role otherwise they will be met
with a 403.


## Installation
Download the latest jar from the releases page and add it to the `providers` folder of your keycloak.


## Usage

After adding the extension to your keycloak instance follow the below steps:

1. Create a new realm role called `read-users-idp-tokens`
2. Assign the role to the user/service account that will be making the requests.
3. Make a GET request to the `/realms/{realm}/user-idp-token?userId={USER_ID}&idpName={IDP_ALIAS}` endpoint with
   the following body:
4. Profit from the response.