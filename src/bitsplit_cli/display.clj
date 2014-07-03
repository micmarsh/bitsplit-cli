(ns bitsplit-cli.display)

(defn enumerate [things]
    (map-indexed vector things))

(defn address-info [[from addresses]]
    {:address from
     :to (apply merge
            (for [[i info] (enumerate addresses)]
                {(str i) info}))})

(defn percent->string [big-dec]
    (-> big-dec
        (* 100)
        (str "% ")))

(defn- info->string [[index [address percent]]]
    (str "    "
         "(" index ") "
         address " " (percent->string percent)
         \newline))

(defn- render-pair->string [[index info]]
    (apply str "(" index ") " 
        (:address info) 
        \newline
        (map info->string 
            (:to info))))

(defn renderable->string [renderable]
    (->> renderable
        (map render-pair->string)
        (apply str)))

(defn splits->renderable [splits]
    (apply merge
        (for [[i addresses] (enumerate splits)]
            {(str i) (address-info addresses)})))

(defn render [last-rendered splits]
    (->> splits
        splits->renderable
        (reset! last-rendered)
        renderable->string))