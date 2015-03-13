(ns clojure-benchmarking.power-optimization
   (:require [watershed.core :as w]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [physicloud.core :as phy]))
(defn -main []
 ;;pull initial state, ip, etc from config file...
 (def properties (load-file "opt-config.clj"))
 
 (if-not properties 
   (println "config file not found")
   (do 
     (def ip (:ip properties))
     (def neighbors (:neighbors properties))))
 
 (defn my-key []
   (keyword (str "agent-" (:agent-num properties))))
  
 (defn provides []
   (if (= (my-key) :agent-1)
     [:cloud :agent-1]
     (vector (my-key))))
 
 (defn requires []
   (if (= (my-key) :agent-1)
     (into [] (map (fn [num] (keyword (str "agent-" num)))(range 2 (inc neighbors))))
     [:cloud]))
  
 (if (= (my-key) :agent-1) 
   ;only make cloud vertex if you are agent 1
   (phy/physicloud-instance 
      {:ip ip
       :neighbors neighbors
       :provides (provides)
       :requires (requires)}
      (w/vertex 
        :cloud 
        (into [] (map (fn [num] (keyword (str "agent-" num)))(range 1 (inc neighbors)))) ;; should return something like: [:agent-1 :agent-2 :agent-3]
        (fn cloud-fn 
          ([] :step)
          ([& streams] 
          (s/map 
            (fn [agent-maps] 
              (println "CLOUD: Here are the agent maps: \n" agent-maps)
              (if (empty? (filter (fn [map] (> (:del-j map) 5)) agent-maps))
                   :kill ;;when they all get less than 5, terminate
                   :step))
            (apply s/zip streams)))))
      ;agent vertex currently faking gradient descent
      (w/vertex 
        (my-key) 
        [(my-key) :cloud] 
        (fn 
          ([] 
            (println "no args received at" (my-key) "vertex function") {:id (my-key) :del-j 10000})
         
          ([my-stream cloud-stream] 
            (s/map 
              (fn [[my-stream-map cloud-stream-msg]] 
                (if (= cloud-stream-msg :step)
                  (do
                    (println "stepping... currently at: " (:del-j my-stream-map))
                    {:id (my-key) :del-j (dec (:del-j my-stream-map))})
                  (do 
                    (println "did not receive step instruction... killing")
                    (s/close! my-stream))))                  
              (apply s/zip [my-stream cloud-stream]))))))
   ;;else...
   (phy/physicloud-instance 
      {:ip ip
       :neighbors neighbors
       :provides (provides)
       :requires (requires)}
     ;agent vertex currently faking gradient descent
      (w/vertex 
        (my-key) 
        [(my-key) :cloud] 
        (fn 
          ([] 
            (println "no args received to" (my-key) "vertex function") {:id (my-key) :del-j 10000})
         
          ([my-stream cloud-stream] 
            (s/map 
              (fn [[my-stream-map cloud-stream-msg]] 
                (if (= cloud-stream-msg :step)
                  (do
                    (println "stepping... currently at: " (:del-j my-stream-map))
                    {:id (my-key) :del-j (dec (:del-j my-stream-map))})
                  (do 
                    (println "did not receive step instruction... killing")
                    (s/close! my-stream))))                  
              (apply s/zip [my-stream cloud-stream]))))))))
     
     
     
     
     
     
     
     
     
     
     
     
     
     

