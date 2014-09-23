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
(def fs (js/require "fs"))

(defn- grab-percentages [per unspents]
  {:percentages per
   :unspents unspents})

(def start? (partial = "start"))
(def stop? (partial = "stop"))
(def network #(some #{%} ["main" "test"]))

(def noop (constantly nil))

(defn- build-system [options]
  (let [path (str base-directory cd(:network options) "/")
        _ (when-not (.existsSync fs path)
             (.mkdirSync fs path 0766))
        storage (->File (str path "splits"))
        client (->Client (str path "seed") options)]
    {:storage storage :client client}))

(defn -main [& [cmd & _ :as args]]
  (cond
   (start? cmd)
    (let [options (args->options args)
          system (build-system options)]
      (set! *print-fn*
            (if (:debug options)
              #(.log js/console %)
              (open-log (str base-directory "logfile"))))
      (handle-unspents! grab-percentages system))
    (not (stop? cmd))
    (let [system (build-system {:network (or (network cmd) "main")})
          args (if (network cmd) (rest args) args)
          build-cmd (partial assoc system :command)
          exec-cmd (comp execute build-cmd)]
      (set! *print-fn* #(.log js/console %))
      (exec-cmd (apply str (interpose \space args))))))


(set! *main-cli-fn* -main)
