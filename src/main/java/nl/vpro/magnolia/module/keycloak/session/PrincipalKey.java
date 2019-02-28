/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author r.jansen
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class PrincipalKey {
    private String name;
    private String realm;
}
