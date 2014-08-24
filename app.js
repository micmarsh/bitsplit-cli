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

    default:
        args.unshift(program)
        child_process.exec('node ' + args.join(' '), function (err, stdout, stderr) {
            if (err) {
                console.log("error executing command: ", err);
            }
            console.log(stdout.toString());
        });

}
