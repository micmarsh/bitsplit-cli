(ns bitsplit-cli.client
    (:use
        [cljs.core.async :only (chan put!)]
        [bitsplit.client.protocol :only 
        (Queries addresses unspent-amounts unspent-channel
         Operations send-amounts! new-address!)]))

(defrecord FakeClient [info]
    Queries
    (addresses [this]
        ["dke98di398fdjr98feijr3oifsoij"
         "kdi9d9ekdkjeufjeueudjudd"
         "83dkm3mde030dk3kd3jdmjewkdi"])
    (unspent-amounts [this]
        (vec 
            (map (fn [address amount] {"address" address "amount" amount})
            (addresses this) [0.23 2 1.8])))
    (unspent-channel [this]
        (let [amounts (unspent-amounts this)
              index (atom 0)
              return (chan)]
            (js/setInterval
                (fn []
                    (put! return (amounts @index))
                    (swap! index inc)
                    (swap! index #(mod % 3)))
                1000)))
    Operations
    (send-amounts! [this amounts])
    (new-address! [this]))

