# Keycloak login themes

Every immediate subdirectory is treated as one Keycloak theme and is copied to
`/opt/keycloak/themes/<directory-name>` when the development or production
Keycloak image is built.

Required structure:

```text
keycloak-login-themes/
  my-theme/
    login/
      theme.properties
      resources/
        css/
          styles.css
        img/
          ...
```

The directory name is the value selected in Keycloak under:

`Realm settings -> Themes -> Login theme`

After adding or changing a theme, rebuild/recreate Keycloak through
`./trustdeck.sh dev start` or `./trustdeck.sh prod start`. The startup script
validates all theme directories before Docker builds the image.
