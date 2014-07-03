(ns bitsplit-cli.display)

(defn enumerate [things]
    (map-indexed vector things))

(defn address-info [[from addresses]]
    {:address from
     :to (apply merge
            (for [[i info] (enumerate addresses)]
                {(str i) info}))})

(declare renderable->string)

(defn splits->renderable [splits]
    (apply merge
        (for [[i addresses] (enumerate splits)]
            {(str i) (address-info addresses)})))

(defn render [last-rendered splits]
    (->> splits
        splits->renderable
        (reset! last-rendered)
        renderable->string))