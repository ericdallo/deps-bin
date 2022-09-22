(ns deps-bin.impl.bin
  (:require
   [clj-zip-meta.core :refer [repair-zip-with-preamble-bytes]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clostache.parser :refer [render]]
   [me.raynes.fs :as fs]))

(def ^:private windows?
  (str/starts-with? (System/getProperty "os.name") "Windows"))

(def ^:private preamble-template
  (if windows?
    "@echo off\r\njava {{{jvm-opts}}} -jar \"%~f0\" %*\r\nexit /b\r\n"
    "#!/usr/bin/env bash\nexec java {{{jvm-opts}}} -jar $0 \"$@\"\ngoto :eof\n"))

(defn ^:private print-help []
  (println "library usage:")
  (println "  clojure -X:bin :jar MyProject.jar :name myBin")
  (println "options:")
  (println "  :help true         -- show this help (and exit)")
  (println "  :jar sym-or-str    -- specify the source name of the JAR file")
  (println "  :name sym-or-str   -- specify the name of the generated BIN file")
  (println "  :skip-realign true -- whether should skip byte alignment repair")
  (println "  :jvm-opts [strs]   -- optional list of JVM options to use during bin executing"))

(defn ^:private preamble [{:keys [jvm-opts] :as  options}]
  (-> (render preamble-template (merge options
                                       {:jvm-opts (str/join " " jvm-opts)}))
      (str/replace #"\\\$" "\\$")))

(defn ^:private write-bin [bin-file jar preamble]
  (let [bin-file (if windows? (str bin-file ".bat") bin-file)]
    (io/make-parents bin-file)
    (with-open [bin (io/output-stream bin-file)]
      (.write bin (.getBytes preamble))
      (io/copy (fs/file jar) bin))
    (fs/chmod "+x" bin-file)))

(defn build-bin
  "Core functionality for deps-bin. Can be called from a REPL or as a library.
  Returns a hash map containing:
  * `:success` -- `true` or `false`
  * `:reason` -- if `:success` is `false`, this explains what failed:
    * `:help` -- help was requested
    * `:no-jar` -- the `:jar` option was missing
    * `:no-name` -- the `:name` option was missing
  Additional detail about success and failure is also logged."
  [{:keys [help jar name skip-realign] :as options}]
  (cond

    help
    {:success false :reason :help}

    (not jar)
    {:success false :reason :no-jar}

    (not name)
    {:success false :reason :no-name}

    :else
    (let [bin-file (io/file name)]
      (println "Creating standalone executable:" name)
      (write-bin name jar (preamble options))
      (when-not skip-realign
        (println "Re-aligning zip offsets...")
        (repair-zip-with-preamble-bytes bin-file))
      {:success true})))

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
          :help   (print-help)
          :no-jar  (print-help)
          :no-name (print-help))
        (System/exit 1)))))
