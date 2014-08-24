(ns bitsplit-cli.client.transactions
  (:require [bitsplit.utils.calculate :refer (apply-diff)]))

(def bitcoin (js/require "bitcoinjs-lib"))

(def Transaction (.-Transaction bitcoin))

(defn- new-txs []
  (cons (Transaction.) (lazy-seq (new-txs))))

(defn make-txs [addresses]
  (zipmap addresses (new-txs)))

(defn add-inputs! [tx unspents]
  (doseq [{:keys [tx-hash index]} unspents]
    (println tx-hash index unspents)
    (.addInput tx tx-hash index))
  tx)

(def with-inputs! (partial merge-with add-inputs!))

(def ^:private tx-fee 10000)

(defn- output? [send-to]
  (->> send-to
    (map second)
    (remove #{0})
    (empty?)
    (not)))

(defn- add-output! [tx send-to]
  (when (output? send-to)
    (doseq [with-fee [(apply-diff (- tx-fee) send-to)]
            [address amount] with-fee]
      (.addOutput tx address amount))
    tx))

(def with-outputs! (partial merge-with add-output!))

(defn- sign! [tx private-key]
  (loop [inputs (.-ins tx)
         i 0]
    (if (first inputs)
      (do
        (.sign tx i private-key)
        (recur (rest inputs) (inc i)))
      tx)))

(def with-signatures! (partial merge-with sign!))
