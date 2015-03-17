(ns clojure-benchmarking.quasi-descent-power
  (:require [manifold.stream :as s]))

(use 'clojure.pprint)
(use '(incanter core charts))

(def rho 0.05)
(def epsilon 0.0001)

(def state-history (atom []))
(def iterations (atom 0))

(defn dot-mult
  [m v] 
  (mapv #(* % v) m))

(defn ebe-add 
  [m1 m2] 
  (mapv + m1 m2))

(defn my-x 
  [agent]
  (get (:x agent) (:id agent)))

(defn objective-function 
  [agent] 
  (pow (- (my-x agent) (:x-spec agent)) 2))
  
(defn del-objective-function
  [agent]
  (* 2 (- (my-x agent) (:x-spec agent)) (:alpha agent)))

(defn- positive 
  [num] 
  (if (> num 0) num 0))

(defn global-constraint
  [agents]
  (let [states (mapv (fn [agent] (my-x agent)) agents)] 
      (concat [(- (reduce + states) (reduce + (map :x-spec agents)))]
              (mapv (fn [agent] (positive (- (my-x agent) (:x-max agent)))) agents))))

(defn mu-step 
  [agents mu]
  (ebe-add mu (dot-mult (global-constraint agents) rho)))

(defn state-step
  [agent]
  (- (my-x agent)
     (* rho (+ (del-objective-function agent) 
               (+ (nth (:mu agent) (inc (:id agent))) (first (:mu agent)))))));due to sympilicity of global constraint, del-global-constraint
                                                                              ;just causes certain elements of mu to persist (just index into mu instead)
(defn agent-fn
  [agent state-vec mu]
  (let [updated (assoc agent :mu mu :x state-vec)]
    (assoc-in updated [:x (:id updated)](state-step updated))))

(defn within-epsilon? [agent]
  (let [val (+ (del-objective-function agent) 
               (+ (nth (:mu agent) (inc (:id agent))) 
                  (first (:mu agent))))]
    (println "E: " epsilon)
    (< val epsilon)))

(defn step? [agents]
  (some false? (map within-epsilon? agents)))

(defn cloud-fn
  [agents]     
  (let [states (mapv (fn [agent] (my-x agent)) agents)]   
    (println "cloud iterating... states: " states)
    (swap! state-history conj states)
    (swap! iterations inc)
    [states (mu-step agents (:mu (first agents))) (step? agents)]))

(defn produce-plot [num-agents]
  (let [plot (xy-plot
               (range 0 @iterations)
               (map (fn [state-vec] (get state-vec 0)) @state-history)
               :series-label  "Agent-0"
               :points true 
               :legend true 
               :x-label "Iterations"
               :y-label "Power (W)")]
    (doseq [num (range 1 num-agents)]
      (add-lines plot 
                (range 0 @iterations) 
                (map 
                  (fn [state-vec] (get state-vec num)) 
                  @state-history) 
                :series-label (str "Agent-" num)
                :points true))
  (view plot)))
      
      
      
      
      
      
      
