(ns samples.wsdl.metamodel
  (:require [metam.core :refer :all]))


;; Define metamodel


(defmetamodel wsdl
  (-> (make-hierarchy)
      (derive ::complextype ::datatype)
      (derive ::simpletype ::datatype))
  
  {; a data structure
   ::complextype  {:elements [(coll (type-of ::e))]}
   ; a primitive type (like string, number, date)
   ::simpletype   {}
   ; an element of a complextype
   ::e            {:type [required (type-of ::datatype)]
                   :mult [(value-of :one :many)]}   
   ; a web service                         
   ::service      {:operations [(coll (type-of ::op))]}
   ; a service operation
   ::op           {:in-elements [(coll (type-of ::e))]
                   :out-elements [(coll (type-of ::e))]}
   }
  #'no-defaults)


;; Define shortcut functions

(defn one [name type]
  (e name :type type :mult :one))

(defn many [name type]
  (e name :type type :mult :many))


;; Define useful queries

(defmulti referenced-types metatype :hierarchy #'wsdl-hierarchy)

(defmethod referenced-types ::service [service]
  (->> (:operations service) (mapcat referenced-types) set))

(defmethod referenced-types ::op [op]
  (->> (concat (:in-elements op) (:out-elements op))
                   (mapcat referenced-types)
                   set))

(defmethod referenced-types ::e [e]
  (let [dt (:type e)]
    (into #{dt} (referenced-types dt))))

(defmethod referenced-types ::complextype [ct]
  (->> (:elements ct) (mapcat referenced-types) set))

(defmethod referenced-types :default [x]
  #{})
