(ns metam.core-test
  (:require [clojure.test :refer :all]
            [metam.core :refer :all]))


(declare defaults)

(defmetamodel forms
  (-> (make-hierarchy)
      (derive ::textfield ::widget)
      (derive ::button ::widget))
  {::textfield    {:label [string?]
                   :password [(value-of true false)]}
   ::button       {:text [string?]}
   ::panel        {:elements [required
                              (coll (type-of ::widget))
                              #(> (count %) 0)]}}
  #'defaults)

(defdefaults defaults forms
  {:default nil
   [::textfield :password] true})


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
                   :elements [(button "b1" :text "B")
                              (textfield "t1" :label "T", :password true)])
           (pr-model p)))))


(deftest predicates-test
  (is ((coll string?) ["1" "2"]))
  (is (not ((coll string?) [1 2])))
  (is ((type-of ::widget) (button "b1" :text "B")))
  (is (not ((type-of ::widget) (panel "p" :elements [(button "b1" :text "B")])))))


(deftest boolean?-test
  (are [r x] (= r (boolean? x))
       false nil
       true false
       true true
       false "foo"))
