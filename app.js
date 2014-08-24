var daemon = require("daemonize2").setup({
    main: "target/main.js",
    name: "Bitsplit",
    pidfile: "bitsplit.pid"
});

switch (process.argv[2]) {

    case "start":
        daemon.start();
        daemon.on('error', function (err, _) {
            console.log("oh shit error", err)
        });
        break;

    case "stop":
        daemon.stop();
        break;

    default:
        console.log("Usage: [start|stop]");
}
