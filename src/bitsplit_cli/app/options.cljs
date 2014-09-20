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

(defn verify-options [options-map]
  (doseq [[k v] options-map]
    (when-not v
      (throw (js/Error. (str "Unrecognized option: " k))))))

(def key-transforms
  {#{"-n" "--network"} "network"
   #{"-i" "--interval"} "interval"
   #{"-v" "--verbose"} "verbose"} )

(def args->options
  (comp
   verify-options
   (partial transform-keys key-transforms)
   seq->map
   rest))
