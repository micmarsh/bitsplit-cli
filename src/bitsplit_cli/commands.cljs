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

(defn- arity? [method args]
    (= (.-length method) (count args)))

(def ^:private bad-args? (comp not arity?))

(defn- -execute [{:keys [command] :as system}]
    (let [[cmd & args] (split-cmd command)
          method (commands cmd)]
        (cond 
            (nil? method)
                (str "No such command: \"" cmd \")
            (bad-args? method (cons system args))
                (str "Incorrect number of arguments ("
                    (count args) ") for \"" cmd "\"")
            :else
                (apply method system args))))

(enable-console-print!)

(def execute (comp println -execute))