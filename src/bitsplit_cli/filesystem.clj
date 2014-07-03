(ns bitsplit-cli.filesystem
    (:use bitsplit.storage.protocol))


(def HOME (System/getProperty "user.home"))
(def DIR (str HOME "/.bitcoin/bitsplit/"))
(.mkdir (java.io.File. DIR))
(def DEFAULT_LOCATION (str DIR "splits"))


(def fake-file (atom { }))

(defn read-file 
    ([filename]
        (try-file filename { }))
    ([filename default]
        (if (= filename "FAKE")
            @fake-file
            (try 
                (load-file filename)
            (catch java.io.FileNotFoundException e
                default)))))

(defn write-file [filename data]
    (if (= filename "FAKE")
        (reset! fake-file data)
        (spit location data)))

(defrecord File [location]
    Storage
    (all [this]
        (read-file location))
    (lookup [this address]
        (let [full-splits (all this)]
            (get full-splits address)))
    (save! [this address splits]
        (let [new-data (assoc data address splits)]
            (write-file location new-data)
            (select-keys new-data [address])))
    (delete! [this address]
        (let [new-data (dissoc data address)]
            (write-file location new-data)
            (select-keys new-data [address])))
    