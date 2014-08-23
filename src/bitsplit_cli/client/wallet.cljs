(ns bitsplit-cli.client.wallet
  (:require [bitsplit-cli.constants :refer (DIR)]))

(def fs (js/require "fs"))

(def bitcoin (js/require "bitcoinjs-lib"))

(def crypto (.-crypto bitcoin))
(def Wallet (.-Wallet bitcoin))
(def ncrypto (js/require "crypto"))

(defn- load-seed [location]
  (.split
    (try
      (str (.readFileSync fs location))
      (catch js/Error e
        (let [new-seed (.randomBytes ncrypto 256)
              output (str new-seed \newline 0)]
          (.writeFileSync fs location output)
          output)))
    "\n"))

(defn load-wallet
  ([ ]
    (load-wallet (str DIR "seed")))
  ([location]
      (let [[seed i] (load-seed location)
            sha (.sha256 crypto seed)
            wallet (Wallet. sha ;TODO somehow this shit needs
                (-> bitcoin .-networks .-testnet))]
        (doseq [_ (range (js/Number i))]
          (.generateAddress wallet))
        (set! (.-location wallet) location)
        wallet))) ; TODO reference global testnet constant?

(defn inc* [thing]
  (-> thing
    (js/Number)
    (inc)))

(defn generate-address! [wallet]
  (let [address (.generateAddress wallet)
        location (.-location wallet)
        current (.readFileSync fs location)
        [seed i] (-> current (str) (.split "\n"))]
    (.writeFileSync fs location
      (str seed \newline (inc* i)))
    address))