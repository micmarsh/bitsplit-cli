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
          [bitsplit-cli.utils.log :only (open-log)])
    (:use-macros
        [cljs.core.async.macros :only (go-loop)]))

(def prompt (js/require "prompt"))
(set! (.-message prompt) "")
(set! (.-delimiter prompt) "")
(set! (.-colors prompt) false)

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

(defn- seq->map [seq]
  (->> seq
       (partition 2)
       (map vec)
       (into { })))

(defn transform-key [transforms key]
  (->> transforms
       (keys)
       (filter #(contains? % key))
       (first)
       (transforms)))

(defn transform-keys [transforms map]
  (into { }
        (for [[k v] map]
          (let [new-key (transform-key transforms k)]
            (if (nil? new-key)
              [k nil]
              [new-key v])))))

(defn verify-options [options-map]
  (doseq [[k v] options-map]
    (when-not v
      (throw (js/Error. (str "Unrecognized option: " k))))))

(def key-transforms
  {#{"-n" "--network"} "network"
   #{"-i" "--interval"} "interval"
   #{"-v" "--verbose"} "verbose"} )

(def option-names #{"network" "interval" "verbose"})

(def args->options
  (comp
   verify-options
   (partial transform-keys key-transforms)
   seq->map
   rest))

(defn -main [& [cmd & _ :as args]]
  (let [storage (->File splits-location)
        client (->Client (str base-directory "seed"))
        system {:storage storage :client client}]
    (if  (or (= "start-debug" cmd) (start? cmd))
      (do
        (set! *print-fn*
              (if (start? cmd)
                (open-log (str base-directory "logfile"))
                #(.log js/console %)))
        (handle-unspents! grab-percentages system))
      (let [build-cmd (partial assoc system :command)
            exec-cmd (comp execute build-cmd)]
        (set! *print-fn* #(.log js/console %))
        (exec-cmd (apply str (interpose \space args)))))))

(defn- filter-stop [main]
  (fn [& [cmd & _ :as args]]
    (when-not (= "stop" cmd)
      (apply main args))))

(set! *main-cli-fn*
  (filter-stop -main))
