(ns bitsplit-cli.display.splits)

(defn- round [number]
  (let [remainder (mod number 1)]
    (.floor js/Math
      (if (< remainder 0.5)
        number
        (+ 1 number)))))

(defn show-percent [big-dec]
    (-> big-dec
        (* 100)
        (round)
        (str "% ")))

(def unique-index
  (let [index (atom 0)]
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