(ns bitsplit-cli.utils.storage
 (:require [bitsplit.storage.protocol :refer (all save!)]
           [bitsplit.client.protocol :refer (addresses)]))

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