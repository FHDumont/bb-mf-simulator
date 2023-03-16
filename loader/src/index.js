require("events").EventEmitter.prototype._maxListeners = 0;

var transactionsLoad = require("./transactions");

var main = function () {
  transactionsLoad.main();
};

main();
