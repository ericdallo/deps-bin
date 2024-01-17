(ns deps-bin.impl.bin
  (:require
   [clj-zip-meta.core :refer [repair-zip-with-preamble-bytes]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clostache.parser :refer [render]]
   [me.raynes.fs :as fs]))

(def ^:private allowed-platforms
  #{:unix :windows})

(defn ^:private system-platform []
  (if (str/starts-with? (System/getProperty "os.name") "Windows")
    :windows
    :unix))

(defn ^:private platform-filename [platform name]
  (case platform
    :windows (str name ".bat")
    :unix name))

(defn ^:private preamble-template [platform]
  (case platform
    :unix    "#!/usr/bin/env bash\nexec java {{{jvm-opts}}} -jar $0 \"$@\"\ngoto :eof\n"
    :windows "@echo off\r\njava {{{jvm-opts}}} -jar \"%~f0\" %*\r\nexit /b %errorlevel%\r\n"))

(defn ^:private print-help []
  (println "library usage:")
  (println "  clojure -X:bin :jar MyProject.jar :name myBin")
  (println "options:")
  (println "  :help true         -- show this help (and exit)")
  (println "  :jar sym-or-str    -- specify the source name of the JAR file")
  (println "  :name sym-or-str   -- specify the name of the generated BIN file")
  (println "  :skip-realign true -- whether should skip byte alignment repair")
  (println "  :jvm-opts [strs]   -- optional list of JVM options to use during bin executing")
  (println "  :platforms [kws]   -- optional list of platforms for which to emit bins,")
  (println "                        valid kws: :unix, :windows, defaults to system platform"))

(defn ^:private preamble [platform {:keys [jvm-opts] :as  options}]
  (-> (preamble-template platform)
      (render (merge options {:jvm-opts (str/join " " jvm-opts)}))
      (str/replace #"\\\$" "\\$")))

(defn ^:private write-bin [bin-file jar preamble]
  (io/make-parents bin-file)
  (with-open [bin (io/output-stream bin-file)]
    (.write bin (.getBytes preamble))
    (io/copy (fs/file jar) bin))
  (fs/chmod "+x" bin-file))

(defn ^:private coerce-platforms [coll]
  (if-some [platforms (->> coll (keep allowed-platforms) distinct not-empty)]
    platforms
    [(system-platform)]))

(defn emit-bin!
  "Writes bin, optionally re-aligns it and returns its canonical path."
  [platform {:keys [jar name skip-realign] :as options}]
  (let [name     (platform-filename platform name)
        bin-file (io/file name)
        preamble (preamble platform options)]
    (println "Creating" (clojure.core/name platform) "standalone executable:" name)
    (write-bin name jar preamble)
    (when-not skip-realign
      (println "Re-aligning zip offsets...")
      (repair-zip-with-preamble-bytes bin-file))
    (.getCanonicalPath bin-file)))

(defn build-bin
  "Core functionality for deps-bin. Can be called from a REPL or as a library.
  Returns a hash map containing:
  * `:success` -- `true` or `false`
  * `:bin-path` -- On `:success`, it is the abs path to the binary.
  * `:bin-paths` -- Same as above, but when building for multiple platforms.
  * `:reason` -- if `:success` is `false`, this explains what failed:
    * `:help` -- help was requested
    * `:no-jar` -- the `:jar` option was missing
    * `:no-name` -- the `:name` option was missing
  Additional detail about success and failure is also logged."
  [{:keys [help jar name skip-realign platforms] :as options}]
  (cond

    help
    {:success false :reason :help}

    (not jar)
    {:success false :reason :no-jar}

    (not name)
    {:success false :reason :no-name}

    :else
    (let [platforms     (coerce-platforms platforms)
          emitted-paths (mapv #(emit-bin! % options) platforms)]
      (if (= (count emitted-paths) 1)
        {:success true :bin-path (first emitted-paths)}
        {:success true :bin-paths emitted-paths}))))

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
