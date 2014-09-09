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
    switch(response.status) {
      case 200:
        var m = document.createTextNode("Registration successful!");
        d.appendChild(m);
        s.style.display = "block";
        f.style.display = "none";
        break;
      case 400:
        var m = document.createTextNode(response.message);
        d.appendChild(m);
        s.style.display = "none";
        f.style.display = "block";
        break;      
      case 500:     
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