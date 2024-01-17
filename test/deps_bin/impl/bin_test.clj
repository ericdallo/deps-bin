(ns deps-bin.impl.bin-test
  (:require
    [babashka.fs :as fs]
    [babashka.process :as p]
    [clojure.string :as str]
    [clojure.test :refer [deftest testing is]]
    [deps-bin.impl.bin :as bin]
    [clj-zip-meta.core :as clj-zip-meta]))

(deftest build-bin-as-main
  (testing "printing help"
    (is (= {:success false
            :reason :help}
           (bin/build-bin {:help true}))))
  (testing "when :jar is not provided"
    (is (= {:success false
            :reason :no-jar}
           (bin/build-bin {}))))
  (testing "when :name is not provided"
    (is (= {:success false
            :reason :no-name}
           (bin/build-bin {:jar "some-jar.jar"}))))
  (testing "when :name and :jar is provided"
    (with-redefs [bin/write-bin (constantly nil)
                  clj-zip-meta/repair-zip-with-preamble-bytes (constantly nil)]
      (is (= {:success true
              :bin-path (str (fs/canonicalize (cond-> "some-bin" (fs/windows?) (str ".bat"))))}
             (bin/build-bin {:jar "some-jar.jar"
                             :name "some-bin"})))))
  (testing "with :platforms provided"
    (with-redefs [bin/write-bin (constantly nil)
                  clj-zip-meta/repair-zip-with-preamble-bytes (constantly nil)]
      (is (= {:success true
              :bin-paths [(str (fs/canonicalize "some-bin.bat"))
                          (str (fs/canonicalize "some-bin"))]}
             (bin/build-bin {:jar "some-jar.jar"
                             :name "some-bin"
                             :platforms ["foo" :windows :unix :unix]})))))
  (testing "with invalid :platforms, default to system platform"
    (with-redefs [bin/write-bin (constantly nil)
                  clj-zip-meta/repair-zip-with-preamble-bytes (constantly nil)]
      (is (= {:success true
              :bin-path (str (fs/canonicalize (cond-> "some-bin" (fs/windows?) (str ".bat"))))}
             (bin/build-bin {:jar "some-jar.jar"
                             :name "some-bin"
                             :platforms [:bad-platform]}))))))

(deftest binary
  (let [test-jar "test/test-jar/testjar.jar"]

    (testing "jvm opts passing"
      (fs/with-temp-dir
        [temp-dir {}]
        (let [bin-path (str (fs/path temp-dir "testbin"))

              {:keys [success bin-path] :as build}
              (bin/build-bin {:jar test-jar
                              :name bin-path
                              :jvm-opts ["-Dxyz=5" "-Dzyx=0"]
                              :skip-realign true})]
          (is success build)
          (let [{:keys [out]} (-> (p/process [bin-path "xyz" "zyx"] {:out :string :err :inherit})
                                  p/check)]
            (is (= [":prop xyz :v 5" ":prop zyx :v 0"]
                   (str/split-lines out)))))))

    (testing "return error value in case of error"
      (fs/with-temp-dir
        [temp-dir {}]
        (let [bin-path (str (fs/path temp-dir "testbin"))
              {:keys [success bin-path] :as  build} (bin/build-bin {:jar test-jar
                                                                    :name bin-path
                                                                    :jvm-opts ["-Dtestjar.return=7"]
                                                                    :skip-realign true})]
          (is success build)
          (let [{:keys [exit out]} @(p/process [bin-path "testjar.return"] {:out :string :err :ihnerit})]
            (is (= [":prop testjar.return :v 7"]
                   (str/split-lines out)))
            (is (= 7 exit))))))))
