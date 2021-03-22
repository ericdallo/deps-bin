(ns deps-bin.impl.bin-test
  (:require
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
      (is (= {:success true}
             (bin/build-bin {:jar "some-jar.jar"
                             :name "some-bin"}))))))
