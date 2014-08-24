var daemon = require("daemonize2").setup({
    main: "target/main.js",
    name: "Bitsplit",
    pidfile: "bitsplit.pid"
});

switch (process.argv[2]) {

    case "start":
        daemon.start();
        break;

    case "stop":
        daemon.stop();
        break;

    case "help":
        console.log("Usage: [start|stop]");
        break;
}
