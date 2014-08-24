(ns bitsplit-cli.client.network
  (:require [bitsplit-cli.utils.async :refer (callback->channel)]
            [cljs.core.async :refer (<!)])
  (:use-macros [cljs.core.async.macros :only (go)]))

(def request (js/require "request"))
(def get-request
  (partial callback->channel request))
(def post-request
  (partial callback->channel
    (.-post request)))

#_(def urls
  {:blockchain
    "https://blockchain.info/unspent?address=$address$"
   :helloblock
    "https://mainnet.helloblock.io/v1/addresses/$address$/unspents"})

(def urls
  {:helloblock
    "https://testnet.helloblock.io/v1/addresses/$address$/unspents"})

(def push-urls
  {:helloblock
    "https://testnet.helloblock.io/v1/transactions"})

(def standard-tx
  {:blockchain
    (fn [output]
      {:tx-hash (.-tx_hash output)
       :index (.-tx_index output)
       :value (.-value output)})
   :helloblock
    (fn [output]
      {:tx-hash (.-txHash output)
       :index (.-index output)
       :value (.-value output)})})

(defn- response->txs [which result]
  (if-let [array (or (.-unspent_outputs result) ; blockchain
                     (some-> result ; helloblock
                       (.-data)
                       (.-unspents)))]
    (map (which standard-tx) array)))

(defn- parse-body [which response]
  (try
    (->> response
      (second)
      (.parse js/JSON)
      (response->txs which))
  (catch js/SyntaxError e)))

(defn get-unspents [urls address]
  (go
    (if-let [[which template] (first urls)]
      (let [url (.replace template "$address$" address)
            response (<! (get-request url))]
        (if-let [error (:error response)]
          (do
            (println "zomg error" error)
            (<! (get-unspents (rest urls) address)))
          (parse-body which response))))))

(defn address->unspents [urls address]
  (go
    (let [unspents (<! (get-unspents urls address))]
      [address unspents])))

(defn push-tx [urls tx]
  (go
    (if-let [[which url] (first urls)]
      (let [data (clj->js {:rawTxHex (.toHex tx)})
            ch (-> {:uri url :json data}
                  (clj->js) (post-request))
            response (<! ch)]
        (if-let [error (:error response)]
          (do
            (println "bad push req" error)
            (<! (push-tx (rest urls) tx)))
          (do
            (println response)
            response))))))