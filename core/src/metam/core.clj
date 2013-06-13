(ns metam.core
  (:require [clojure.string :as str])
  (:use [clojure.set :only [superset?]]))

;; Concepts
;;
;; A model is a model element.
;; A model-element is a map {attr-keyword->model-value}.
;; A model-element map has always a :name key that points to a string.
;; A model-value is either
;;    - a vector containing model-values,
;;    - a Clojure scalar (string, boolean, number), or
;;    - another model element,
;; A model-element-type (a.k.a meta-type) is denoted by a namespace aware keyword, the type-keyword.
;; A model-element map has a key :metam.core/meta that points to a map with additional type information:
;;    - :model points to the meta-model
;;    - :type points to the type-keyword
;;    - :constraints points to a vector with predicates
;; A meta-model is a map that describes model-element-types and their constraints. It contains
;;  - :hierarchy pointing to a Clojure hierarchy of keywords
;;  - :types pointing to a map {type-keyword->contraints}
;;  - :default-fn-var pointing to a function that
;;    - takes as arguments the model-element the type-keyword and the attr-keyword, and
;;    - returns a model-value that is assoc'ed with the model-element under the attr-keyword.

;; Metamodel definition infrastructure

(defn- not-nil?
  "Returns true if x is not nil."
  [x]
  (not (nil? x)))

(def required not-nil?)


(defn- check-value
  "Returns the vector [k v] if k is contained in (keys attrmap) and
   all constraint function invocations return true. Otherwise
   an IllegalArgumentException is thrown."
  [attrmap [k v]]
  (if-let [preds (get attrmap k)]
    (if (reduce #(and %1 (%2 v)) true preds)
      [k v]
      (throw (IllegalArgumentException.
              (str "Value '" v "' violates constraints for attribute " k "."))))
    (throw (IllegalArgumentException.
            (str "Attribute " k " is unknown. Valid attribute keys are " (str/join ", " (keys attrmap)))))))


(defn- keyset-for-values
  [pred kv-pairs]
  (->> kv-pairs
       (filter
        (fn [[k v]] (pred v)))
       (map first)
       set))

(defn- check-required
  "Throws an IllegalArgumentException if a required value is missing in the key-value-pairs seq."
  [attrmap kv-pairs]
  (let [not-nil-keys (keyset-for-values not-nil? kv-pairs)
        required-keys (->> attrmap
                           (filter (fn [[k preds]] (= required (first preds))))
                           (map first)
                           set)]
    (when (not (superset? not-nil-keys required-keys))
      (throw (IllegalArgumentException.
              (str "Required values are missing."
                   "Given are " not-nil-keys ", required are " required-keys))))))

(defn- get-default-fn
  [default-fn-var]
  (if (var? default-fn-var)
    (var-get default-fn-var)
    (throw (IllegalArgumentException.
            "The parameter for the default function must be a var, e.g. #'my-defaults"))))


(defn- with-defaults
  "Adds default values using the function default-fn to the model element x.
   A default-fn function must take three arguments:
   - the model element under construction as map
   - the typekey that denotes the metatype
   - the attribute key"
  [default-fn-var me]
  (if-let [default-fn (get-default-fn default-fn-var)]
    (let [typekey (->> me ::meta :type)
          available-keys (->> me ::meta :constraints keys)
          update-missing (fn [k]
                           [k (let [v (get me k)]
                                (if (nil? v) (default-fn me typekey k) v))])]
      (->> available-keys
           (map update-missing)
           (filter second)
           (into me)))
    me))

(defn metatype
  "Returns the type-keyword of the model-element me."
  [me]
  (-> me ::meta :type))


(defn- hierarchy
  "Returns the type hierarchy of the meta-model of me."
  [me]
  (-> me ::meta :model :hierarchy))


(defn metatype?
  "Returns true if me is of the meta-type specified by type-keyword."
  [type-keyword me]
  (let [type-check? (fn [me]
                      (let [h (hierarchy me)
                            mt (metatype me)]
                        (and h mt (isa? h mt type-keyword))))]
   (if (vector? me)
    (every? type-check? me)
    (type-check? me))))


(defn instance-factory
  "Returns a factory function that is used to create model elements of the
   type described by typekey and attrmap."
  [meta-model type-keyword]
  (fn [id & keys-and-values]
    (let [kv-pairs (partition 2 keys-and-values)
          attrmap (-> meta-model :types type-keyword)]
      (check-required attrmap kv-pairs)
      (->> kv-pairs
           (map (partial check-value attrmap))
           (into {::meta {:model meta-model
                          :type type-keyword
                          :constraints attrmap}
                  :name id})
           (with-defaults (:default-fn-var meta-model))))))


(defn type-of?
  "Returns a function that checks if x is of the metatype specified by typekey."
  [type-keyword]
  (partial metatype? type-keyword))


(defn value-of?
  "Returns a function that checks if a given key is contained in the set
   passed in the value-of? call."
  [ & keys]
  (let [keyset (set keys)]
    (fn [x]
      (contains? keyset x))))


(defn pr-model
  "Creates a string that represents the model-element me."
  [me]
  (cond
   (map? me) (str
             (if-let [mt (metatype me)]
               (name mt))
             (if-let [id (-> me :name)]
               (str " " id))
             " {"
             (str/join ", " (for [[k v] me :when (not (#{::meta :name} k))] (str k " " (pr-model v))))
             "}")
   (coll? me) (str
              "["
              (str/join ", " (map pr-model me))
              "]")
   :else (str me)))

(def no-defaults nil)

(defmacro defmetamodel
  "Defines several vars in the current namespace:
    - The sym-hierarchy var contains the type hierarchy.
    - The sym-metamodel var contains a map with keys/values :hierarchy and :types.
    - For each type a factory function that creates and checks a model element for that type."
  [sym hierarchy typemap default-fn-var]
  (let [hier-sym (symbol (str sym "-hierarchy"))
        mm {:hierarchy hier-sym
            :types typemap
            :default-fn-var default-fn-var}]
    `(do (def ~hier-sym ~hierarchy)
         (def ^:metamodel ~sym ~mm)
         ~@(for [[typekey attrmap] typemap]
             `(def ~(symbol (name typekey)) (instance-factory ~sym ~typekey))))))
