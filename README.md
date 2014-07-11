# Bitsplit CLI

A "basic" Bitsplit instance to be run alongside bitcoind

## Usage

Clone it, run it

## lein npm
I had to do this to get things working:
```shell
sudo chown -R $USER ~/tmp/
sudo chown -R $USER ~/.npm/
```
I'm sure this is a symptom of not having set up npm permissions "correctly" in the first place, though.

Also, leiningen and node stdin appear to mess w/ each other, so this needs to be run using `node target/main.js`, not `lein run`

## License

Copyright © 2014 Michael Marsh

Distributed under the Eclipse Public License version 1.0 