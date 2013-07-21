(ns type-examples.core
  "Examples of core.typed, compiled from;
   1. https://github.com/clojure/core.typed/wiki/Quick-Guide
   2. http://www.clojure.net/2013/03/14/Typed-Clojure/
   Much thanks to both authors!"
  (:require [clojure.core.typed :as t])
  (:import (clojure.lang Seqable IPersistentVector Symbol Keyword ISeq Indexed)))


;; See what the type signature of a form is,
;; (t/cf (fn [] 5)) 
;; => [(Fn [-> (Value 5)]) {:then tt, :else ff}]

;; Annotate the type for a simple var
(t/ann x Long)
(def x 10)

;; Now for a function that takes two numbers (Long, Integer, etc), and returns a single number
(t/ann add [Number Number -> Number])
(defn add [a b]
  (+ a b))
(t/ann add-test Number)
(def add-test (add 1 2))

;; This is what a function of no arguments looks like.
;; Notice that even though x is already annotated we have to provide the 
;; signature for the function, but at least it'll throw an error if the 
;; signatures don't match!
(t/ann get-x [-> Number])
(defn get-x [] x)
(t/ann get-x-test Number)
(def get-x-test (get-x))

;; Don't forget that Clojure returns nil if no return value is provided
(t/ann foo [String -> nil])
(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

;; A function of no arguments and no return values (besides nil)
(t/ann foo-case [-> nil])
(defn foo-case [] (foo "harhar"))

;; Handle a seq full of something unknown, by fn
;; (t/ann get-first-by-first (All [x] [(Seqable x) -> x]))
(t/ann get-first-by-first (All [x] (Fn [(t/Option (I (Seqable x) (ExactCount 0))) -> nil] 
                                       [(I (Seqable x) (CountRange 1)) -> x] 
                                       [(t/Option (Seqable x)) -> (t/Option x)])))
(defn get-first-by-first [s]
  (first s))
;; NOTE: above passes check-ns but doesn't actually work!!!
(t/ann get-first-by-first-test Number)
(def get-first-by-first-test (get-first-by-first [1]))
(t/ann get-first-by-first-test-two Number)
(def get-first-by-first-test-two (get-first-by-first [1 2 3]))
;; The below fails though...
;; (t/ann get-first-by-first-test-three Number)
;; (def get-first-by-first-test-three (get-first-by-first [1 :two "three"]))



;; Handle a seq full of something unknown, by destructuring
;;(t/ann get-first-by-destruct (All [x] [(Vector* x) -> x]))
(t/ann get-first-by-destruct (All [x] 
                                  [(Seqable x) -> (U nil x)]
;;                                  [(U (Indexed x) (Seqable x)) -> (U x nil)]
))
(defn get-first-by-destruct [s]
  (let [[x & xs] s]
    x))
;; NOTE: above passes check-ns but doesn't actually work!!!
;; (t/ann get-first-by-destruct-test Number)
;; (def get-first-by-destruct-test (get-first-by-destruct [1]))

;; (t/ann handle-vec [(Vector* Any) -> Number])
;; (defn handle-vec [v]
;;   (count v))
;; NOTE: above passes check-ns but doesn't actually work!!!
;; (t/ann handle-vec-test Number)
;; (def handle-vec-test (handle-vec [1 2 3]))


;; Use HMap to handle Clojure map types
(t/ann handle-map [(HMap) -> (HMap)])
(defn handle-map [map-arg]
  {:response map-arg})


;; HMap's can define mandatory and optional keys!
(t/ann wrap-response [(HMap :mandatory {:a Number}) -> (HMap)])
(defn wrap-response [resp]
  {:response resp})


(t/ann ok-call [-> nil])
(defn ok-call [] (do (wrap-response {:a 1} ) nil))

;; (t/ann error-call [-> nil])
;; (defn error-call [] (do (wrap-response [{:a 1}]) nil))

;; (t/ann error-call-two [-> nil])
;; (defn error-call-two [] (do (wrap-response {:a "1"}) nil))


(defn compile-time-type-check [a]
  (t/ann-form a Number) ;; Errors at compile time if not number
  (class a))


(comment
;; start comments

;This example demonstrates how to add type information to regular clojure maps
;; (defn purchase-order [id date amount]
;;   ^{:type ::PurchaseOrder} ;metadata
;;    {:id id :date date :amount amount})
 
;; (def my-order (purchase-order 10 (java.util.Date.) 100.0))
;; (my-order)
;; {:id 10, :date #<Date Sun May 15 14:29:19 EDT 2011>, :amount 100.0}
;; (type my-order)




 (t/cf (fn [] [1]))

;; end comments
)


