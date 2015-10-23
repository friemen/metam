(ns metam.core-test
  (:require [clojure.test :refer :all]
            [metam.core :refer :all]))


(declare defaults)

(defmetamodel forms
  (-> (make-hierarchy)
      (derive ::textfield ::widget)
      (derive ::button ::widget))
  {::widget       {:disabled [boolean?]}
   ::textfield    {:label [string?]
                   :password [(value-of true false)]}
   ::button       {:text [string?]
                   :disabled []}
   ::panel        {:elements [required
                              (coll (type-of ::widget))
                              #(> (count %) 0)]}}
  #'defaults)

(defdefaults defaults forms
  {:default nil
   [::textfield :password] true
   [::widget :disabled]    false})


(deftest valid-panel-test
  (let [p (panel "p1" :elements
                 [(button "b1" :text "B")
                  (textfield "t1" :label "T")])]
    (are [result ks] (= result (get-in p ks))
         "p1" [:name]
         "b1" [:elements 0 :name]
         "B"  [:elements 0 :text]
         "t1" [:elements 1 :name]
         "T"  [:elements 1 :label])))


(deftest invalid-models-test
  (are [model] (thrown? IllegalArgumentException model)
    (textfield "t1" :label "T" :password "")
    (textfield "t1" :label "T" :disabled "foo")
    (panel "p1")
    (panel "p1" :children [])
    (panel "p1" :elements [])
    (panel "p1" :elements ["foo"])
    (panel "p1" :elements
           [(button "b1" :text 1)])))


(deftest defaults-test
  (let [t1 (textfield "t1" :label "T1")
        t2 (textfield "t2" :label "T2" :password false)
        t3 (textfield "t3" :label "T3" :password true)]
    (are [r t] (= r (:password t))
         true t1
         false t2
         true t3)))


(deftest pr-model-test
  (let [p (panel "p1" :elements
                 [(button "b1" :text "B")
                  (textfield "t1" :label "T")])]
    (is (= '(panel "p1"
                   :elements [(button "b1" :text "B" :disabled false)
                              (textfield "t1" :label "T" :disabled false :password true)])
           (pr-model p)))))


(deftest predicates-test
  (is ((coll string?) ["1" "2"]))
  (is (not ((coll string?) [1 2])))
  (is ((type-of ::widget) (button "b1" :text "B")))
  (is (not ((type-of ::widget) (panel "p" :elements [(button "b1" :text "B")]))))
  (is ((coll keyword? fn?) {:id identity :map map}))
  (is (not ((coll string? number?) {"foo" 42 "bar" "4711"}))))


(deftest boolean?-test
  (are [r x] (= r (boolean? x))
       false nil
       true false
       true true
       false "foo"))


(deftest docstring-test
  (let [docstring (-> (var textfield) meta :doc)]
    (is "Creates a textfield.\n  Valid keys are label, password.")))


(deftest inheritance-test
  (are [model data] (= data (dissoc model :metam.core/meta))
    (textfield "t")
    {:name "t", :disabled false, :password true}

    (textfield "t" :disabled true)
    {:name "t", :disabled true, :password true}

    (button "b" :disabled "no")
    {:name "b" :disabled "no"}))
