(ns bitsplit-cli.commands
    (:use [bitsplit.core :only (add-address! remove-address!)]
          [bitsplit.storage.protocol :only (all save!)]
          [bitsplit.client.protocol :only (new-address! unspent-amounts)]
          [bitsplit-cli.display.splits :only (render show-address)]
          [bitsplit-cli.utils.async :only (chan?)]
          [cljs.core.async :only (map< take!)])
    (:use-macros [cljs.core.async.macros :only (go)]))

(defn split-cmd [command]
    (-> command
        (.trim)
        (.split #" +")))

(def last-rendered (atom nil))

(declare commands)

(defn change-split
    ([method system from to]
        (change-split method system from to 0))
    ([method {:keys [client storage] :as system}
      from to percentage]
      (let [addr1 (or (@last-rendered from) from)
            addr2 (or (@last-rendered to) to)]
            (method storage
              {:parent addr1
               :address addr2
               :percent (js/Number percentage)})
            ((commands "list") system))))

(defn unspent->string [[addr unspents]]
  (str " " addr "  "
       (->> unspents (map :value) (reduce +))
       " satoshis\n"))

(def ^:private commands {
    "list"
        (fn [{:keys [client storage]}]
            (->> storage
                all
                (render last-rendered)))

    "split" (partial change-split add-address!)

    "unsplit" (partial change-split remove-address!)

    "unspent" 
    (fn [{:keys [client]}]
      (go
       (let [unspents (<! (unspent-amounts client))]
         (->> unspents
              (map unspent->string)
              (apply str \newline)))))

    "generate"
      (fn [{:keys [client storage] :as system}]
        (let [address (new-address! client)]
          (save! storage address { })
          ((commands "list") system)))
    })

(def ^:private arg-counts
  {"list" 0 "split" 3
   "unsplit" 2 "generate" 0
   "unspent" 0})

(defn- arity? [method args]
    (= (arg-counts method)
       (count args)))

(def ^:private bad-args? (comp not arity?))

(defn- -execute [{:keys [command] :as system}]
    (let [[cmd & args] (split-cmd command)
          _ ((commands "list") system)
          method (commands cmd)]
        (cond
            (nil? method)
                (str "No such command: \"" cmd \")
            (bad-args? cmd args)
                (str "Incorrect number of arguments ("
                    (count args) ") for \"" cmd "\"")
            :else
                (apply method system args))))

(enable-console-print!)

(defn print-chan [thing]
  (try
    (take! thing println)
    (catch js/Error e
      (println thing))))

(def execute (comp print-chan -execute))
