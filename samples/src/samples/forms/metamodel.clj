(ns samples.forms.metamodel
  (:use [metam.core]))

(declare default-value)

(defmetamodel forml
  (-> (make-hierarchy)
      (derive ::panel ::component)
      (derive ::panel ::growing)
      (derive ::widget ::component)
      (derive ::textfield ::labeled)
      (derive ::textfield ::widget)
      (derive ::label ::widget)
      (derive ::button ::widget))
  {::panel       {:lygeneral [string?]
                  :lycolumns [string?]
                  :lyrows [string?]
                  :lyhint [string?]
                  :components [(coll (type-of ::component))]}
   ::label       {:text [string?]
                  :lyhint [string?]}
   ::textfield   {:label [string?]
                  :lyhint [string?]
                  :labelyhint [string?]}
   ::button      {:text [string?]
                  :lyhint [string?]}}
  #'default-value)


(defmulti default-value
  (fn [spec type-keyword attr-keyword]
    [type-keyword attr-keyword])
  :hierarchy #'forml-hierarchy)

(defmacro ^:private defdefault
  [dispatch-value & forms]
  (let [args ['spec 'tk 'ak]]
    `(defmethod default-value ~dispatch-value
     ~args
     ~@forms)))

(defdefault :default                     nil)
(defdefault [::widget :lyhint]           "")
(defdefault [::growing :lyhint]          "grow")
(defdefault [::panel :lyrows]            "")
(defdefault [::panel :lycolumns]         "")
(defdefault [::labeled :labelyhint]      "")
(defdefault [::textfield :label]         (:name spec))
(defdefault [::button :text]             (:name spec))
