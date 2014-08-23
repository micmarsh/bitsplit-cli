(ns bitsplit-cli.client.transactions)

(def bitcoin (js/require "bitcoinjs-lib"))

(def Transaction (.-Transaction bitcoin))
(def ECKey (.-ECKey bitcoin))

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

(defn- add-output! [tx send-to]
  (doseq [[address amount] send-to]
    (.addOutput tx address amount))
  tx)

(def with-outputs! (partial merge-with add-output!))

(defn- sign! [tx private-key]
  (loop [inputs (.-ins tx)
         i 0]
    (if (first inputs)
      (do
        (.sign tx i (ECKey. private-key))
        (recur (rest inputs) (inc i)))
      tx)))

(def with-signatures! (partial merge-with sign!))
