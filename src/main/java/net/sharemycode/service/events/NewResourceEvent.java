package net.sharemycode.service.events;

import net.sharemycode.service.model.ProjectResource;

/**
 * This event is raised when a new project resource is created
 *
 * @author Shane Bryzak
 */
public class NewResourceEvent
{
   private ProjectResource resource;

   public NewResourceEvent(ProjectResource resource)
   {
      this.resource = resource;
   }

   public ProjectResource getResource()
   {
      return resource;
   }
}
