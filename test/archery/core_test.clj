(ns archery.core-test
  (:require [clojure.test :refer :all]
            [archery.core :refer :all]
            [archery.shape :refer :all]
            [archery.util :refer :all]
            [archery.visitor :refer :all]))

(deftest test-fast-contains?
  (testing "Fast contains function."
    (is (= true (fast-contains? [10 15 30 5] 30)))))

(deftest test-visitors
  (testing "Visitor functions."
    (let [child1 (->RectangleNode true
                                  [(->Point [1 1]) (->Point [5 5])
                                   (map->Rectangle {:shape [[10 15] [10 15]]})]
                                  [[0 50] [0 50]])
          child2 (->RectangleNode true
                                  [(->Point [60 60])
                                   (->Rectangle [[55 60] [55 60]])]
                                  [[0 60] [0 60]])
          root (->RectangleNode false [child1 child2] [[0 50] [0 50]])]
      (is (= #{child1 child2} (set (leaf-collector root))))
      (is (= child1 (node-contains-shape-finder root (->Point [1 1]))))
      (is (= child2 (node-contains-shape-finder root (->Rectangle [[55 60] [55 60]]))))
      (is (= nil (node-contains-shape-finder root (->Point [300 300]))))
      (is (= #{(->Point [1 1]) (->Point [5 5]) (->Rectangle [[10 15] [10 15]])}
             (set (enveloped-shapes-collector root (->Rectangle [[0 20] [0 20]]))))))))

(deftest test-minimum-bounding-rectangle
  (testing "Minimum bounding rectangle."
    (is (= [[-10 55] [-15 60]]
           (minimum-bounding-rectangle (->Point [0 0])
                                       (->Point [55 60])
                                       (->Point [-10 -15]))))
    (is (= [[-100 100] [-300 300]]
           (minimum-bounding-rectangle (->Point [-100 -300])
                                       (->Point [100 300])
                                       (->Rectangle [[55 60] [25 100]]))))
    (is (= [[100 100] [150 150]]
           (minimum-bounding-rectangle (->Rectangle [[100 100] [150 150]]))))))

(deftest test-intersects?
  (testing "Intersects function."
    (is (= true (intersects? (->Rectangle [[100 150] [25 300]])
                             (->Rectangle [[50 300] [40 500]]))))
    (is (= false (intersects? (->Rectangle [[50 60] [50 60]])
                              (->Rectangle [[10 15] [10 15]]))))
    (is (= true (intersects? (->Rectangle [[100 300] [100 300]])
                             (->Rectangle [[150 250] [150 250]]))))))

(deftest test-envelops?
  (testing "Envelops function."
    (is (= true (envelops? (map->Point {:shape [5 10] :foo ""})
                           (->Point [5 10]))))
    (is (= false (envelops? (->Point [5 10]) (->Point [0 0]))))
    (is (= false (envelops? (->Point [5 10]) (->Rectangle [[5 10] [35 35]]))))
    (is (= true (envelops? (->Rectangle [[5 10] [35 39]]) (->Point [6 38]))))
    (is (= true (envelops? (->Rectangle [[100 300] [100 300]])
                           (->Rectangle [[150 250] [150 250]]))))
    (is (= false (envelops? (->Rectangle [[100 150] [25 300]])
                            (->Rectangle [[50 300] [40 500]]))))))

(deftest test-collect-points
  (testing "Collect points function."
    (is (= [[0] [3]] (collect-points (->Point [0 3]))))
    (is (= [[0 10] [5 15]] (collect-points (->Rectangle [[0 10] [5 15]]))))))

(deftest test-shape->rectangle
  (testing "shape->rectangle"
    (is (= (->Rectangle [[5 10] [5 10]])
           (shape->rectangle (->Rectangle [[5 10] [5 10]]))))
    (is (= (->Rectangle [[5 5] [10 10]])
           (shape->rectangle (->Point [5 10]))))))

(deftest test-augment-shape
  (testing "augment-shape"
    (is (= {0 [10 10], 1 [15 15]}
           (:augmented (augment-shape (->Point [10 15])))))
    (is (= {0 [10 15], 1 [35 40]}
           (:augmented (augment-shape (->Rectangle [[10 15] [35 40]])))))))

(deftest test-augmented-val
  (is (= 10 ((augmented-val second 2)
              (augment-shape (->Rectangle [[10 15] [3 5] [0 10]]))))))

(deftest test-area-enlargement-diff
  (is (= 25 (area-enlargement-diff (->RectangleNode true [] [[0 5] [0 5]]) (->Point [5 10])))))

(deftest test-compress-rectangle
  (is (= [[5 10] [5 10]]
         (shape (compress-node (->RectangleNode true [] [[5 10] [5 10]])))))
  (is (= [[5 5] [5 5]]
         (shape (compress-node (->RectangleNode true [(->Point [5 5])] [[0 0] [0 0]])))))
  (is (= [[5 10] [5 10]]
         (shape (compress-node (->RectangleNode true [] [[5 8] [5 8]])
                                (->Point [5 5])
                                (->Point [5 10])
                                (->Point [10 10]))))))

(deftest test-linear-seeds
  (let [shapes [(->Point [0 0])
                (->Point [10 10])
                (map->Rectangle {:shape [[0 3] [6 8]]})
                (map->Rectangle {:shape [[15 30] [35 55]]})]
        seeds (linear-seeds-across-dimensions shapes)]
    (is (= {:dimension       0
            :seeds           [(->Point [0 0]) (map->Rectangle {:shape [[15 30] [35 55]]})]
            :norm-separation (/ 15 30)}
          (first seeds)))
    (is (= {:dimension       1
            :seeds           [(->Point [0 0]) (map->Rectangle {:shape [[15 30] [35 55]]})]
            :norm-separation (/ 35 55)}
           (second seeds)))
    (is (= (set [[[15 30] [35 55]] [[0 0] [0 0]]])
           (set (map shape (linear-seeds shapes true)))))))

(deftest test-initialize-seed
  (let [point-seed (->Point [15 30])
        rectangle-seed (->Rectangle [[15 30] [35 45]])]
    (is (= (shape (->RectangleNode true [point-seed] [[15 15] [30 30]]))
           (shape (initialize-seed point-seed true))))
    (is (= (shape
             (->RectangleNode true [rectangle-seed] [[15 30] [35 45]]))
           (shape (initialize-seed rectangle-seed true))))))

(deftest test-shape->seed
  (let [l-seed (->RectangleNode true [(->Point [15 30])] [[15 15] [30 30]])
        r-seed (->RectangleNode true [(->Rectangle [[35 55] [80 85]])] [[35 55] [80 85]])
        p (->Point [18 18])
        result (shape->seed p r-seed l-seed)]
    (is (= [[15 18] [18 30]]
           (shape (:enlarged-seed result))))
    (is (= :l-seed (:next-seed result)))))