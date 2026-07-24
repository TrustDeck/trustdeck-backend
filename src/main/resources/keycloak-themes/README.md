# Repository-managed Keycloak themes

Every immediate subdirectory is treated as one Keycloak theme and is copied to
`/opt/keycloak/themes/<directory-name>` when the development or production
Keycloak image is built.

The directory can contain any supported Keycloak theme type:

```text
keycloak-themes/
  my-theme/
    login/
      theme.properties
      resources/
        css/
        img/
    account/
      theme.properties
      resources/
        css/
        img/
    admin/
      theme.properties
      resources/
        css/
        img/
    email/
      theme.properties
      ...
```

A theme only needs to provide the types it supports. For example, a theme might only provide `login`, while `trustdeck` provides `login`, `account`, and `admin`.

The first-level directory name is the value selected in Keycloak under
`Realm settings -> Themes`. The corresponding selector must be set separately
for Login, Account, and Admin Console themes. To style the master Admin Console,
set the Admin Console theme on the `master` realm.

The TrustDeck Account Console theme extends `keycloak.v3`; the TrustDeck Admin
Console theme extends `keycloak.v2`. When upgrading Keycloak, verify that those parent themes still exist and review the custom CSS against the new PatternFly/console version.

After adding or changing a theme, rebuild/recreate Keycloak through
`./trustdeck.sh dev start` or `./trustdeck.sh prod start`. The startup script
validates all theme directories before Docker builds the image.
