(ns bitsplit-cli.addressses
    (:require [clj-btc.core :as btc]))


(defn address? [thing]
    (btc/validateaddress
        :address thing))

(defn parse-address [shortcuts addr]
    (cond
        (address? addr) addr
        (shortcuts addr) (shortcuts addr)))