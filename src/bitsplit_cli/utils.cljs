(ns bitsplit-cli.utils
    (:use           
        [bitsplit.storage.protocol :only (all save!)]
        [bitsplit.client.protocol :only (addresses)]))

(defn sync-addresses! [{:keys [client storage]}]
    (let [current (all storage)
          addrs (addresses client)
          valid (select-keys current addrs)]
        (when (not= valid current)
            (doseq [[address splits] valid]
                (save! storage address splits)))
        (when (= 0 (count valid))
            (doseq [address addrs]
                ; (println (.stringify js/JSON client))
                (save! storage address { })))))