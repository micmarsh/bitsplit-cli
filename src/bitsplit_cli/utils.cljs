(ns bitsplit-cli.utils
    (:use      
        [cljs.core.async :only (put! close! chan <! >!)]     
        [bitsplit.storage.protocol :only (all save!)]
        [bitsplit.client.protocol :only (addresses)])
    (:use-macros 
        [cljs.core.async.macros :only (go)]))

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

(defn callback->channel [function]
    (fn [& args]
        (let [return (chan)
              callback 
                (fn [err & results]
                    (if (= 1 (count results))
                        (put! return (first results))
                        (put! return results))
                    (close! return))
              total-args (concat args [callback])]
              (apply function total-args)
              return)))

(defn call-method [object method & args]
    (.apply 
        (callback->channel (aget object method)) 
        object
        (into-array args)))

(defn chans->chan [sequence]
    (go
        (let [array #js []]
            (doseq [channel sequence]
                (.push array (<! channel)))
            (seq array))))