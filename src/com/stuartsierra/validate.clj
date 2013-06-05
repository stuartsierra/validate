(ns com.stuartsierra.validate
  (:refer-clojure :exclude (and cond count keys or range vals when)))

;;; General-purpose predicates

(defn supports?
  "Returns true if it is possible to call f on x without throwing an
  exception."
  [f x]
  (try (f x)
       true
       (catch Exception _ false)))

(defn seqable?
  "Returns true if it is possible to call clojure.core/seq on x."
  [x]
  (supports? seq x))

;;; Core validation

(defn validator
  "Returns a validator function based on predicate. The validator
  function accepts a single argument, calls pred on it. If pred
  returns true (the validation passes) the validator function returns
  nil, otherwise it returns a list containing msg. msg must be a map
  describing the failure, the original input will be assoc'd into this
  map with the key :value."
  ([pred]
     (validator pred {:pred pred}))
  ([pred msg]
     (fn [input]
       (when-not (pred input)
         (list (assoc msg :value input))))))

;;; Validator-creating macros

(defmacro is
  "Returns a validator function which will call (pred ~@args input).
  pred must be a symbol."
  ([pred]
     {:pre [(symbol? pred)]}
     `(validator ~pred {:expected '~&form}))
  ([pred & args]
     {:pre [(symbol? pred)]}
     `(validator (partial ~pred ~@args)
                 {:expected '~&form})))

(defmacro is-not
  "Returns a validator function which will call
  (not (pred ~@args input)). pred must be a symbol."
  ([pred]
     {:pre [(symbol? pred)]}
     `(validator (complement ~pred)
                 {:expected '~&form}))
  ([pred & args]
     {:pre [(symbol? pred)]}
     `(validator (complement (partial ~pred ~@args))
                 {:expected '~&form})))

(defmacro are
  "Returns a validator function which will call (pred ~@args input) on
  each element of an input collection. pred must be a symbol."
  ([pred]
     {:pre [(symbol? pred)]}
     `(every (validator ~pred {:expected '~&form})))
  ([pred & args]
     {:pre [(symbol? pred)]}
     `(every (validator (partial ~pred ~@args)
                        {:expected '~&form}))))

;;; Validator Combinators

(defn and-all
  "Returns a single validator function which takes a single argument
  and calls all the given validators on it. If all the validators
  pass, it returns nil, otherwise it returns a sequence of errors."
  [& validators]
  (fn [input]
    (seq (mapcat #(% input) validators))))

(defn and
  "Returns the conjunction of validator functions. The returned
  function takes a single argument and calls all the validator
  functions on it. If all the validations pass, it returns nil. If any
  validation fails, short-circuits and returns a sequence of errors,
  does not run the remaining validations."
  [& validators]
  (fn [input]
    (loop [validators validators]
      (when-first [vfn validators]
        (if-let [errs (vfn input)]
          errs
          (recur (rest validators)))))))

(defn or
  "Returns the disjunction of validator functions. The returned
  function takes a single argument and calls all the validator
  functions on it. If all the validations pass, it returns nil. If any
  validation fails, short-circuits and returns a sequence of errors,
  does not run the remaining validations."
  [& validators]
  (fn [input]
    (loop [validators validators
           errors []]
      (if-let [vfn (first validators)]
        (when-let [errs (vfn input)]
          (recur (rest validators) (into errors errs)))
        (list {:expected "at least one of"
               :errors errors
               :input input})))))

(defn if-in
  "Like 'in' but does not return an error if the structure does not
  contain the given keys."
  [ks & validators]
  (let [vfn (apply and validators)]
    (fn [input]
      (let [value (get-in input ks ::not-found)]
        (if (= value ::not-found)
          nil
          (when-let [errs (vfn value)]
            (list {:in ks :errors errs :value input})))))))

(defn has
  "Returns a validation function that checks for the presence of keys
  in a nested associative structure. ks is a sequence of keys."
  [ks]
  (fn [input]
    (clojure.core/when (= ::not-found (get-in input ks ::not-found))
      (list {:in ks :error :not-found :value input}))))

(defn in
  "Returns a composition of validator functions that operate on a value
  in nested associative structures. Reaches into the structure as with
  'get-in' where ks is a sequential collection of keys. Calls the
  'and'd validators on the value. If any validations fail, returns
  a map with :ks and :errors.

  If the structure does not contain ks, returns an error. See also
  if-in."
  [ks & validators]
  (and (has ks)
       (apply if-in ks validators)))

(defn every
  "Returns a validator function that applies the validators to each
  element of the input collection."
  [& validators]
  (let [vfn (apply and validators)]
    (and (is seqable?)
         (fn [input]
           (let [s (seq input)]
             (clojure.core/when s
               (seq (mapcat #(vfn %) s))))))))

(defn when
  "Returns a validator function that only checks the validators
  when (pred input) is true."
  [pred & validators]
  (let [vfn (apply and validators)]
    (fn [input]
      (clojure.core/when (pred input)
        (vfn input)))))

(defn cond
  "Returns a validator function that checks multiple conditions. Each
  clause is a pair of a predicate and a validator. Optional last
  argument is a validator to run if none of the predicates returns
  true; otherwise the validation fails."
  [& clauses]
  (fn [input]
    (loop [[pred vfn] clauses]
      (if vfn
        (if (pred input)
          (vfn input)
          (recur (nthnext clauses 2)))
        (if pred
          (pred input)
          (list {:error :no-matching-clause :value input}))))))

(defn call-fn
  "Returns a validator function that calls f on the input value and
  then performs validation on the return value of f. If f throws an
  exception, validation fails. validators are validation functions
  combined as with 'and'."
  [f msg & validators]
  (let [vfn (apply and validators)]
    (fn [input]
      (try (let [v (f input)]
             (when-let [errs (vfn v)]
               (list (merge msg {:value v :errors errs}))))
           (catch Exception e
             (merge msg {:value input :errors (list e)}))))))

(defmacro call
  "Returns a validation function that calls f on the input value and
  then performs validation on the return value of f. validators are
  combined as with 'and'."
  [sym & validators]
  {:pre [(symbol? sym)]}
  `(call-fn ~sym {:expr '~&form} ~@validators))

(defmacro keys
  "Returns a validation function that performs validation on the keys
  of its input, which must be a map."
  [& validators]
  `(call clojure.core/keys ~@validators))

(defmacro vals [& validators]
  `(call clojure.core/vals ~@validators))

(defmacro count [& validators]
  `(call clojure.core/count ~@validators))

;;; Validator-creation functions

(defn range
  "Returns a validator function that checks if its input is numeric and
  is between min, inclusive, and max, exclusive."
  [min-incl max-excl]
  (and (is number?)
       (validator #(clojure.core/and (<= min-incl %) (< % max-excl))
                  {:expected `(range ~min-incl ~max-excl)})))

(defn within
  "Returns a validator function that checks if its input is between
  min and max, both inclusive. Uses clojure.core/compare to compare
  values."
  [min-incl max-incl]
  (validator #(clojure.core/and (not (neg? (compare % min-incl)))
                                (not (pos? (compare % max-incl))))
             {:expected `(within ~min-incl ~max-incl)}))

;;; Invocation patterns

(defn valid?
  "Returns true if value passes the validation function."
  [value validator]
  (not (validator value)))

(defmacro assert-valid
  "Tests the value of expr with validator. If it passes, returns the
  value. If not, throws an exception with validation information
  attached."
  [expr validator]
  `(let [v# ~expr]
     (if-let [errors# (~validator v#)]
       (throw (ex-info "Validation failed"
                       {:errors errors#
                        :expr '~expr
                        :value v#
                        :line ~(:line (meta &form))
                        :file ~*file*}))
       v#)))
