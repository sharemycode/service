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
    xw.Sys.getWidget("registerService").post({content:JSON.stringify(props), callback: cb});
  },
    createClassCallback: function(response) {
    xw.Popup.close();
  },
  registerCallback: function(response) {
    xw.Popup.close();
  }
};
