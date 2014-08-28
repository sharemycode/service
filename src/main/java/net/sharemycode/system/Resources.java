package net.sharemycode.system;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import net.sharemycode.Repository;


public class Resources {
    @Produces
    @PersistenceContext(unitName = "sharemycode-default")
    private EntityManager em;
}
