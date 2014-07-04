(ns bitsplit-cli.addresses
    (:require [clj-btc.core :as btc]))

(defn address? [thing]
    (> 8 (count thing))
   )
    ; (-> :bitcoinaddress
    ;     (btc/validateaddress thing)
    ;     (get "isvalid")))

(defn- find-addr [index addr shortcuts]
    (println shortcuts)
    (->> shortcuts
        (map (fn [[_ info]] info))
        (filter #(= (get % index) addr))
        first))

(defn- address [index addr shortcuts]
    (let [next (if (address? addr)
                (find-addr index addr shortcuts)   
                (shortcuts addr))]
        [(get next index) (:to next)]))

(def split-address (partial address :address))

(def sub-address (partial address 0))
