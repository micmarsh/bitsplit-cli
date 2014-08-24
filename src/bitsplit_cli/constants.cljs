(ns bitsplit-cli.constants)
(def fs (js/require "fs"))

(def HOME
    (let [env (.-env js/process)]
        (or (aget env "HOME")
            (aget env "USERPROFILE"))))
(def DIR (str HOME "/.bitcoin/bitsplit/"))
(when (not (.existsSync fs DIR))
    (.mkdirSync fs DIR 0766))
(def SPLITS_LOCATION (str DIR "splits"))