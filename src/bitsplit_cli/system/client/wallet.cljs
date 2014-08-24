(ns bitplit-cli.system.client.wallet
  (:require [bitsplit-cli.utils.constants :refer (base-directory)]
            [bitsplit-cli.utils.storage :refer (read-file write-file)]))

(def bitcoin (js/require "bitcoinjs-lib"))

(def crypto (.-crypto bitcoin))
(def Wallet (.-Wallet bitcoin))
(def ncrypto (js/require "crypto"))

(defn- load-wallet-file [location]
  (.split
      (read-file location
        ; if nothing found
        #(let [new-seed (.randomBytes ncrypto 256)
              output (str new-seed \newline 0)]
          (write-file location output)
          output))
    "\n"))

(defn load-wallet
  ([ ]
    (load-wallet (str base-directory "seed")))
  ([location]
      (let [[seed i] (load-wallet-file location)
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
        current (read-file location)
        [seed i] (-> current (str) (.split "\n"))]
    (write-file location
      (str seed \newline (inc* i)))
    address))