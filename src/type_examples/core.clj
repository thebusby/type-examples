(ns type-examples.core
  "Examples of core.typed, compiled from;
   1. Much help from the esteemed Ambrose of core.typed fame.
   2. https://github.com/clojure/core.typed/wiki/Quick-Guide
   3. http://www.clojure.net/2013/03/14/Typed-Clojure/   
   Much thanks to both authors!"
  (:require [clojure.core.typed :as t])
  (:use [datomic.api :only [db q] :as d])
  (:import (clojure.lang Seqable IPersistentVector Symbol Keyword ISeq Indexed)))


;; Note: To see, or confirm, what the type signature of a form is;
;; ---------------------------------------------------------------
;; (t/cf (fn [] 5)) 
;; => [(Fn [-> (Value 5)]) {:then tt, :else ff}]
;;
;; (t/cf [1 2 3] (clojure.lang.Seqable Number))
;; => (Seqable Number)


;; Annotate the type for a simple var
;; ----------------------------------
(t/ann x-num Long)
(def x-num 10)


;; Now for a function that takes two 'Number's (Long, Integer, etc), and returns a single Number
;; ---------------------------------------------------------------------------------------------
(t/ann add [Number Number -> Number])
(defn add [a b]
  (+ a b))
(t/ann add-test Number)
(def add-test (add 1 2))
(t/ann add-test-two Number)
(def add-test-two (add 1.0 2))


;; This is what a function of no arguments looks like.
;; Notice that even though x is already annotated above we have to provide the 
;; signature for the function, but at least it'll throw an error if the 
;; signatures don't match!
;; -----------------------
(t/ann get-x-num [-> Number])
(defn get-x-num [] x-num)
(t/ann get-x-num-test Number)
(def get-x-num-test (get-x-num))


;; Don't forget that Clojure returns nil if no return value is provided
;; --------------------------------------------------------------------
(t/ann foo [String -> nil])
(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))


;; A function of no arguments, and no return values (besides nil)
;; --------------------------------------------------------------
(t/ann foo-case [-> nil])
(defn foo-case [] (foo "foo "))


;; Handle a seq of an unknown type
;; -------------------------------
(t/ann get-first-by-first (All [x] (Fn [nil -> nil] ;; Handle the case of nil
                                       [(I (clojure.lang.Seqable x) (ExactCount 0)) -> nil] ;; Handle the case of a vector of zero elements
                                       [(I (clojure.lang.Seqable x) (CountRange 1)) -> x] ;; Handle the case of a vector of one or more elements
                                       [(t/Option (clojure.lang.Seqable x)) -> (t/Option x)] ;; Handle the case of a seq where ExactCount/CountRange isn't going to work. *NOTE* this means we'll return (U nil x) instead of x or nil!!!
                                       )))
(defn get-first-by-first [s]
  (first s))
(t/ann get-first-by-first-test Number)
(def get-first-by-first-test (get-first-by-first [1]))
(t/ann get-first-by-first-test-two Number)
(def get-first-by-first-test-two (get-first-by-first [1 2 3]))
(t/ann get-first-by-first-test-three nil)
(def get-first-by-first-test-three (get-first-by-first []))
(t/ann get-first-by-first-test-four nil)
(def get-first-by-first-test-four (get-first-by-first nil))
(t/ann get-first-by-first-test-five (U Number Keyword String))
(def get-first-by-first-test-five (get-first-by-first [1 :two "three"]))

;; map will return LazySeq which doesn't have a size, so we'll get back (t/Option Number)
(t/ann get-first-by-first-test-six (t/Option Number)) 
(def get-first-by-first-test-six (get-first-by-first (map inc [1 2 3])))

;; As we know this can't return nil, let's help the type system understand that.
(t/ann never-nil (All [x] [(U x nil) -> x]))
(defn never-nil [a]
  (assert (not (nil? a)) "Found nil where it shouldn't be possible")
  a)
(t/ann get-first-by-first-test-seven Number) ;; Having removed the potential nil, now we're back to just a Number
(def get-first-by-first-test-seven (never-nil (get-first-by-first (map inc [1 2 3]))))

;; Note: (assert (seq (map inc [1 2 3]))) would add (CountRange 1) to the type
;; (t/cf (let [s (map inc [1 2 3]) _ (assert (seq s))] s)) 
;; => (I (LazySeq t/AnyInteger) (CountRange 1))

