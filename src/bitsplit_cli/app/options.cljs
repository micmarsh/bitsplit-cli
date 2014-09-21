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

(defn- transform-keys [transforms map]
  (into { }
        (for [[k v] map]
          (let [new-key (transform-key transforms k)]
            (if (nil? new-key)
              [k nil]
              [new-key v])))))

(defn- verify-keys [map]
  (doseq [[k v] map]
    (when (nil? v)
      (throw (js/Error. (str "Unrecognized option: " (name k))))))
  map)

(defn- error-on-dupes [transforms seq]
  (doseq [seen [(atom #{})]
          item (take-nth 2 seq)]
    (if (contains? @seen item)
      (throw (js/Error. (str "Duplicate option: " item)))
      (swap! seen
             conj (transform-key transforms item))))
  seq)

(defn- parse-values [transforms map]
  (let [transformed (merge-with #(%1 %2) transforms map)]
    (doseq [[k v] transformed]
      (when (nil? v)
        (throw
         (js/Error.
          (str "Illegal value for option \"" (name k) "\": " (get map k))))))
    transformed))

(def digits (set (map str (range 10))))

(defn parse-network [value]
  (case value
    ("main" "mainnet" "m") "main"
    ("test" "testnet" "t") "test"
    nil))

(defn- parse-bool [value]
   (case value
    "true" true
    "false" false
    nil))

(defn- parse-interval [value]
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
   :interval parse-interval
   :debug parse-bool})

(def key-transforms
  {#{"-n" "--network"} :network
   #{"-i" "--interval"} :interval
   #{"-v" "--verbose"} :verbose
   #{"-d" "--debug"} :debug})

(def defaults
  {:network "main"
   :interval "5m"
   :verbose "false"
   :debug "false"})

(def args->options
  (comp
   (partial parse-values val-transforms)
   verify-keys
   (partial merge defaults)
   (partial transform-keys key-transforms)
   seq->map
   (partial error-on-dupes key-transforms)
   rest))
