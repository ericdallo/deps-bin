(ns deps-bin.impl.bin
  (:require [clojure.tools.logging :as logger]))

(defn ^:private print-help []
  (println "library usage:")
  (println "  clojure -X:bin :jar MyProject.jar :name myBin")
  (println "options:")
  (println "  :help true         -- show this help (and exit)")
  (println "  :jar sym-or-str    -- specify the source name of the JAR file")
  (println "  :name sym-or-str   -- specify the name of the generated BIN file"))

(defn ^:private build-bin
   "Core functionality for deps-bin. Can be called from a REPL or as a library.
  Returns a hash map containing:
  * `:success` -- `true` or `false`
  * `:reason` -- if `:success` is `false`, this explains what failed:
    * `:help` -- help was requested
    * `:no-jar` -- the `:jar` option was missing
    * `:copy-failure` -- one or more files could not be copied to BIN
  Additional detail about success and failure is also logged."
  [{:keys [help jar] :as options}]
  (cond

    help
    {:success false :reason :help}

    (not jar)
    {:success false :reason :no-jar}

    :else
    {:success true}))

(defn build-bin-as-main
  "Command-line entry point for `-X` (and legacy `-M`) that performs
  checking on arguments, offers help, and calls `(System/exit 1)` if
  the BIN-building process encounters errors."
  [options]
  (let [result (build-bin options)]
    (if (:success result)
      (shutdown-agents)
      (do
        (case (:reason result)
          :help         (print-help)
          :no-jar       (print-help)
          :copy-failure (logger/error "Completed with errors!"))
        (System/exit 1)))))
