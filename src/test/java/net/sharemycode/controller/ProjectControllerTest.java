package net.sharemycode.controller;

import java.util.List;

import javax.inject.Inject;

import net.sharemycode.model.Project;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ProjectControllerTest {
    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(ProjectController.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }
    
    @Inject ProjectController projectController;
    
    @Test
    public void createProject() {
        Project p = new Project();
        p.setName("test");
        p.setVersion("0.0.Test");
        p.setDescription("Test project, this is...");
        Project result = projectController.createProject(p);
        Assert.assertNotNull(result);
    }
    
    @Test
    public void listAllProjects() {
        List<Project> projects = projectController.listAllProjects();
        Assert.assertNotNull(projects);
    }
}
