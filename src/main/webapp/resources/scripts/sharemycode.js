/* INITIALIZATION */
xw.Ajax.loadingCallback = function(requests) {
  var ctl = xw.Sys.getObject("ajax");
  if (requests > 0) {
    ctl.style.display = "block";
  } else {
    ctl.style.display = "none";
  }
};

/* Set the Log level */
xw.Log.logLevel = "DEBUG";

var ShareMyCode = {
  registerUser: function(props) {
    var cb = function(response) {
      ShareMyCode.registerCallback(JSON.parse(response));
    };
    xw.Sys.getWidget("registrationService").post({content:JSON.stringify(props), callback: cb});
  },
    createClassCallback: function(response) {
    xw.Popup.close();
  },
  registerCallback: function(response) {
    var d = xw.Sys.getObject("response_message");
    var s = xw.Sys.getObject("success_response");
    var f = xw.Sys.getObejct("failure_response");
    xw.Sys.clearChildren(d);
    var overlay = xw.Sys.getObject("popupOverlay");
    select(response.status) {
      case 200:
        var m = document.createTextNode("Registration successful!");
        d.appendChild(m);
        s.style.display = "block";
        f.style.display = "none";
      case 400:
        var m = document.createTextNode(response.message);
        d.appendChild(m);
        s.style.display = "none";
        f.style.display = "block";      
      case 500:
        var m = document.createTextNode(response.message);
        d.appendChild(m);
        s.style.display = "none";
        f.style.display = "block";      
      default:
        var m = document.createTextNode(response.message);
        d.appendChild(m);
        s.style.display = "none";
        f.style.display = "block"; 
    }
    d.style.display = "block";
    overlay.style.display = "block";
  }
};
