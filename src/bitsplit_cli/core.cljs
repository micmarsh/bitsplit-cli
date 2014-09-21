(ns bitsplit-cli.core
    (:use [cljs.core.async :only (chan put! <!)]
          [bitsplit.core :only (handle-unspents!)]

          [bitsplit.client.protocol :only (unspent-channel send-amounts!)]
          [bitsplit.storage.protocol :only (all)]

          [bitsplit-cli.system.splits :only (->File)]
          [bitsplit-cli.utils.constants :only (base-directory splits-location)]
          [bitsplit-cli.utils.storage :only (sync-addresses!)]
          [bitplit-cli.system.client :only (->Client)]
          [bitsplit-cli.commands :only (execute)]
          [bitsplit-cli.utils.log :only (open-log)]
          [bitsplit-cli.app.options :only (args->options)])
    (:use-macros
        [cljs.core.async.macros :only (go-loop)]))

(defn- grab-percentages [per unspents]
  {:percentages per
   :unspents unspents})

(def start? (partial = "start"))
(def noop (constantly nil))

(defn- build-system [options]
  (let [storage (->File splits-location)
        client (->Client (str base-directory "seed") options)]
    {:storage storage :client client}))

(defn -main [& [cmd & _ :as args]]
  (let [options (args->options args)]
    (if (start? cmd)
      (let [system (build-system options)]
        (println options)
        (set! *print-fn*
              (if (:debug options)
                #(.log js/console %)
                (open-log (str base-directory "logfile"))))
        (handle-unspents! grab-percentages system))
      (let [options (args->options args)
            build-cmd (partial assoc system :command)
            exec-cmd (comp execute build-cmd)]
        (set! *print-fn* #(.log js/console %))
        (exec-cmd (apply str (interpose \space args)))))))

(defn- filter-stop [main]
  (fn [& [cmd & _ :as args]]
    (when-not (= "stop" cmd)
      (apply main args))))

(set! *main-cli-fn*
  (filter-stop -main))
