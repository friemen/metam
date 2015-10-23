# metam

A meta modeling facility for textual model representation with Clojure.

[![Build Status](https://travis-ci.org/friemen/metam.png?branch=master)](https://travis-ci.org/friemen/metam)

[![Clojars Project](http://clojars.org/metam/core/latest-version.svg)](http://clojars.org/metam/core)

Include the dependency as shown above in your project.clj

[API docs](https://friemen.github.com/metam)

## Usage

There are only a few API functions:

 * **defmetamodel** -- Macro to define a new metamodel consisting of a hierarchy,
 a map `{type-keyword -> {attr-keyword -> predicate-vector}}` and a defaults
 function var. The defaults function is usually a multimethod that takes
 three arguments: the model element, the type-keyword and the attr-keyword.
 It is invoked for each attribute whose value is nil, and is expected to return
 a corresponding default value.
 * **defdefaults** -- Macro to create a multimethod from a map of default values.
 * **pr-model** -- To get a (human) readable representation of an instantiated
 model m use `(pr-model m)` in the REPL.
 * **metatype** -- Returns the type-keyword of a model element.

For an example see the wsdl [metamodel](samples/src/samples/wsdl/metamodel.clj)
and [model](samples/src/samples/wsdl/model.clj) namespaces.


## Motivation by examples

If you want to describe the structure of UI forms, entities,
service interfaces, state machines or other typical elements
of an enterprise application in a DSL like manner, you could
do that easily by just using regular Clojure maps.

Those maps would form an in-memory **model**.

In fact I started creating models in Clojure by introducing
map-creating factory functions that I used in a nested fashion
to provide the look as shown below:

Here's the Clojure textual representation of a **UI panel** model:

```clojure
(def p (panel "sample" :lygeneral "flowy, fill" :components [
     (panel "details" :lygeneral "ins 0, wrap 2" :components [
        (dropdownlist "salutation" :initial-items salutations :formatter #(str % "..."))
        (textfield "firstname")
        (textfield "lastname" :required true)
        (textfield "birthday" :format date)
        (textfield "age" :format number :required true)
        (textfield "height" :format number2)
        (checkbox "male")
        (radiogroup "gender" :buttons [ :male :female (radiobutton "other" :text "OTHER") ])
      ])
      (table "addresses" :lyhint "span, grow" :columns [
        (column "firstname" :title "First name" :editable true)
        (column "lastname" :getter-fn #(:lastname %) :editable true)
        (column "street" :getter-fn #(:street %) :setter-fn #(assoc % :street %2))
      ])
      (panel "actions" :lygeneral "ins 0" :lyhint "right, span" :components [
        (button "delete" :action-fn delete)
        (button "ok" :action-fn ok)
        (button "cancel")
      ])
   ])
)
```

And a Clojure representation of a **service interface** would be something like this:

```clojure
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
```

To prevent me from creating useless models, a little bit of validation
as part of these factory functions would be nice.
And yes, it would be handy if model elements would be complemented
with default values to use convention-over-configuration and make
textual models less noisy.

If you'd build more than one of those simple DSLs yourself then you'll see
a lot of repetition (factory functions, validation, defaults provision).
**metam** simply factors these common things into a library with a
`defmetamodel` macro. So the meta model for the service interface as shown
above would look like this:

```clojure
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
   })


;; Define additional shortcut functions as you like

(defn one [name type]
  (e name :type type :mult :one))

(defn many [name type]
  (e name :type type :mult :many))
```

## Inheritance

Attributes defined for a parent meta type are available in all meta
types deriving from the parent.

I could, for example, add a `::datatype` to the `wsdl` metamodel above
and expect all attributes attached to `::datatype` to be available in
derived meta types `::complextype` and `::simpletype`:


```clojure
(defmetamodel wsdl
  (-> (make-hierarchy)
      (derive ::complextype ::datatype)
      (derive ::simpletype ::datatype))
  {::datatype     {:namespace [string?]}
   ::complextype  {:elements [(coll (type-of ::e))]}
   ::simpletype   {}
   ;; ...
   })
```

Now it would be perfectly valid to invoke

```clojure
(simpletype "mytype" :namespace "foo.bar")
```

since the hierarchy defined `::simpletype` as derived from `::datatype`.


## What the macro does

The **defmetamodel** creates a factory function for every meta type
(like ::complextype, ::e or ::service). The functions specified in the
vectors following the attribute keywords are predicates that are used
for validation.
Each factory function validates input and provides default values (if
a corresponding function var was given).

In essence, an invocation of macroexpand-1 on a defmetamodel
expression outputs:

```clojure
(do (def wsdl-hierarchy
      (-> (make-hierarchy)
          (derive :wsdl/complextype :wsdl/datatype)
          (derive :wsdl/simpletype :wsdl/datatype)))
    (def wsdl
      {:hierarchy wsdl-hierarchy,
       :types
       {:wsdl/complextype
        {:elements [(coll (type-of :wsdl/e))]},
        :wsdl/simpletype
        {},
        :wsdl/e
        {:type [required (type-of :wsdl/datatype)],
         :mult [(value-of :one :many)]},
        :wsdl/service
        {:operations [(coll (type-of :wsdl/op))]},
        :wsdl/op
        {:in-elements [(coll (type-of :wsdl/e))],
         :out-elements [(coll (type-of :wsdl/e))]}},
       :default-fn-var #'no-defaults})
    (def complextype (wsdl/instance-factory wsdl :wsdl/complextype))
    (def simpletype (wsdl/instance-factory wsdl :wsdl/simpletype))
    (def e (wsdl/instance-factory wsdl :wsdl/e))
    (def service (wsdl/instance-factory wsdl :wsdl/service))
    (def op (wsdl/instance-factory wsdl :wsdl/op)))
```

## How to add default values

The following snippet shows another example of a metamodel that also
supports default values:

```clojure
(ns visuals.forml
  (:require [metam.core :refer :all]))

(declare default-value)

(defmetamodel forml
  (-> (make-hierarchy)
      (derive ::panel     ::component)
      (derive ::panel     ::growing)
      (derive ::widget    ::component)
      (derive ::textfield ::labeled)
      (derive ::textfield ::widget)
      (derive ::label     ::widget)
      (derive ::button    ::widget))
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
```

The default-value function is a multimethod that takes three arguments:
the model element, the type-keyword and the attr-keyword. It returns the
default value. It is invoked for each attribute whose value is nil.
The **defdefaults** macro is used here to make the actual mapping between
the dispatch-value and the expression yielding the default value more concise.

The multimethod generated by the macro makes use of the hierarchy the
metamodel was defined with. For example, a default value assigned to
`[::widget :lyhint]` will apply to all meta types derived from
`::widget`, which means a missing `:lyhint` value is set to the empty
string for `::button`, `::label` and `::textfield`.


# License

Copyright 2013-2015 F.Riemenschneider

Distributed under the Eclipse Public License, the same as Clojure.
