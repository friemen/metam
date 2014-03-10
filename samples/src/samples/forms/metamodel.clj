(ns samples.forms.metamodel
  (:require [metam.core :refer :all]))

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


(defdefaults default-value forml
  {:default                     nil
   [::widget :lyhint]           ""
   [::growing :lyhint]          "grow"
   [::panel :lyrows]            ""
   [::panel :lycolumns]         ""
   [::labeled :labelyhint]      ""
   [::textfield :label]         (:name spec)
   [::button :text]             (:name spec)})


