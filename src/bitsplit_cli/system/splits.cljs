(ns bitsplit-cli.system.splits
    (:use
        [cljs.reader :only (read-string)]
        [bitsplit.storage.protocol
            :only (Storage all lookup save! delete!)]))

(def fs (js/require "fs"))

(def fake-file (atom { }))

(defn- read-file
    ([filename]
        (read-file filename { }))
    ([filename default]
        (if (= filename "FAKE")
            @fake-file
            (if (.existsSync fs filename)
                (->> filename
                    (.readFileSync fs)
                    .toString
                    read-string)
                default))))

(defn- write-file [filename data]
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
