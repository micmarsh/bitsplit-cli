(ns bitsplit-cli.display.splits)

(defn show-percent [big-dec]
    (-> big-dec
        (* 100)
        (str "% ")))

(def unique-index
    (let [index (atom 0)]
    ; TODO smarter memoize/cache that won't grow forever
    (memoize (fn [_] (swap! index inc)))))


(defn show-address [shortcuts address]
    (let [i (unique-index address)]
        (swap! shortcuts assoc (str i) address)
        (str \( i \) " " address)))

(defn- show-addresses [shortcuts [address percent]]
    (str "    "
        (show-address shortcuts address)
        " "  (show-percent percent)
        \newline))

; todo given the existence of unique id, can do this in a very simple way
(defn- show-splits [shortcuts [address children]]
    (apply str
        (show-address shortcuts address)
        \newline
        (map (partial show-addresses shortcuts) children)))


(defn render [shortcuts splits]
    (apply str \newline
        (map (partial show-splits shortcuts) splits)))