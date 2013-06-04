(ns user
  "Conveniences for interactive development and testing."
  (:require [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [com.stuartsierra.validate :as v]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :as pprint :refer (pprint)])
  (:import (java.io File PrintWriter PushbackReader)))

(comment
  ;; example to replace:
  :post [(every? (fn [[k v]] (and (string? k)
                                  (every? (fn [[k v]] (and (keyword? k)
                                                           (every? (fn [[k v]] (and (keyword? k)
                                                                                    (sequential? v))) v))) v))) %)])

(def m1
  (v/and (v/is map?)
         (v/keys (v/are string?))
         (v/vals (v/every (v/and (v/is map?)
                                 (v/keys (v/are keyword?))
                                 (v/vals (v/are sequential?)))))))

;;; Regenerating the readme.clj samples file

(defn format-return [return-value]
  (str/join "\n"
   (map str
        (cons ";;=> " (repeat ";;   "))
        (str/split-lines (with-out-str (pprint return-value))))))

(defn transcript
  "Run all forms, printing a transcript as if forms were
   individually entered interactively at the REPL."
  [forms]
  (binding [*ns* *ns*]
    (let [temp (gensym)]
      (println ";; Executing forms in temp namespace: " temp)
      (in-ns temp)
      (clojure.core/use 'clojure.core 'clojure.repl 'clojure.pprint)
      (doseq [f forms]
        (pprint/with-pprint-dispatch pprint/code-dispatch
          (pprint f))
        (println (format-return (eval f)))
        (println))
      (remove-ns temp)
      :done)))

(defn read-all [in]
  (lazy-seq
   (let [form (read in false ::eof)]
     (when (not= form ::eof)
       (cons form (read-all in))))))

(defn run-transcript [file]
  (with-open [in (PushbackReader. (io/reader file))]
    (transcript (read-all in))))

(defn save-transcript [input-file]
  (let [output-file (File/createTempFile "temp" ".clj")]
    (with-open [out (io/writer output-file)]
      (binding [*out* out]
        (run-transcript input-file)))
    output-file))

(defn overwrite-transcript [file]
  (let [tempfile (save-transcript (io/file file))]
    (io/copy tempfile (io/file file))))

(defn readme []
  (overwrite-transcript "dev/readme.clj"))
