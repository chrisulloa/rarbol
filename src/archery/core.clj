(ns archery.core
  (:require [archery.shape :refer [shape rectangle-shape ->Point ->Rectangle rtree]]
            [archery.visitor :refer [shapes-collector insert]]
            [clojure.core.protocols :refer [datafy]]
            [clojure.pprint :refer [pprint]])
  (:import (com.github.davidmoten.rtree RTree)
           (com.github.davidmoten.rtree.geometry Geometries))
  (:gen-class))

(defn shape->vega-data
  [shape]
  {:x1 (first (first shape))
   :y1 (first (second shape))
   :x2 (second (first shape))
   :y2 (second (second shape))})

(defn export-as-vega-lite
  [tree]
  (let [{:keys [nodes rectangles points]} (shapes-collector (:root tree))]
    {:background "white",
     :config {:gridOpacity 0}
     :width 1000
     :height 1000
     :layer [{:data {:values (->> nodes
                                  (map shape)
                                  (mapv shape->vega-data))}
              :mark "rect"
              :encoding {:x {:field "x1", :type "quantitative"},
                         :x2 {:field "x2"},
                         :y {:field "y1", :type "quantitative"},
                         :y2 {:field "y2"},
                         :opacity {:value 0.5}
                         :stroke {:value "#023858"}}}
             {:data {:values (->> points
                                  (map rectangle-shape)
                                  (mapv shape->vega-data))}
              :mark "circle"
              :encoding {:color {:value "#3690c0"}
                         :x {:field "x1", :type "quantitative"},
                         :x2 {:field "x2"},
                         :y {:field "y1", :type "quantitative"},
                         :y2 {:field "y2"}}}
             {:data {:values (->> rectangles
                                  (map shape)
                                  (mapv shape->vega-data))}
              :mark "rect"
              :encoding {:x {:field "x1", :type "quantitative"},
                         :x2 {:field "x2"},
                         :y {:field "y1", :type "quantitative"},
                         :y2 {:field "y2"},
                         :stroke {:value "#74a9cf"}
                         :opacity {:value 0.8}}}]}))

(defn -main []
  (let [random-shapes (fn [] (let [min-x (rand-int 500000)
                                   max-x (+ min-x (rand-int 100000))
                                   min-y (rand-int 500000)
                                   max-y (+ min-y (rand-int 100000))]
                               [min-x min-y max-x max-y]))
        sample (take 5000 (repeatedly random-shapes))
        small-sample (take 20 (repeatedly random-shapes))
        create-rectangle (fn [[x1 y1 x2 y2]]
                           (->Rectangle x1 y1 x2 y2))
        create-java-rectangle (fn [[x-min y-min x-max y-max]]
                                (Geometries/rectangle (double x-min)
                                                      (double y-min)
                                                      (double x-max)
                                                      (double y-max)))]
    (println "Example Clojure RTree")
    (pprint (datafy (insert (rtree) (map create-rectangle small-sample))))
    (println "\nExample Java RTree")
    (println (.asString (reduce #(.add %1 nil %2) (RTree/create) (map create-java-rectangle small-sample))))
    (println "Starting benchmark: Inserting 5,000 rectangles.")
    (time
      (do
        (dotimes [n 25]
          (println (format "Clojure RTree Iteration %s" n))
          (time (insert (rtree) (map create-rectangle sample))))
        (println "For all runs:")))
    (time
      (do
        (dotimes [n 25]
          (println (format "Java RTree Iteration %s" n))
          (time
            (reduce #(.add %1 nil %2) (RTree/create) (map create-java-rectangle sample))))
        (println "For all runs:")))))
