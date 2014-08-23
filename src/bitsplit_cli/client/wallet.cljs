(ns bitsplit-cli.client.wallet
  (:require [bitsplit-cli.constants :refer (DIR)]))

(def fs (js/require "fs"))

(def bitcoin (js/require "bitcoinjs-lib"))

(def crypto (.-crypto bitcoin))
(def Wallet (.-Wallet bitcoin))
(def ncrypto (js/require "crypto"))

(defn- load-seed [location]
  (try
    (.readFileSync fs location)
    (catch js/Error e
      (let [new-seed (.randomBytes ncrypto 256)]
        (.writeFileSync fs location new-seed)
        new-seed))))

(defn load-wallet
  ([ ]
    (load-wallet (str DIR "seed")))
  ([location]
      (let [seed (load-seed location)
            sha (.sha256 crypto seed)]
        (Wallet. sha)))) ; TODO reference global testnet constant?