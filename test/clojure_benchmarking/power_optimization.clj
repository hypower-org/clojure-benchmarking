(ns clojure-benchmarking.power-optimization
   (:require [watershed.core :as w]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [physicloud.core :as phy]
            [clojure-benchmarking.quasi-descent-power :as q])
   (:gen-class))

 ;;pull initial state, ip, etc from config file...
 (def properties (load-file "opt-config.clj"))
 (def counter (atom 0))
 
 (if-not properties 
   (println "config file not found")
   (do 
     (def ip (:ip properties))
     (def neighbors (:neighbors properties))))
 
                       
 (def init-agent-map {;mu vector length is num-agents + 1
                      :mu (vec (repeat (inc neighbors) 0))
                       ;x is the vector holding this agent's copy of everyone's state
                      :x (assoc (vec (repeat neighbors 0)) (:id properties) (:start-power properties))
                       ;x-spec is the specified power
                      :x-spec (:spec-power properties)
                       ;ids range from 0 ... n where n is the number of agents (agent 0 gets cloud)
                      :id (:id properties)
                      ;max power that this cpu can draw
                      :x-max (:max-power properties)
                      ;weight of the objective function for this cpu 
                      :alpha (:alpha properties)})
                      
 (defn my-key []
   (keyword (str "agent-" (:id properties))))
  
 (defn provides []
   (if (= (my-key) :agent-0)
     [:cloud :agent-0]
     (vector (my-key))))
 
 (defn requires []
   (if (= (my-key) :agent-0)
     (into [] (map (fn [num] (keyword (str "agent-" num)))(range 1  neighbors)))
     (vector :cloud)))
            
 
 (defn -main []
   (let [cloud-vertex (if (= (my-key) :agent-0) 
                        ;only make cloud vertex if you are agent 0
                        (w/vertex 
                          :cloud 
                          (into [] (map (fn [num] (keyword (str "agent-" num)))(range 0  neighbors))) ;; should return something like: [:agent-0 :agent-1 :agent-2]
                          (fn cloud-fn
                            ([] (println "cloud vertex called without args")
                                [nil nil true])
                            ([& streams]
                            (s/map
                              (fn [agent-maps] 
                                (q/cloud-fn agent-maps))
                              (apply s/zip streams))))))
         
         agent-vertex (w/vertex
                        (my-key)
                        [(my-key) :cloud]
                        (fn
                          ;if no args supplied, emit initial agent map
                          ([] init-agent-map)
                          ([my-stream cloud-stream] 
                            (s/map 
                              (fn [zipped-streams] 
                                ;destructure streams 
                                (let [[my-agent-map [states mu step?]] zipped-streams]
                                  
                                  (cond
                                    (not states)  ;;if cloud vertex gets called first, it sends nils in for args so agents can emit their initial maps
                                      (do 
                                        (println "agent-vertex called with nil args from cloud vertex, emitting map...")
                                        init-agent-map)
                                    ;step?
                                    (> @counter 2000)
                                      (do
                                        (println "step instruction not received, stopping...")
                                        (s/close! my-stream))
                                    :else
                                      (do
                                        (println "running...")
                                        (swap! counter inc)
                                        (q/agent-fn my-agent-map states mu))
;                                    :else
;                                      (do
;                                        (println "step instruction not received, stopping...")
;                                        (s/close! my-stream))
                                    )))
                              (apply s/zip [my-stream cloud-stream])))))
                      
;         kill-vertex (w/vertex :kill 
;                                [:cloud [:all :without [:kill]]] 
;                                (fn [cloud-stream & streams] 
;                                  (s/map 
;                                    (fn [[states mu step?]]
;                                      (when-not step?
;                                        (println "Did not receive step instruction from cloud, stopping...")
;                                        (println "final power states..." states)
;                                        (println "plotting algorithm progression...")
;                                        (q/produce-plot neighbors)
;                                        (doseq [s (concat [cloud-stream] streams)]
;                                          (if (s/stream? s)
;                                            (s/close! s)))))
;                                    cloud-stream)))
         ]
     
     (def cloud-v cloud-vertex)
     (def agent-v agent-vertex)
     
     (if cloud-vertex
       ;build cloud vertex if agent-0
        (phy/physicloud-instance 
           {:ip ip
            :neighbors neighbors
            :provides (provides)
            :requires (requires)}
           cloud-vertex
           agent-vertex
           ;kill-vertex
           )
       ;otherwise just build agent and kill vertices
        (phy/physicloud-instance
          {:ip ip
           :neighbors neighbors
           :provides (provides)
           :requires (requires)}
           agent-vertex
          ; kill-vertex
           ))))
      
