(ns deps-bin.main
  "Entry point for clojure -X options."
  (:require [deps-bin.impl.bin :as bin]))

(defn bin
  "Generic entry point for bin invocations.
  Can be used with `clojure -X`:
  In `:aliases`:
```clojure
      :bin {:replace-deps {com.github.ericdallo/deps-bin {:mvn/version ...}}
            :exec-fn deps-bin.main/bin
            :exec-args {:name \"myBin\"}}
```
  Then run:
```
      clojure -X:bin :jar MyProject.jar
```
  If the source JAR file is fixed, it could be added to `:exec-args` in
  `deps.edn`:
```clojure
      :jar {:replace-deps {com.github.ericdallo/deps-bin {:mvn/version ...}}
            :exec-fn deps-bin.main/bin
            :exec-args {:jar MyProject.jar}}
```
  `:jar` can be specified as a symbol or a string.
  `:name` can be specified as a symbol or a string."
  [options]
  (bin/build-bin-as-main options))
