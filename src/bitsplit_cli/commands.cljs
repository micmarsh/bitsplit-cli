(ns bitsplit-cli.commands
    (:use [bitsplit.core :only (add-address! remove-address!)]
          [bitsplit.storage.protocol :only (all)]
          [bitsplit-cli.display :only (render)]))

(defn split-cmd [command]
    (-> command
        (.trim)
        (.split #" +")))

(def last-rendered (atom nil))

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

(def ^:private commands {
    "list"
        (fn [{:keys [client storage]}]
            (->> storage
                all
                (render last-rendered)))

    "split" (partial change-split add-address!)

    "unsplit" (partial change-split remove-address!)
    })

(def ^:private arg-counts 
    {"list" 0 "split" 3 "unsplit" 2})

(defn- arity? [method args]
    (= (arg-counts method) 
       (count args)))

(def ^:private bad-args? (comp not arity?))

(defn- -execute [{:keys [command] :as system}]
    (let [[cmd & args] (split-cmd command)
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

(def execute (comp println -execute))