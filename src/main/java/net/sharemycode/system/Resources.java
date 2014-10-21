package net.sharemycode.system;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Resources
 * - Defines EntityManager Persistence Context
 * 
 * @author Lachlan Archibald
 */

public class Resources {
    @Produces
    @PersistenceContext(unitName = "sharemycode-default")
    private EntityManager em;
}