(t/ann get-first-by-first-test-eight (t/Option Number))
(def get-first-by-first-test-eight (get-first-by-first (mapv inc [1 2 3])))
(t/ann get-first-by-first-test-nine (t/Option Number))
(def get-first-by-first-test-nine (get-first-by-first '(1 2 3)))

;; Not sure how to make this work...
;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
;; (t/ann get-first-by-first-test-ten [Number -> (clojure.lang.LazySeq Number)])
;; (defn get-first-by-first-test-ten [x] (get-first-by-first [x 2 3]))



;; Handle a seq full of something unknown, by destructuring...
;; *NOTE* Shouldn't this have the same signature as get-first-by-first?
;; -------------------------------------------------------------
;; Unfortunately none of the following works
;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
;; (t/ann get-first-by-destruct (All [x] (Fn [nil -> nil] ;; Handle the case of nil
;;                                           [(I (clojure.lang.Seqable x) (ExactCount 0)) -> nil] ;; Handle the case of a vector of zero elements
;;                                           [(I (clojure.lang.Seqable x) (CountRange 1)) -> x] ;; Handle the case of a vector of one or more elements
;;                                           [(t/Option (clojure.lang.Seqable x)) -> (t/Option x)] ;; Handle the case of a LazySeq, or an uncountable sequence; unfortunately this doesn't seem to work...
;;                                           )))
;; (defn get-first-by-destruct [s]
;;   (let [[x & xs] s]
;;     x))
;; (t/ann get-first-by-destruct-test Number)
;; (def get-first-by-destruct-test (get-first-by-destruct [1]))
;; (t/ann get-first-by-destruct-test-two Number)
;; (def get-first-by-destruct-test-two (get-first-by-destruct [1 2 3]))
;; (t/ann get-first-by-destruct-test-three nil)
;; (def get-first-by-destruct-test-three (get-first-by-destruct []))
;; (t/ann get-first-by-destruct-test-four nil)
;; (def get-first-by-destruct-test-four (get-first-by-destruct nil))
;; (t/ann get-first-by-destruct-test-five (U Number Keyword String))
;; (def get-first-by-destruct-test-five (get-first-by-destruct [1 :two "three"]))
;; (t/ann get-first-by-destruct-test-six Number)
;; (def get-first-by-destruct-test-six (get-first-by-destruct (map inc [1 2 3])))
;; (t/ann get-first-by-destruct-test-seven Number)
;; (def get-first-by-destruct-test-seven (get-first-by-destruct (mapv inc [1 2 3])))
;; (t/ann get-first-by-destruct-test-eight Number)
;; (def get-first-by-destruct-test-eight (get-first-by-destruct [1 :two "three"]))
;; (t/ann get-first-by-destruct-test-nine Number)
;; (def get-first-by-destruct-test-nine (get-first-by-destruct '(1 2 3)))


;; Handle a vector, and return the count of it's elements
;; ------------------------------------------------------
(t/ann count-seq (Fn [nil -> Number]
                     [(Seqable Any) -> Number]))
(defn count-seq [v]
  (count v))
(t/ann count-seq-test Number)
(def count-seq-test (count-seq [1]))
(t/ann count-seq-test-two Number)
(def count-seq-test-two (count-seq [1 2 3]))
(t/ann count-seq-test-three Number)
(def count-seq-test-three (count-seq nil))
(t/ann count-seq-test-four Number)
(def count-seq-test-four (count-seq '(1 2 3 4)))
(t/ann count-seq-test-five Number)
(def count-seq-test-five (count-seq (map inc [1 2 3])))

;; Now, if you want to ensure that nil returns zero...
(t/ann count-seq-two (Fn [nil -> (Value 0)]
                         [(Seqable Any) -> Number]))
(defn count-seq-two [v]
  (if v
    (count v)
    0))
(t/ann count-seq-two-test Number)
(def count-seq-two-test (count-seq-two [1]))
(t/ann count-seq-two-test-two Number)
(def count-seq-two-test-two (count-seq-two [1 2 3]))
(t/ann count-seq-two-test-three (Value 0))
(def count-seq-two-test-three (count-seq-two nil))
(t/ann count-seq-two-test-four Number)
(def count-seq-two-test-four (count-seq-two '(1 2 3 4)))
(t/ann count-seq-two-test-five Number)
(def count-seq-two-test-five (count-seq-two (map inc [1 2 3])))

;; Or update the signature for clojure.core.RT/count so it's more accurate
;; -----------------------------------------------------------------------
;; You might expect this to work, but it doesn't
;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
;; (t/ann clojure.lang.RT/count (Fn [nil -> (Value 0)]
;;                                  [(U clojure.lang.Counted (Seqable Any)) -> Number]))
;; (t/ann count-seq-three (Fn [nil -> (Value 0)]
;;                            [(Seqable Any) -> Number]))
;; (defn count-seq-three [v]
;;   (count v))
;; (t/ann count-seq-three-test Number)
;; (def count-seq-three-test (count-seq-three [1]))
;; (t/ann count-seq-three-test-two Number)
;; (def count-seq-three-test-two (count-seq-three [1 2 3]))
;; (t/ann count-seq-three-test-three (Value 0))
;; (def count-seq-three-test-three (count-seq-three nil))
;; (t/ann count-seq-three-test-four Number)
;; (def count-seq-three-test-four (count-seq-three '(1 2 3 4)))
;; (t/ann count-seq-three-test-five Number)
;; (def count-seq-three-test-five (count-seq-three (map inc [1 2 3])))


;; Use HMap to indicate Clojure map types
;; --------------------------------------
(t/ann handle-hmap (Fn ['{} -> '{:foo String}]
;;                       [(clojure.lang.APersistentMap clojure.lang.Keyword Any) -> '{:foo String}] ;; To support zipmap, but this doesn't work...
                       ))
;; or (t/ann handle-hmap [(HMap) -> (HMap :mandatory {:foo String})])
(defn handle-hmap [map-arg]
  (assoc map-arg :foo "bar"))
(t/ann handle-hmap-test '{:foo String})
(def handle-hmap-test (handle-hmap {}))
(t/ann handle-hmap-test-two '{:foo String})
(def handle-hmap-test-two (handle-hmap {:foo 3.14}))
(t/ann handle-hmap-test-three '{:foo String})
(def handle-hmap-test-three (handle-hmap {:a 1 :b 2 :c 3}))


;; Unfortunately not all maps can be HMaps
;; ---------------------------------------
(t/ann handle-map [(clojure.lang.IPersistentMap Keyword Any) -> (clojure.lang.IPersistentMap Keyword Any)])
(defn handle-map [m]
  (reduce (t/ann-form #(assoc %1 %2 :win) 
                      [(clojure.lang.IPersistentMap Keyword Any) clojure.lang.Keyword 
                       -> 
                       (clojure.lang.IPersistentMap Keyword Any)]) 
          m 
          [:a :b :c]))
(t/ann handle-map-test (clojure.lang.IPersistentMap Keyword Any))
(def handle-map-test (handle-map {}))
(t/ann handle-map-test-two (clojure.lang.IPersistentMap Keyword Any))
(def handle-map-test-two (handle-map {:a 3.14}))

;; Note zipmap returns an APersistentMap, so it can't be used with an HMap
(t/ann handle-map-test-three (clojure.lang.IPersistentMap Keyword Any))
(def handle-map-test-three (handle-map (zipmap [:x :y :z] [1 2 3])))


;; How to handle a function as an argument
;; ---------------------------------------
(t/ann my-fn-handler [(Fn [Number -> Number]) -> Number])
(defn my-fn-handler [f]
  (f 5))
(t/ann my-fn-handler-test Number)
(def my-fn-handler-test (my-fn-handler inc))
(t/ann my-fn-handler-test-two Number)
(def my-fn-handler-test-two (my-fn-handler (t/ann-form #(+ 3 %)
                                                       [Number -> Number])))
;; Another example
(t/ann my-filter (All [x] [(Fn [x -> Any]) (Seqable x) -> (Seqable x)]))
(defn my-filter [f s]
  (filter f s))
(t/ann my-filter-test (Seqable Number))
(def my-filter-test (filter even? (range 10)))


;; This is what an error looks like
;; --------------------------------
;; (t/ann error-case [ -> (HMap)])
;; (defn error-case [] (handle-hmap 1))



;; How you might wrap a third party library
;; ----------------------------------------
(t/ann conn-uri String)
(def conn-uri "datomic:mem://in-mem-db")

;; Why does this still return a warning?
(t/ann datomic.api/delete-database [String -> Boolean])
(t/ann datomic.api/create-database [String -> Boolean])


;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
;; How would the following work with opaque types?
;; I've replaced the actual types with Any below, 
;; because the actual types as indicated by (class conn)
;; don't appear to work with core.typed's annotations.
;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
(t/ann datomic.api/connect [String -> Any]) ;; This should really be datomic.peer.LocalConnection instead of Any
(t/ann conn Any) ;; This should really be datomic.peer.LocalConnection instead of Any
(def conn (do
            (d/delete-database conn-uri)
            (d/create-database conn-uri)
            (d/connect conn-uri)))

(t/ann datomic.api/transact [Any (Seqable Any) -> Any]) ;; should return a future instead of Any

(t/ann transact-one Any) ;; This should be a future instead of Any
(def transact-one (d/transact conn [{:db/id                 #db/id[:db.part/db]
                                     :db/ident              :foo/name
                                     :db/valueType          :db.type/string
                                     :db/cardinality        :db.cardinality/one
                                     :db.install/_attribute :db.part/db}]))

(t/ann transact-two Any) ;; This should be a future instead of Any
(def transact-two (d/transact conn [{:db/id #db/id[:db.part/user] :foo/name "bar"}]))


(t/ann datomic.api/q [(Seqable Any) Any -> java.util.HashSet]) ;; How do I indicated this is a HashSet of clojure.lang.PersistentVector's ?
(t/ann datomic.api/db [Any -> Any]) ;; This should be datomic.peer.LocalConnection and datomic.db.Db instead of Any
(t/ann query-result java.util.HashSet)
(def query-result (q '[:find ?e ?name 
                       :where [?e :foo/name ?name]] 
                     (db conn)))


;; Is there a way to get the following to work?
;; Note: query-result is a java.util.HashSet
;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
;; (t/ann query-result-rec (t/Option (clojure.lang.PersistentVector Any)))
;; (def query-result-rec (first (seq query-result)))
