(ns bitsplit-cli.utils.constants)
(def fs (js/require "fs"))

(def user-home
    (let [env (.-env js/process)]
        (or (aget env "HOME")
            (aget env "USERPROFILE"))))
(def base-directory (str user-home "/.bitcoin/bitsplit/"))
(when (not (.existsSync fs base-directory))
    (.mkdirSync fs base-directory 0766))
(def splits-location (str base-directory "splits"))