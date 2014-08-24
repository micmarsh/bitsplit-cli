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

(def start? (partial = "start"))
(def noop (constantly nil))

(defn -main [& [cmd & _ :as args]]
  (let [storage (->File splits-location)
        log (if (start? cmd) (open-log (str base-directory "logfile")) noop)
        client (new-client (str base-directory "seed") log)
        system {:storage storage :client client}]
    (if (start? cmd)
      (handle-unspents! grab-percentages system)
      (let [build-cmd (partial assoc system :command)
            exec-cmd (comp execute build-cmd)]
        (exec-cmd (apply str (interpose \space args)))))))

(defn- filter-stop [main]
  (fn [& [cmd & _ :as args]]
    (when-not (= "stop" cmd)
      (apply main args))))

(set! *main-cli-fn*
  (filter-stop -main))
