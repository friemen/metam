(ns samples.statemachine.diagram
  (:require [clojure.java.io :as io])
  (:import [net.sourceforge.plantuml SourceStringReader]))


(defn statename
  [s]
  (let [n (if (string? s) s (:name s))]
    (.replaceAll n "-" "")))

(defn umlstr
  [sm]
  (str "@startuml\n"
       "[*] -> " (-> sm :initial-state :name) "\n"
       (apply str (for [from-state (-> sm :states vals),
                        [event to-state] (:transitions from-state)]
                    (str (statename from-state)
                         " --> "
                         (statename to-state)
                         (str  " : " (name event))
                         "\n")))
       (apply str (for [state (-> sm :states vals),
                        command (:commands state)]
                    (str (statename state)
                         ":"
                         (-> command class str (.split "\\$") (nth 1)) "\n")))
       "\n@enduml"))

(defn generate-png
  [sm pngfile]
  (with-open [os (io/output-stream pngfile)]
    (-> sm umlstr SourceStringReader. (.generateImage os))))
