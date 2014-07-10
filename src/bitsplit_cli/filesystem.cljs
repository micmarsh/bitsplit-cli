(ns bitsplit-cli.filesystem
    (:use 
        [bitsplit.storage.protocol 
            :only (Storage all lookup save! delete!)]))
(def fs (js/require "fs"))

(def HOME 
    (let [env (.-env js/process)]
        (or (aget env "HOME") 
            (aget env "USERPROFILE"))))
(def DIR (str HOME "/.bitcoin/bitsplit/"))
(when (not (.existsSync fs DIR))
    (.mkdirSync fs DIR 0766))
(def DEFAULT_LOCATION (str DIR "splits"))


(def fake-file 
    (atom 
        { "dke98di398fdjr98feijr3oifsoij"
            {"dlkjf98398jdlkjrewoiufdz" 0.2 "8328ff98rw98fs98r3" 0.05
             "kd98e8fue7fd87f7eu3u848" 0.75} 
          "kdi9d9ekdkjeufjeueudjudd" {"dlfjoiduwoieu98jkLJKDO" 0.6
             "zsddddddudfoiudsoiajflk" 0.4}}))

(defn read-file 
    ([filename]
        (read-file filename { }))
    ([filename default]
        (if (= filename "FAKE")
            @fake-file
            (try 
                (.readFileSync fs filename)
            (catch e
                default)))))

(defn write-file [filename data]
    (if (= filename "FAKE")
        (reset! fake-file data)
        (.writeFileSync fs filename data)))

(defrecord File [location]
    Storage
    (all [this]
        (read-file location))
    (lookup [this address]
        (let [full-splits (all this)]
            (get full-splits address)))
    (save! [this address splits]
        (let [data (all this)
              new-data (assoc data address splits)]
            (write-file location new-data)
            (select-keys new-data [address])))
    (delete! [this address]
        (let [data (all this)
              new-data (dissoc data address)]
            (write-file location new-data)
            (select-keys new-data [address]))))
