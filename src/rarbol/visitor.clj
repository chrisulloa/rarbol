(ns rarbol.visitor
  (:require [rarbol.zipper :refer [tree-visitor zipper]]
            [rarbol.shape :refer [enlargement
                                  area
                                  envelops?
                                  intersects?
                                  minimum-bounding-rectangle]]
            [rarbol.util :refer [fast-contains?]]))

(defn leaf-visitor
  "Visitor that collects all leaf nodes."
  [node state]
  (when (:leaf? node)
    {:state (conj state node)}))

(defn node-contains-shape-visitor
  "Visitor that returns node which contains shape."
  [shape]
  (fn [node state]
    (if (envelops? node shape)
      (when (and (:leaf? node)
                 (fast-contains? (:values node) shape))
        {:state (conj state node)
         :stop  true})
      {:next true})))

(defn enveloped-shapes-visitor
  "Visitor that returns shapes enveloped by given rectangle."
  [rectangle]
  (fn [node state]
    (if (or (envelops? node rectangle)
            (intersects? node rectangle))
      (when (:leaf? node)
        (->> node
             :values
             (filter #(envelops? rectangle %))
             (concat state)
             (hash-map :state)))
      {:next true})))

(defn insertion-visitor
  "Visitor that returns the best node for insertion of given shape."
  [shape]
  (fn [node state]
    (when (:leaf? node)
      {:state node
       :stop  true}
      (if (and (not (empty? state))
               (<= (enlargement shape state) (enlargement shape node)))
        {:next true}
        {:state node}))))

(defn leaf-collector
  "Collect all leaf nodes."
  [node]
  (:state (tree-visitor (zipper node) [leaf-visitor])))

(defn node-contains-shape-finder
  "Finds first node that contains the shape."
  [node shape]
  (first
    (:state
      (tree-visitor
        (zipper node) [(node-contains-shape-visitor shape)]))))

(defn enveloped-shapes-collector
  "Find entries which are enveloped by given rectangle."
  [node rectangle]
  (:state
    (tree-visitor
      (zipper node) [(enveloped-shapes-visitor rectangle)])))

(defn insertion-finder
  "Finds node that is best suited for insertion of shape."
  [node shape]
  (:state (tree-visitor (zipper node) [(insertion-visitor shape)])))
