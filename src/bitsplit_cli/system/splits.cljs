(ns bitsplit-cli.system.splits
    (:use
        [cljs.reader :only (read-string)]
        [bitsplit.storage.protocol
            :only (Storage all lookup save! delete!)]
        [bitsplit-cli.utils.storage :only (read-file write-file)]))

(defrecord File [location]
    Storage
    (all [this]
        (-> location
          (read-file)
          (read-string)))
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
