(ns bitsplit-cli.utils.async
    (:use
        [cljs.core.async :only (put! close! merge chan <! >!)])
    (:use-macros
        [cljs.core.async.macros :only (go)]))

(defn callback->channel [function & args]
  (let [return (chan)
        callback
          (fn [err & results]
              (cond
                err
                  (put! return {:error err})
                (= 1 (count results))
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

(defn empty-chan []
    (let [c (chan)]
        (close! c)
        c))