(ns bitsplit-cli.commands
    (:use [bitsplit.core :only (add-address! remove-address!)]
          clojure.string
          bitsplit.client.protocol
          bitsplit.storage.protocol
          bitsplit-cli.addresses
          bitsplit-cli.display))

(defn split-cmd [command]
    (-> command
        trim
        (split #" +")))

(def last-rendered (atom nil))

(def ^:private commands {
    "list"
        (fn [{:keys [client storage]}]
            (->> storage
                all
                (render last-rendered)))
    "split" 
        (fn [{:keys [client storage]}
             from to percentage]
            (let [[from-addr sub] (split-address from @last-rendered)
                  [to-addr _] (sub-address to sub)]
                (str from-addr \space to-addr)))
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