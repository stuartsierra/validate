(defproject com.stuartsierra/validate "0.1.0-SNAPSHOT"
  :description "Composable data validation functions"
  :url "https://github.com/stuartsierra/validate"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1"]
                                  [org.clojure/tools.namespace "0.2.3"]]
                   :source-paths ["dev"]}
             :clj-1.5.1 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :clj-1.4.0 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :clj-1.3.0 {:dependencies [[org.clojure/clojure "1.3.0"]]}})
