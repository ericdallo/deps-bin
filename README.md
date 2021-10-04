[![Clojars Project](https://img.shields.io/clojars/v/com.github.ericdallo/deps-bin.svg)](https://clojars.org/com.github.ericdallo/deps-bin)

# deps-bin

A Clojure library that generate an embeddable jar executable from a jar.
This library is a `deps.edn` only alternative for [lein-binplus](https://github.com/BrunoBonacci/lein-binplus). It will create a executable file that exec `java -jar` with the provided jvm options, so you still need `java` on your `$PATH`.

## Usage

To generate a executable, simply merge:

```clojure
{:bin {:extra-deps {com.github.ericdallo/deps-bin {:mvn/version "RELEASE"}}
       :exec-fn deps-bin.deps-bin/bin
       :exec-args {:jar "my-jar.jar" 
                   :name "my-bin"}}}
```

into your deps.edn, and build with:

``` bash
$ clojure -X:bin
```

and it should generate a executable that you can run with:

``` bash
./my-bin
```

In this example, it expects that you have a jar `my-jar.jar`, you can generate it with [depstar](https://github.com/seancorfield/depstar), for example:

``` bash
$ clojure -X:jar && clojure -X:bin
```

## Other options

- `:help` true              -- show this help (and exit)
- `:jar` sym-or-str         -- specify the source name of the JAR file
- `:name` sym-or-str        -- specify the name of the generated BIN file
- `:skip-realign` true      -- if should skip byte alignment repair.
- `:jvm-opts` [strs]        -- optional list of JVM options to use during bin executing

## Contribution

Contributions are very welcome, please open an issue or a pull request if inspired :)
