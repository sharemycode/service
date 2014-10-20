/* Set the Log level */
xw.Log.logLevel = "DEBUG";

var ShareMyCode = {
    projects: [],
    projectExplorer: null,
    messageHandler: {},
    selectedProject: function() {
      var n = ShareMyCode.projectExplorer.selectedNode;
      while (n.parent != null) {
        n = n.parent;
      }
      return n.userObject;
    },
    selectedResource: function() {
      var n = ShareMyCode.projectExplorer.selectedNode;
      return n.parent != null ? n.userObject : null;
    },
    getProjectById: function(projectId) {
      for (var i = 0; i < ShareMyCode.projects.length; i++) {
        if (ShareMyCode.projects[i].id == projectId) {
          return ShareMyCode.projects[i];
        }
      }  
    },
    getResourceById: function(projectId, resourceId) {
      var p = ShareMyCode.getProjectById(projectId);
      for (var i = 0; i < p.resources.length; i++) {
        if (p.resources[i].id == resourceId) {
          return p.resources[i];
        }
      }
    },
  registerUser: function(props) {
    var cb = function(message, response) {
      ShareMyCode.registerCallback(message, response.status);
    };
    xw.Sys.getWidget("registrationService").post({content:JSON.stringify(props), callback: cb});
  },
  createProject: function(props) {
    props.attachments = window.attachments;
    var cb = function(message, response) {
        window.attachments.length = 0;	// clear the global array
        ShareMyCode.registerCallback(message, response.status);
    };
    xw.Sys.getWidget("projectService").post({content: JSON.stringify(props), callback: cb});
  },
  parseProjects: function(result) {
    return result;
  },
  loginUser: function(props) {
    var cb = function(message, response) {
		  ShareMyCode.loginCallback(message, response.status);
    };
    xw.Sys.getWidget("authenticationService").post({content:JSON.stringify(props), callback: cb});
  },
  createClassCallback: function(response) {
    xw.Popup.close();
  },
  loginCallback: function(message, status) {
    var r  = xw.Sys.getObject("serverResponse");
    var d = xw.Sys.getObject("responseMessage");
    xw.Sys.clearChildren(d);
    var m = document.createTextNode(message);
    d.appendChild(m);
    ShareMyCode.updateResponseStatus(status);
    r.style.display = "block";
  },
  registerCallback: function(message, status) {
    var r  = xw.Sys.getObject("serverResponse");
    var d = xw.Sys.getObject("responseMessage");
    xw.Sys.clearChildren(d);
    var m = document.createTextNode(message);
    d.appendChild(m);
    ShareMyCode.updateResponseStatus(status);
    r.style.display = "block";
  },
  updateCallback: function(message, response) {
    var r  = xw.Sys.getObject("serverResponse");
    var d = xw.Sys.getObject("responseMessage");
    xw.Sys.clearChildren(d);
    var m = document.createTextNode(message);
    d.appendChild(m);
    ShareMyCode.updateResponseStatus(response.status);
    r.style.display = "block";
  },
  updateResponseStatus: function(status) {
    var s = xw.Sys.getObject("successResponse");
    var f = xw.Sys.getObject("failureResponse");
    switch(status) {
      case 200:
        s.style.display = "block";
        f.style.display = "none";
        break;
      case 201:
        s.style.display = "block";
        f.style.display = "none";
        break;
      case 400:
      case 404:
      case 500:     
      default:
        s.style.display = "none";
        f.style.display = "block";
    }
  },
  createUploader: function() {
    var uploader = new qq.FileUploader({
      debug: true,
      // pass the dom node (ex. $(selector)[0] for jQuery users)
      element: document.getElementById('file-uploader'),
      // path to server-side upload script
      action: 'upload',
      sizeLimit: 10485760,	// 10MB
      onComplete: function(id, filename, responseJSON) {
        if (responseJSON.success) {
          // add attachment id to array (global)
          attachments.push(responseJSON.id);
        }
      }
    });

  },
  setProjectExplorer: function(tree) {
    ShareMyCode.projectExplorer = tree;
  },
  addProjectNode: function(project, select) {
    var n = new org.xwidgets.core.TreeNode(project.name, false, project);
    ShareMyCode.projects.push({
      id: project.id,
      name: project.name,
      node: n,
      resources: []});
    ShareMyCode.projectExplorer.model.addRootNode(n); 
    if (select) {
      ShareMyCode.projectExplorer.selectNode(n);
    }
  },
  addResourceNode: function(resource, select) {
    var leaf = resource.resourceType != "DIRECTORY";
    var n = new org.xwidgets.core.TreeNode(resource.name, leaf, resource);  

    var p = ShareMyCode.getProjectById(resource.project.id);
    p.resources.push({
      id: resource.id,
      name: resource.name,
      type: resource.type,
      node: n
    });

    if (resource.parent != null) {
      var r = ShareMyCode.getResourceById(resource.project.id, resource.parent.id);
      r.node.add(n);
    } else {
      p.node.add(n);
    }
    
    if (select) {
      ShareMyCode.projectExplorer.selectNode(n);
    }
  },
  openResource: function(id) {
    xw.Sys.getWidget("projectListener").send(ShareMyCode.createMessage("resource", "open", {id:(id + "")}));
  }
};
ShareMyCode.resourceManager = {
    openResources: [],
    openResource: function(id) {
      ShareMyCode.openResource(id);
    }
  };

var attachments = [];	// create array for attachment id's
