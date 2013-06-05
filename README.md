# com.stuartsierra.validate

This is a Clojure data validation library. There are many like it, but
this one is mine.

**ALPHA code:** All names and return values are subject to change
without warning in subsequent 0.x releases.



## Description

The goal of this library is to construct validation functions which:

1. Are fully composable
2. Do not assume anything about the input structure
   (e.g. that it is a map)
3. Return data structures (for internationalization or further
   processing) instead of strings
4. Return a complete description of what failed and why

The last point is the hardest to get right. The layout and naming in
the "failure" messages returned by this library are the part most
likely to change.

A *validation function* takes a single argument, the value you want to
validate. It returns either:

* `nil`, indicating that the validation *passed*

* a sequence of maps describing how the validation *failed*

Most of this library consists of tools to create and compose
validation functions.



## Releases and Dependency Information

No releases yet.

Install version "0.1.0-SNAPSHOT" locally by running `lein install` in
this directory. Then add a [Leiningen](http://leiningen.org/)
dependency as `[com.stuartsierra/validate "0.1.0-SNAPSHOT"]`.



## Example Usage

    (require '[com.stuartsierra.validate :as v])


### Creating Validation Functions

The `is` macro transforms any boolean predicate function into a
validation function:

    (def number-validator (v/is number?))
    
    (number-validator 42)
    ;;=> nil
    
    (number-validator "hi")
    ;;=> ({:expected (v/is number?), :value "hi"})


The `is` macro only works on symbols. You can transform an abitrary
boolean predicate function into a validation function with the
`validator` function. `validator` takes a predicate and an error
description, which must be a map. When the predicate returns logical
false, the validation function will associate the input value into
that map:

    (def under-10
      (v/validator #(< % 10) {:error "must be less than 10"}))

    (under-10 42)
    ;;=> ({:error "must be less than 10", :value 42})


### Assertions

Normally you would not call a validation function directly but instead
pass it to the `assert-valid` macro, which throws an exception if the
validation fails:

    (v/assert-valid (/ 22.0 7.0) (v/is integer?))
    ;; #<ExceptionInfo clojure.lang.ExceptionInfo: Validation failed ...>

Get the error messages out from `ex-data`:

    (ex-data *e)
    ;;=> {:errors ({:value 3.142857142857143,
    ;;              :expected (v/is integer?)}),
    ;;    :line 1,
    ;;    :expr (/ 22.0 7.0),
    ;;    :value 3.142857142857143,
    ;;    :file "NO_SOURCE_PATH"}

When the validation passes, `assert-valid` returns the input value,
making it suitable for pipelining:

    (-> (rand-int 100)
        (* 2)
        inc
        (v/assert-valid (v/is odd?)))
    ;;=> 145

There is also a function, `valid?`, that returns true if the
validation passes or false if it does not:

    (v/valid? "hello" (v/is string?))
    ;;=> true


### Combining Validation Functions

The `and` function combines multiple validation functions into one.
This is not the same as `clojure.core/and`, but it does short-circuit
the same way:

    (def odd-integer
      (v/and (v/is integer?)
             (v/is odd?)))

    (odd-integer 4)
    ;;=> ({:expected (v/is odd?), :value 4})

    (odd-integer 4.0)
    ;;=> ({:expected (v/is integer?), :value 4.0})


### Looking Inside Collections

The `every` function transforms a simple validation function into a
validation function that operates on each element of a collection:

    ((v/every (v/is even?)) [4 3 8 15])
    ;;=> ({:value 3, :expected (v/is even?)}
    ;;    {:value 15, :expected (v/is even?)})

The `are` macro is shorthand for `every` and `is`.

    ((v/are even?) [4 3 8 15])
    ;;=> ({:value 3, :expected (v/is even?)}
    ;;    {:value 15, :expected (v/is even?)})

For map-like collections, you can use `keys` and `vals` to apply
validations to the keys and values of the map, respectively:

    (def simple-map
     (v/and (v/keys (v/are keyword?))
            (v/vals (v/are string?))))

    (simple-map {:a "one", :b 2})
    ;;=> ({:errors ({:expected (v/are string?), :value 2}),
    ;;     :expr (v/vals (v/are string?)),
    ;;     :value ("one" 2)})

You can also use `count` to validate facts about the number of
elements in a collection:

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

(This error message needs work.)

More generally, you can call any function on the input and perform
additional validation on the return value of that function with the
`call` macro and `call-fn` function.


### Looking Inside Maps

When you know that certain keys should be present in a map, you can
check that their values pass additional validation checks. The `in`
function takes a vector of keys and one or more validation functions.
It returns a validation function that navigates into the map as with
Clojure's `get-in` function and runs the validation functions on the
value. 

    (def john {:name "John Doe", :address {:city "Baltimore"}})

    ((v/in [:address :city] (v/is string?)) john)
    ;;=> nil

    ((v/in [:address :zip] (v/is integer?)) john)
    ;;=> ({:in [:address :zip],
    ;;     :error :not-found,
    ;;     :value {:name "John Doe", :address {:city "Baltimore"}}})

With `in`, the map must contain the given keys or it fails the
validation. An alternate form, `if-in`, allows the validation to pass
if any keys are missing.

    ((v/if-in [:address :zip] (v/is integer?)) john)
    ;;=> nil



## Development and Contributing

Unfortunately, I do not have time to respond to every issue or pull
request. Please feel free to fork and modify this library to suit your
own needs. I will make updates and new releases as I have time
available.



## Change Log

No releases yet.



## Copyright and License

Copyright (c) Stuart Sierra, 2013. All rights reserved. The use and
distribution terms for this software are covered by the Eclipse Public
License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can
be found in the file epl-v10.html at the root of this distribution. By
using this software in any fashion, you are agreeing to be bound by
the terms of this license. You must not remove this notice, or any
other, from this software.
