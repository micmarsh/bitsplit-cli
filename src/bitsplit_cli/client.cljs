(ns bitsplit-cli.client
    (:use
        [bitsplit.client.protocol :only
        (Queries addresses unspent-amounts unspent-channel
         Operations send-amounts! new-address!)]
        [bitsplit.storage.protocol :only (all)]
        [bitsplit-cli.constants :only (DIR)]
        [bitsplit-cli.utils :only (call-method callback->channel)])
    (:require [cljs.core.async :as a])
    (:use-macros
        [cljs.core.async.macros :only (go)]))

(def request
  (partial
    callback->channel
    (js/require "request")))

(def urls
  ["https://blockchain.info/address/$address$?format=json"])

(defn- balance [result]
  (or (.-final_balance result)))

(defn- parse-body [response]
  (->> response
    (second)
    (.parse js/JSON)))

(defn get-balance [urls address]
  (go
    (if-let [url (-> urls first (.replace "$address$" address))]
      (let [response (a/<! (request url))]
        (if-let [error (:error response)]
          (do
            (println "zomg error" error)
            (a/<! (get-balance (rest urls) address)))
          (-> response parse-body balance))))))

(defn- send? [fee amount]
    (< (* 3 fee) amount))

(defrecord Client [wallet]
    Queries
    (addresses [this]
        (-> wallet .-addresses js->clj))
    (unspent-amounts [this]
      (let [my-addrs (addresses this)]
        (println "read addresses" my-addrs)
        (if-not (empty? my-addrs)
          (let [amount-chan
                 (->> my-addrs
                    (map (partial get-balance urls))
                    a/merge (a/into [ ]))]
            (a/map< (fn [x] (println x) (zipmap my-addrs x)) amount-chan)))))
    (unspent-channel [this]
      (let [return (a/chan)]
        (println "starting unspent loop")
        (js/setInterval
          #(if-let [unspents (unspent-amounts this)]
            (a/take! unspents
              (partial a/put! return)))
          10000)
        return))
    Operations
    (send-amounts! [this amounts]
        #_(a/merge
          (for [[from splits] amounts
                [to amount] splits
                :when (send? (.-fee coin) amount)]
            (let [account (aget (.-aaccounts coin) from)]
              (println from to amount)
              (call-method coin "sendFrom" account to amount)))))
    (new-address! [this]
        #_(-> coin
            .createAccount
            .getAddress)))

(defn- change-dust! [coin]
    (set! (.-dust coin) 1)
    coin)

(defn setup-save-loop! [coin]
  (js/setInterval
    #(.saveWallet coin) 10000)
  coin)

(defn new-client [location]
    (->Client #js {:addresses
        #js ["1LXv8VR7XMaCNAqui9hUicfsZqs4bGFpX4"
             "1GZRg3CHErJNsgoN92JAQzeEzNehMHYSX"]})
    #_(-> {:db
          {:type "tiny" :path
            (str location "tinydb")}
         :wallet
            (str location "wallet.json")}
         clj->js
         coined
         change-dust!
         setup-save-loop!
         (->Client)))
