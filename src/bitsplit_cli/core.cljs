(ns bitsplit-cli.core
    (:use [cljs.core.async :only (chan put! <!)]
          [bitsplit.core :only (handle-unspents!)]

          [bitsplit.client.protocol :only (unspent-channel send-amounts!)]
          [bitsplit.storage.protocol :only (all)]

          [bitsplit-cli.system.splits :only (->File)]
          [bitsplit-cli.utils.constants :only (base-directory splits-location)]
          [bitsplit-cli.utils.storage :only (sync-addresses!)]
          [bitplit-cli.system.client :only (new-client)]
          [bitsplit-cli.commands :only (execute)]
          [bitsplit-cli.utils.log :only (open-log)])
    (:use-macros
        [cljs.core.async.macros :only (go-loop)]))

(def prompt (js/require "prompt"))
(set! (.-message prompt) "")
(set! (.-delimiter prompt) "")
(set! (.-colors prompt) false)

(set! *print-fn* #(.log js/console %))

(def )
(def )

(def system {:storage storage :client client})
(def build-cmd (partial assoc system :command))
(def exec-cmd (comp execute build-cmd))

(defn- read-prompt [message]
    (let [return (chan 1)]
        (.get prompt message
            (fn [err input]
                (when (-> input nil? not)
                    (put! return
                        (aget input message)))))
        return))

(def read-in (partial read-prompt "bitsplit> "))

(defn exit? [command]
    (->> "exit"
          seq
          (= (take 4 command))))

(defn- grab-percentages [per unspents]
  {:percentages per
   :unspents unspents})


(defn -main [& args]
  (let [log (open-log "logfile")
        storage (->File splits-location)
        client (new-client (str base-directory "seed") log)
        unspent-results (handle-unspents! grab-percentages system)]))

(set! *main-cli-fn* -main)
