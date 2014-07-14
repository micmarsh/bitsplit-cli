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
                (save! storage address { })))))

(defn callback->channel [function & args]
    (let [return (chan)
          callback 
            (fn [err & results]
                (cond (= 1 (count results))
                        (put! return (first results))
                      (> (count results) 1)
                        (put! return results))
                (close! return))
          total-args (concat args [callback])]
          (apply function total-args)
          return))

(defn call-method [object method & args]
    (let [function (aget object method)
          bound (.bind js/goog function object)
          asynced (partial callback->channel bound)]
        (apply asynced args)))

(defn chans->chan [sequence]
    (go
        (let [array #js []]
            (doseq [channel sequence]
                (.push array (<! channel)))
            (seq array))))

(defn empty-chan []
    (let [c (chan)] 
        (close! c) 
        c))