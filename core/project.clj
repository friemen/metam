(defproject metam/core "1.1.1-SNAPSHOT"
  :description
  "A meta modeling facility for textual model representation."

  :url
  "https://github.com/friemen/metam"

  :license
  {:name "Eclipse Public License"
   :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies
  [[org.clojure/clojure "1.10.3"]]

  :plugins
  [[lein-codox "0.10.8"]]

  :codox
  {:defaults {}
   :sources ["src"]
   :exclude []
   :src-dir-uri "https://github.com/friemen/metam/blob/master/core"
   :src-linenum-anchor-prefix "L"}

  :scm
  {:name "git"
   :url "https://github.com/friemen/metam/core"}

  :repositories
  [["clojars" {:url "https://clojars.org/repo"
               :creds :gpg}]])
