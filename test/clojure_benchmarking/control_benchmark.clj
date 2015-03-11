(ns clojure-benchmarking.control-benchmark  
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
    (helper (:methods (-> func d/disassemble-data)))))

(defn bytecode-rate 
  [benchmark-fn] 
  (/ (bytecode-count benchmark-fn) (first (:sample-mean (c/quick-benchmark (benchmark-fn) {:verbose true})))))

(defn random-bytes 
  [n] 
  (loop [i n ret []]
    (if (> i 0)
      (recur (dec i) (conj ret (gen-benchmark)))
      ret)))

(defn approximate-bytecode 
  []
  (let [fns (exper)
        
        counts (mapv bytecode-count fns)
        
        results (mapv bytecode-rate fns)]
    
    (def one counts)
    (def two results)
    (spit "counts.edn" counts)
    (spit "bps.edn" results)))


  (approximate-bytecode)
  (println "AVERAGE BYTECODE RATE (Bps): " (/ (reduce + two) (count two))))




