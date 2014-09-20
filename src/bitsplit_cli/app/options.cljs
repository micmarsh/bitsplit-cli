(ns bitsplit-cli.app.options)

(defn- seq->map [seq]
  (->> seq
       (partition 2)
       (map vec)
       (into { })))

(defn- transform-key [transforms key]
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

(defn verify-keys [map]
  (doseq [[k v] map]
    (when-not v
      (throw (js/Error. (str "Unrecognized option: " (name k))))))
  map)

(defn error-on-dupes [seq]
  (doseq [seen [(atom #{})]
          item (take-nth 2 seq)]
    (if (contains? @seen item)
      (throw (js/Error. (str "Duplicate option: " item)))
      (swap! seen conj item)))
  seq)

(def verify-values identity)

(def digits (set (map str (range 10))))

(defn parse-network [value]
  (case value
    ("main" "mainnet" "m") "main"
    ("test" "testnet" "t") "test"
    nil))

(defn parse-bool [value]
  (case value
    "true" true
    "false" false
    nil))

(defn parse-interval [value]
     (let [number (apply str (take-while digits value))
           unit (apply str (drop-while digits value))
           multiplier
           (case unit
             "ms" 1
             "s" 1000
             "m" 60000
             "h" 3600000
             1)]
       (when-not (some nil? [number unit])
         (* (js/Number. number) multiplier))))

(def val-transforms
  {:network parse-network
   :verbose parse-bool   
   :interval parse-interval})

(def key-transforms
  {#{"-n" "--network"} :network
   #{"-i" "--interval"} :interval
   #{"-v" "--verbose"} :verbose})

(def args->options
  (comp
   verify-values
   (partial merge-with apply val-transforms)
   verify-keys
   (partial transform-keys key-transforms)
   seq->map
   error-on-dupes
   rest))
