(ns bitsplit-cli.addresses
    (:require [clj-btc.core :as btc]))


(defn address? [thing]
    (-> :bitcoinaddress
        (btc/validateaddress thing)
        (get "isvalid")))

(defn address-shortcut [renderable lookup]
    ; this shouldn't be one function
    )