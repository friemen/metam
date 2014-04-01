(ns samples.statemachine.model
  (require [clojure.java.io :as io]
           [samples.statemachine.metamodel :refer :all]
           [samples.statemachine.diagram :refer [generate-png]]
           [metam.core :refer [pr-model]]))

(defn unlock-door [state] state)
(defn lock-door [state] state)
(defn unlock-panel [state] state)
(defn lock-panel [state] state)


(def secret-compartment1
  {:commands [unlock-door lock-door unlock-panel lock-panel]
   :states  {"idle"
             {:commands [unlock-door lock-panel]
              :transitions [{:doorClosed "active"}]}
             "active"
             {:commands []
              :transitions {:lightOn "waiting-for-drawer,"
                            :drawerOpenend "waiting-for-light"}}
             "waiting-for-light"
             {:commands []
              :transitions {:lightOn "unlocked-panel"}}
             "waiting-for-drawer"
             {:commands []
              :transitions {:drawerOpened "unlocked-panel"}}
             "unlocked-panel"
             {:commands [unlock-panel lock-door]
              :transitions {:panelClosed "idle"}}}
   :reset-events [:doorOpened]
   :initial-state :idle})



(def secret-compartment2
  (state-machine "secret-compartment2"
                :commands [unlock-door lock-door unlock-panel lock-panel]
                :states [(state "idle"
                                [unlock-door lock-panel]
                                {:doorClosed "active"})
                         (state "active"
                                {:lightOn "waiting-for-drawer"
                                 :drawerOpenend "waiting-for-light"})
                         (state "waiting-for-light"
                                {:lightOn "unlocked-panel"})
                         (state "waiting-for-drawer"
                                {:drawerOpened "unlocked-panel"})
                         (state "unlocked-panel"
                                [unlock-panel lock-door]
                                {:panelClosed "idle"})]
                :reset-events [:doorOpened]))


(defstatemachine secret-compartment3
  :commands [unlock-door lock-door unlock-panel lock-panel]
  :states [(state "idle"
                  [unlock-door lock-panel]
                  {:doorClosed "active"})
           (state "active"
                  {:lightOn "waiting-for-drawer"
                   :drawerOpenend "waiting-for-light"})
           (state "waiting-for-light"
                  {:lightOn "unlocked-panel"})
           (state "waiting-for-drawer"
                  {:drawerOpened "unlocked-panel"})
           (state "unlocked-panel"
                  [unlock-panel lock-door]
                  {:panelClosed "idle"})]
  :reset-events [:doorOpened])

(add-watch #'secret-compartment3
           :diagram
           (fn [k r o n]
             (generate-png n (io/file "/home/riemensc/test.png"))))


(def secret-compartment4 (statemachine-parser "
events
  doorClosed
  drawerOpened
  lightOn   
  reset doorOpened
  panelClosed
end

commands
  unlockPanel
  lockPanel
  lockDoor
  unlockDoor
end

state idle
  actions { unlockDoor lockPanel }
  doorClosed => active
end

state active
  drawerOpened => waitingForLight
  lightOn    => waitingForDraw
end

state waitingForLight
  lightOn => unlockedPanel
end

state waitingForDraw
  drawerOpened => unlockedPanel
end

state unlockedPanel
  actions { unlockPanel lockDoor }
  panelClosed => idle
end
"))
