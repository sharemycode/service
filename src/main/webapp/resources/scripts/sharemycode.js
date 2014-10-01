/* Set the Log level */
xw.Log.logLevel = "DEBUG";

var ShareMyCode = {
  registerUser: function(props) {
    var cb = function(message, response) {
      ShareMyCode.registerCallback(message, response.status);
    };
    xw.Sys.getWidget("registrationService").post({content:JSON.stringify(props), callback: cb});
  },
  createProject: function(props) {
    props.attachments = window.attachments;
    var cb = function(message, response) {
        ShareMyCode.registerCallback(message, response.status);
    };
    xw.Sys.getWidget("projectService").post({content: JSON.stringify(props), callback: cb});
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
  updateResponseStatus: function(status) {
    var s = xw.Sys.getObject("successResponse");
    var f = xw.Sys.getObject("failureResponse");
    switch(status) {
      case 200:
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
          // add attachment id to array
          attachments.push(responseJSON.id);
        }
      }
    });

  }
};
var user = {
    listProjects: function() {
      return xw.Sys.getWidget("projectService").get(function (data) { return data; });
    }
}
var attachments = [];	// create array for attachment id's
