(ns specter-tests.core-test
  (:use midje.sweet)
  (:use [specter-tests.core]
        [com.rpl.specter]))

(fact "I can run Midje tests in proto-REPL"
  (+ 1 2) => 3)

(def deep-tree
  [1 [2 3] [4 [ ] 5 [6 [7] 8] 9 [[[10 11] 12] 13]]]
  )

(facts "about tree-modification in Specter"

  (fact "I can use a walker to visit nodes of a tree"
    (into [] (traverse (walker integer?) deep-tree)) =>
    [1 2 3 4 5 6 7 8 9 10 11 12 13]
    )

  (fact "I can modify specified nodes of a tree in place"
    (transform
      [(putval 111) (walker #(and (integer? %) (even? %)))]
      *
      deep-tree) =>
    [1 [222 3] [444 [] 5 [666 [7] 888] 9 [[[1110 11] 1332] 13]]]
    )

  (fact "I can replace randomly-specified nodes of a tree"
    (transform
      [(walker #(and (integer? %) (< (rand) 0.3)))]
      (constantly 99)
      deep-tree) => [99 [2 3] [99 [] 5 [6 [99] 8] 9 [[[99 11] 12] 99]]]
      ;;             1   2 3    4    5  6   7  8  9    10 11  12  13
      (provided (rand) =streams=> (cycle [0.1 0.9 0.9]))
    )

  (fact "I can also change the items by jamming in values"
    (setval
      [(walker #(and (integer? %) (< (rand) 0.3)))]
      99
      deep-tree) => [99 [2 3] [99 [] 5 [6 [99] 8] 9 [[[99 11] 12] 99]]]
      ;;             1   2 3    4    5  6   7  8  9    10 11  12  13
      (provided (rand) =streams=> (cycle [0.1 0.9 0.9]))
    )

  (fact "I can change the FIRST of those if I want"
    (transform
      [FIRST (walker #(and (integer? %) (< (rand) 0.3)))]
      (constantly 1111)
      deep-tree) => [1111 [2 3] [4 [] 5 [6 [7] 8] 9 [[[10 11] 12] 13]]]
      ;;             1     2 3   4    5  6  7  8  9    10 11  12  13
      (provided (rand) =streams=> (cycle [0.1 0.9 0.9]))
    )
    )


  (fact "I can change the FIRST of those if I want"
    (transform
      [FIRST (walker integer?)]
      (constantly 1111)
      deep-tree) => [1111 [2 3] [4 [] 5 [6 [7] 8] 9 [[[10 11] 12] 13]]]
      ;;             1     2 3   4    5  6  7  8  9    10 11  12  13
    )


(def TREE-VALUES
  (recursive-path [] p
    (if-path vector?
      [ALL p]
      STAY)))

(defn mutate-one-value
  [changeable-values]
  (assoc
    changeable-values                       ;; a flat collection, here
    (rand-int (count changeable-values))    ;; index of an item in the collection
    1111))                                  ;; new value assigned to that index

(fact "trying Marz's solution"
  (transform
    (subselect TREE-VALUES) ;; gather all the values currently selected by TREE-VALUES
    mutate-one-value
    deep-tree) => [1 [2 3] [4 [] 1111 [6 [7] 8] 9 [[[10 11] 12] 13]]]
    (provided (rand-int 13) => 4) ;; the "random item" will always be item #4
    )
