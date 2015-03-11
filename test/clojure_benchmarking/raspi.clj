(ns clojure-benchmarking.raspi
  (:require [criterium.core :as c]
            [no.disassemble :as d])
  (:gen-class))

(defn- rand-int+ 
  [n]
  (inc (rand-int n)))

(def ^:private ops [- / * +])

(defn gen-op 
  [n] 
  `~(cons (ops (rand-int 4)) (repeatedly n #(rand-int+ n))))

(defmacro gen-benchmark 
  [] 
  `(do
     (fn []
       (~-         
         ~@(repeatedly (+ 10 (rand-int+ 40)) #(gen-op (rand-int+ 10)))))))

(defn- emitter 
  [] 
  `(gen-benchmark))

(defmacro exper 
  []
  `(do 
     (vector ~@(repeatedly 100 #(emitter)))))
 
(defn -main []
(defn bytecode-count
  [func]  
  (let [helper (fn this
                 [ms] 
                 (if (or (= (:name (first ms)) 'invoke) (empty? ms))
                   (count (:bytecode (:code (first ms))))
                   (recur (rest ms))))]  
    (helper (:methods (d/disassemble-data func)))))

(defn bytecode-rate 
  [benchmark-fn] 
  (/ (bytecode-count benchmark-fn) (first (:sample-mean (c/quick-benchmark (benchmark-fn) {:verbose true})))))

(defn approximate-bytecode
  []
  (loop [counts []
         rates  []
         iterations 100]
    (let [arbitrary-fn #(emitter)
          count (bytecode-count arbitrary-fn)
          rate (bytecode-rate arbitrary-fn)]
      (if (< iterations 1)
        (do
          (spit "counts.edn" counts)
          (spit "bps.edn" rates)
          (println "AVERAGE BYTECODE RATE (Bps): " (/ (reduce + rates) (count rates))))
          
        (recur 
          (conj counts count) 
          (conj rates rate)
          (dec iterations))))))

(approximate-bytecode))