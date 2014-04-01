(ns samples.statemachine.metamodel
  (:require [metam.core :refer :all]
            [clojure.set :refer [difference]]
            [instaparse.core :as insta]))


(defmetamodel statemachine
  (make-hierarchy)
  {::state* {:commands [(coll fn?)]
             :transitions [(coll keyword? string?)]}
   ::state-machine {:commands [(coll fn?)]
                    :states [(coll (type-of ::state*))]
                    :reset-events [(coll keyword?)]}}
  #'no-defaults)


(defn state
  ([name transitions]
     (state name [] transitions))
  ([name commands transitions]
     (state* name :commands commands :transitions transitions)))


(defmacro defstatemachine
  [sym & args]
  `(def ~sym (let [sm# (state-machine ~(str sym) ~@args)
                   drs# (dangling-references sm#)]
               (if (seq drs#)
                 (throw (IllegalArgumentException.
                         (str "The following states are referenced, but not defined " drs#))))
               (transform-states sm#))))

(defn dangling-references
  "Returns the set of references to states that are not defined."
  [sm]
  (let [defd-state-names (->> sm :states (map :name) set)
        refd-state-names (->> sm :states (mapcat :transitions) (map second) set)]
    (difference refd-state-names defd-state-names)))


(defn transform-states
  "Turns the vector of states into a map with the state name as key."
  [sm]
  (let [states-map (->> sm :states (map (juxt :name identity)) (into {}))]
    (-> sm
        (assoc :states states-map)
        (assoc :initial-state (-> sm :states first)))))


(def statemachine-parser
  (insta/parser "
MACHINE = <s>? EVENTS <s> COMMANDS <s> STATES <s>?
STATES = (STATE <s>)* STATE
EVENTS = <'events'> <s> (EVENT <s>)+ <'end'>
COMMANDS = <'commands'> <s> (COMMAND <s>)+ <'end'>
STATE = <'state'> <s> token (<s> ACTIONS)? <s> TRANSITIONS <'end'>
ACTIONS = <'actions'> <s>? <'{'> <s>? (COMMAND <s>)* <'}'>
TRANSITIONS = (TRANSITION <s>)*
TRANSITION = token <s> <'=>'> <s> token
EVENT = 'reset'? token
COMMAND = token
<token> = #'[a-zA-Z]+'
<s> = #'\\s+'
"))


