(ns bitsplit-cli.commands
    (:use clojure.string
          bitsplit.client.protocol
          bitsplit.storage.protocol))

(defn split-cmd [command]
    (-> command
        trim
        (split #" +")))

(def ^:private commands (atom { }))

(swap! commands assoc 
    "list"
        (fn [{:keys [client storage]} which]
            (println 
                (case which
                    "addresses"
                        (addresses client)
                    "splits"
                        (all storage))))
    "split" 
        (fn [{:keys [client storage]}
             from to percentage]
            (println from to percentage)))

(def prstr (comp println str))

(defn execute [{:keys [command] :as system}]
    (let [[cmd & args] (split-cmd command)
          method (@commands cmd)]
        (if method
            (try
                (apply method system args)
            (catch clojure.lang.ArityException e 
                (prstr "Incorrect number of arguments ("
                        (count args) ") for \"" cmd "\"")))
            (prstr "No such command: \"" cmd \"))))
