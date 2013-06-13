(ns samples.wsdl.model
  (:use [samples.wsdl.metamodel]
        [samples.wsdl.generator :only [t-wsdl]]
        [metam.core :only [pr-model]]))


;; Define model

(def string (simpletype "string"))
(def date (simpletype "date"))

(def address
  (complextype "Address" :elements
               [(one "street" string)
                (one "zipcode" string)
                (one "city" string)]))

(def person
  (complextype "Person" :elements
               [(one "firstname" string)
                (one "lastname" string)
                (one "birthday" date)
                (many "addresses" address)]))


(def s
  (service "Bar" :operations
           [(op "callMe1"
                :in-elements [(one "a" address)
                              (one "t" string)]
                :out-elements [(one "s" string)])
            (op "callMe2"
                :in-elements [(one "p" person)])]))


(defn generate []
  (println (t-wsdl s)))
