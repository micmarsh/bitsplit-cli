(ns bitsplit-cli.utils.storage
 (:require [bitsplit.storage.protocol :refer (all save!)]
           [bitsplit.client.protocol :refer (addresses)]))


(def fs (js/require "fs"))

(def fake-file (atom { })) ;in memory "file",
; obviously global and non-scalable, probably don't need at all

(defn read-file
    ([filename]
        (read-file filename { }))
    ([filename default]
        (if (= filename "FAKE")
            @fake-file
            (if (.existsSync fs filename)
                (->> filename
                    (.readFileSync fs)
                    .toString)
                (if (fn? default)
                  (default)
                  default)))))

(defn write-file [filename data]
    (if (= filename "FAKE")
        (reset! fake-file data)
        (.writeFileSync fs filename data)))

(defn sync-addresses! [{:keys [client storage]}]
  (let [current (all storage)
        addrs (addresses client)
        valid (select-keys current addrs)]
    (when (not= valid current)
      (doseq [[address splits] valid]
        (save! storage address splits)))
    (when (= 0 (count valid))
      (doseq [address addrs]
        (save! storage address { })))))