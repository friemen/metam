(ns metam.textgen
  (:require [clojure.string :as s])
  (:use [metam.core]))

;; M2T infrastructure

(defn- amend?
  "Returns true for expressions that are bare keywords or evaluate to a function."
  [env expr]
  (and (not (list? expr)) ; not a call
       (not (contains? env expr)) ; not a local symbol
       (or (keyword? expr) ; a bare keyword
           (fn? (eval expr))))) ; a bare function

(defmacro gen-with
  "Transforms keywords and bare function symbols to a call to these with
   x as their only argument, e.g. :name becomes (:name x)."
  [x & exprs]
  `(apply str ~(vec (map #(if (amend? &env %) (list % x) %) exprs))))


(defn gen-map
  "Applies the template function to each item of the collection and
   joins the output strings using the given separator string."
  ([template-fn xs]
     (gen-map template-fn xs ""))
  ([template-fn xs sep]
     (s/join sep (map template-fn xs))))


