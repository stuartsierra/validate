;; Executing forms in temp namespace:  G__2198
(require '[com.stuartsierra.validate :as v])
;;=> nil

(def number-validator (v/is number?))
;;=> #<Var@1c7d7100: 
;;     #<validate$validator$fn__1876 com.stuartsierra.validate$validator$fn__1876@53a816e5>>

(number-validator 42)
;;=> nil

(number-validator "hi")
;;=> ({:expected (v/is number?), :value "hi"})

(def under-10 (v/validator #(< % 10) {:error "must be less than 10"}))
;;=> #<Var@6bef63f9: 
;;     #<validate$validator$fn__1876 com.stuartsierra.validate$validator$fn__1876@7031dcf0>>

(under-10 42)
;;=> ({:error "must be less than 10", :value 42})

(try
  (v/assert-valid (/ 22.0 7.0) (v/is integer?))
  (catch Exception e e))
;;=> #<ExceptionInfo clojure.lang.ExceptionInfo: Validation failed {:errors ({:value 3.142857142857143, :expected (v/is integer?)}), :line nil, :expr (/ 22.0 7.0), :value 3.142857142857143, :file "NO_SOURCE_PATH"}>

(try
  (v/assert-valid (/ 22.0 7.0) (v/is integer?))
  (catch Exception e (ex-data e)))
;;=> {:errors ({:value 3.142857142857143, :expected (v/is integer?)}),
;;    :line nil,
;;    :expr (/ 22.0 7.0),
;;    :value 3.142857142857143,
;;    :file "NO_SOURCE_PATH"}

(-> (rand-int 100) (* 2) inc (v/assert-valid (v/is odd?)))
;;=> 15

(v/valid? "hello" (v/is string?))
;;=> true

(def odd-integer (v/and (v/is integer?) (v/is odd?)))
;;=> #<Var@3bae7fff: 
;;     #<validate$and$fn__1900 com.stuartsierra.validate$and$fn__1900@67b72ada>>

(odd-integer 4)
;;=> ({:expected (v/is odd?), :value 4})

(odd-integer 4.0)
;;=> ({:expected (v/is integer?), :value 4.0})

((v/every (v/is even?)) [4 3 8 15])
;;=> ({:errors
;;     ({:value 3, :expected (v/is even?)}
;;      {:value 15, :expected (v/is even?)}),
;;     :value (4 3 8 15),
;;     :expr (call seq (fn [input] (seq (mapcat vfn input))))})

((v/are even?) [4 3 8 15])
;;=> ({:errors
;;     ({:value 3, :expected (v/are even?)}
;;      {:value 15, :expected (v/are even?)}),
;;     :value (4 3 8 15),
;;     :expr (call seq (fn [input] (seq (mapcat vfn input))))})

((v/or (v/is integer?) (v/is float?)) 3)
;;=> nil

((v/or (v/is integer?) (v/is float?)) 3.14)
;;=> nil

((v/or (v/is integer?) (v/is float?)) "foo")
;;=> ({:expected "at least one of",
;;     :errors
;;     [{:value "foo", :expected (v/is integer?)}
;;      {:value "foo", :expected (v/is float?)}],
;;     :input "foo"})

(def simple-map
 (v/and (v/keys (v/are keyword?)) (v/vals (v/are string?))))
;;=> #<Var@2cec4462: 
;;     #<validate$and$fn__1900 com.stuartsierra.validate$and$fn__1900@6b8dbef1>>

(simple-map {:a "one", :b 2})
;;=> ({:errors
;;     ({:errors ({:expected (v/are string?), :value 2}),
;;       :value ("one" 2),
;;       :expr (call seq (fn [input] (seq (mapcat vfn input))))}),
;;     :expr
;;     (com.stuartsierra.validate/call clojure.core/vals (v/are string?)),
;;     :value ("one" 2)})

((v/count (v/validator #(< % 4))) [:a :b :c :d :e])
;;=> ({:errors
;;     ({:value 5,
;;       :pred
;;       #<G__2198$eval2244$fn__2245 G__2198$eval2244$fn__2245@20986975>}),
;;     :value 5,
;;     :expr
;;     (com.stuartsierra.validate/call
;;      clojure.core/count
;;      (v/validator (fn* [p1__2243#] (< p1__2243# 4))))})

((v/are char?) "hello")
;;=> nil

((v/are char?) 42)
;;=> {:errors
;;    (#<IllegalArgumentException java.lang.IllegalArgumentException: Don't know how to create ISeq from: java.lang.Long>),
;;    :value 42,
;;    :expr (call seq (fn [input] (seq (mapcat vfn input))))}

(def john {:name "John Doe", :address {:city "Baltimore"}})
;;=> #<Var@4ad3d46d: {:name "John Doe", :address {:city "Baltimore"}}>

((v/in [:address :city] (v/is string?)) john)
;;=> nil

((v/in [:address :zip] (v/is integer?)) john)
;;=> ({:in [:address :zip],
;;     :error :not-found,
;;     :value {:name "John Doe", :address {:city "Baltimore"}}})

((v/if-in [:address :zip] (v/is integer?)) john)
;;=> nil

