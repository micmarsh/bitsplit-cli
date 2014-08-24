(ns bitsplit-cli.core
    (:use [cljs.core.async :only (chan put! <!)]
          [bitsplit.core :only (handle-unspents!)]

          [bitsplit.client.protocol :only (unspent-channel send-amounts!)]
          [bitsplit.storage.protocol :only (all)]

          [bitsplit-cli.filesystem :only (->File)]
          [bitsplit-cli.constants :only (DIR SPLITS_LOCATION)]
          [bitsplit-cli.utils :only (sync-addresses!)]
          [bitsplit-cli.client :only (new-client)]
          [bitsplit-cli.commands :only (execute)])
    (:use-macros
        [cljs.core.async.macros :only (go-loop)]))

(def prompt (js/require "prompt"))
(set! (.-message prompt) "")
(set! (.-delimiter prompt) "")
(set! (.-colors prompt) false)

(set! *print-fn* #(.log js/console %))

(def storage (->File SPLITS_LOCATION))
(def client (new-client DIR))

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

(defn start-repl []
    (.start prompt)
    ; not really tied to repl in long term, but whatever
    (sync-addresses! system)
    (exec-cmd "list")
    (let [unspent-results (handle-unspents! grab-percentages system)]
      (go-loop [result (<! unspent-results)]
        (println (type result))
        (recur (<! unspent-results))))
    (go-loop [command (<! (read-in))]
        (if (exit? command)
            (do (println "Shutting down...")
                (.exit js/process))
            (do
                (exec-cmd command)
                (recur (<! (read-in)))))))

(defn -main [& args]
  (if (empty? args)
    (start-repl)
    (->> args
      (map #(str % \space))
      (apply str)
      exec-cmd)))

(set! *main-cli-fn* -main)
