# magnolia-module-vpro-keycloak
Magnolia Module to make it possible to login into Magnolia using Keycloak 

To use it you need to install this module as you do normally in Magnolia.
(see [Installing a module](https://documentation.magnolia-cms.com/display/DOCS/Installing+a+module))

For this module the dependency is:

    <dependency>
      <groupId>nl.vpro.magnolia</groupId>
      <artifactId>magnolia-module-vpro-keycloak</artifactId>
      <version>1.0</version>
    </dependency>
    
You need to configure a realm in Keycloak to use for your Magnolia instances. 

To be able to use keycloak in Magnolia it is necessary to register a client for each instance in your Keycloak installation.
Use the following settings:
- Client Protocol is openid-connect
- Access Type is confidential

Make sure to set the root url to the url of your Magnolia instance.
Do note that https is required to make logins work.

Also add the following parameters to your magnolia.properties :

    keycloak.realm=example-realm
    keycloak.client.id=example-client-id
    keycloak.auth.url=https://keycloakserver.example.com/auth
    keycloak.credentials.secret=234234-234234-234234
    keycloak.principal.attribute=email
    keycloak.logout.url=${keycloak.auth.url}/realms/${keycloak.realm}/protocol/openid-connect/logout
    keycloak.thisServer.sslPort=443

The values of these settings can be found on the client information pages.
Except for the last one which is the ssl port your server is accessible on. It defaults to the ssl port off 443, in development instances this is most likely 8443
 
The setting _keycloak.principal.attribute_ depends on your realm configuration and how you want 
users to login into your application. See Keycloaks [Java Adapters Config](https://keycloak.gitbooks.io/documentation/securing_apps/topics/oidc/java/java-adapter-config.html) for more information.
The value is used as the principal on the Magnolia side and used to resolve further user information in the Jaas Chain (magnolia-sso) that is configured

So in case of email _user@example.com_ would be used as the principal value in the jaas chain to lookup the user.



