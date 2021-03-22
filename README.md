[![Clojars Project](https://img.shields.io/clojars/v/ericdallo/deps-bin.svg)](https://clojars.org/ericdallo/deps-bin)

# deps-bin

A Clojure library that generate an embeddable jar executable from a jar.
This library is an deps.edn only alternative for [lein-binplus](https://github.com/BrunoBonacci/lein-binplus).

## Usage

To generate a executable, simply merge:

```clojure
{:bin {:extra-deps {ericdallo/deps-bin {:mvn/version "RELEASE"}}
       :exec-fn deps-bin.deps-bin/bin
       :exec-args {:jar "my-jar.jar" 
                   :name "my-bin"}}}
```

into your deps.edn, and build with:

``` bash
$ clojure -X:bin
```

In this example, it expects that you have a jar `my-jar.jar`, you can generate it with [depstar](https://github.com/seancorfield/depstar), for example.

## Other options

- `:help` true         -- show this help (and exit)
- `:jar` sym-or-str    -- specify the source name of the JAR file
- `:name` sym-or-str   -- specify the name of the generated BIN file
- `:jvm-opts` [strs]   -- optional list of JVM options to use during bin executing
