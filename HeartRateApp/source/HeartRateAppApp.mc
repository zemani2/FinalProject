import Toybox.Application;
import Toybox.Lang;
import Toybox.WatchUi;

class HeartRateAppApp extends Application.AppBase {

    function initialize() {
        AppBase.initialize();
    }

    // onStart() is called on application start up
    function onStart(state as Dictionary?) as Void {
    }

    // onStop() is called when your application is exiting
    function onStop(state as Dictionary?) as Void {
    }

    // Return the initial view of your application here
    function getInitialView() as [Views] or [Views, InputDelegates] {
        return [ new HeartRateAppView() ];
    }

}

function getApp() as HeartRateAppApp {
    return Application.getApp() as HeartRateAppApp;
}