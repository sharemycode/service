package net.sharemycode.service.events;

import net.sharemycode.service.model.Project;

/**
 * This event is raised when a new project is created
 *
 * @author Shane Bryzak
 */
public class NewProjectEvent
{
   private Project project;

   public NewProjectEvent(Project project)
   {
      this.project = project;
   }

   public Project getProject()
   {
      return project;
   }
}
