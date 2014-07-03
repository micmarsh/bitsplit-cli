(ns bitsplit-cli.commands
    (:use clojure.string
          bitsplit.client.protocol
          bitsplit.storage.protocol
          bitsplit-cli.display))

(defn split-cmd [command]
    (-> command
        trim
        (split #" +")))

(def last-rendered (atom nil))

(def commands {
    "list"
        (fn [{:keys [client storage]}]
            (->> storage
                all
                (render last-rendered)))
    "split" 
        (fn [{:keys [client storage]}
             from to percentage]
            (str from \space to \space percentage))
    })


(defn- -execute [{:keys [command] :as system}]
    (let [[cmd & args] (split-cmd command)
          method (commands cmd)]
        (if method
            (try
                (apply method system args)
            (catch clojure.lang.ArityException e 
                (str "Incorrect number of arguments ("
                        (count args) ") for \"" cmd "\"")))
            (str "No such command: \"" cmd \"))))

(def execute (comp println -execute))