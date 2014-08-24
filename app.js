var program = "target/main.js";
var daemon = require("daemonize2").setup({
    main: program,
    name: "Bitsplit",
    pidfile: "bitsplit.pid"
});
var child_process = require('child_process');
var args = process.argv.slice(2);

switch (args[0]) {

    case "start":
        daemon.start();
        break;

    case "stop":
        daemon.stop();
        break;

    case "help":
        console.log("Usage: [start|stop]");
        break;

    default:
        child_process.execFile(program, args, { }, function (err, stdout, stderr) {
            console.log(stdout.toString(), stderr.toString());
        });

}
