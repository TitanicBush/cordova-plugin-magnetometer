var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    Reading = require('./Reading');

const DEFAULT_INTERVAL_MS = 10;

// Is the sensor running?
var running = false;

// Timer refs
var timers = {};

var listeners = [];

var reading = null;

var eventTimerId = null;

function start() {
  exec(function (a) {
    var tempListeners = listeners.slice(0);
    reading = new Reading(a.x, a.y, a.z, a.timestamp);
    for (var i = 0, l = tempListeners.length; i < l; i++) {
      tempListeners[i].win(reading);
    }
  }, function(e) {
    var tempListeners = listeners.slice(0);
    for (var i = 0, l = tempListeners.length; i < l; i++) {
      tempListeners[i].fail(e);
    }
  }, "Magnetomer", "start", []);
  running = true;
}

function stop() {
  exec(null, null, "Magnetometer", "stop", []);
  reading = null;
  running = false;
}

function createCallbackPair(win, fail) {
  return { win: win, fail: fail};
}

// Removes a win/fail listener pair from the listeners array
function removeListeners(l) {
  var idx = listeners.indexOf(l);
  if (idx > -1) {
      listeners.splice(idx, 1);
      if (listeners.length === 0) {
          stop();
      }
  }
}

var magnetometer = {
  getCurrentReading: function (successCallback, errorCallback, options) {
    argscheck.checkArgs('fF0', 'magnetometer.getCurrentReading', arguments);
    var p;
    var win = function (a) {
      removeListeners(p);
      successCallback(a);
    };
    var fail = function (e) {
      removeListeners(p);
      if (errorCallback) {
        errorCallback(e);
      }
    };

    p = createCallbackPair(win, fail);
    listeners.push(p);

    if (!running) {
      start();
    }
  },
  watchReadings: function (successCallback, errorCallback, options) {
    argscheck.checkArgs('fF0', 'magnetometer.watchReadings', arguments);

    var interval = (options && options.interval && typeof options.interval == 'number') ? options.interval : DEFAULT_INTERVAL_MS;
  
    var id = utils.createUUID();

    var p = createCallbackPair(function () {}, function (e) {
      removeListeners(p);
      if (errorCallback) {
        errorCallback(e);
      }
    });
    listeners.push(p);

    timers[id] = {
      timer: window.setInterval(function() {
        if (reading) {
          successCallback(reading);
        }
      }, interval),
      listeners: p
    };

    if (running) {
      if (reading) {
        successCallback(reading);
      } else {
        start();
      }
    }
    return id;
  },
  clearWatch: function (id) {
    if (id && timers[id]) {
      window.clearInterval(timers[id].timer);
      removeListeners(timers[id].listeners);
      delete timers[id];

      if (eventTimerId && Object.keys(timers.length === 0)) {
        window.clearInterval(eventTimerId);
        eventTimerId = null;
      }
    }
  }
};
module.exports = magnetometer;
