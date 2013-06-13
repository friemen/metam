(ns metam.core-test
  (:use [clojure.test]
        [metam.core]))


(declare defaults)

(defmetamodel forms
  (-> (make-hierarchy)
      (derive ::textfield ::widget)
      (derive ::button ::widget))
  {::textfield    {:label [string?]
                   :password [(value-of? true false)]}
   ::button       {:text [string?]}
   ::panel        {:elements [required
                              (type-of? ::widget)
                              #(> (count %) 0)]}}
  #'defaults)


(defmulti defaults
  (fn [me type-keyword attr-keyword] [type-keyword attr-keyword])
  :hierarchy #'forms-hierarchy)

(defmethod defaults :default [me tk ak] nil)
(defmethod defaults [::textfield :password] [me tk ak] true)



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
    (is (= "panel p1 {:elements [button b1 {:text B}, textfield t1 {:label T, :password true}]}"
           (pr-model p)))))


    (do (def wsdl-hierarchy
          (-> (make-hierarchy)
              (derive :metam.core/complextype :metam.core/datatype)
              (derive :metam.core/simpletype :metam.core/datatype)))
        (def wsdl
          {:hierarchy wsdl-hierarchy,
           :types
           {:metam.core/complextype
            {:elements [(type-of? :metam.core/e)]},
            :metam.core/simpletype
            {},
            :metam.core/e
            {:type [required (type-of? :metam.core/datatype)],
             :mult [(value-of? :one :many)]},
            :metam.core/service
            {:operations [(type-of? :metam.core/op)]},
            :metam.core/op
            {:in-elements [(type-of? :metam.core/e)],
             :out-elements [(type-of? :metam.core/e)]}},
           :default-fn-var #'no-defaults})
        (def complextype (metam.core/instance-factory wsdl :metam.core/complextype))
        (def simpletype (metam.core/instance-factory wsdl :metam.core/simpletype))
        (def e (metam.core/instance-factory wsdl :metam.core/e))
        (def service (metam.core/instance-factory wsdl :metam.core/service))
        (def op (metam.core/instance-factory wsdl :metam.core/op)))
