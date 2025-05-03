(ns otus-16.homework-test
  (:require [clojure.test :refer :all]
            [otus-16.homework :as hw]
            [clojure.java.io :as io]))

;; Define the path to the test log file
(def ^:const test-log-file "./access.log.test")

;; Helper to ensure the test file exists
(defn test-file-exists? []
  (.exists (io/file test-log-file)))

;; Mock the function that lists log files to return only the test file
(defn mock-list-log-files [_dir]
  (if (test-file-exists?)
    [test-log-file]
    (throw (Exception. (str "Test log file not found: " test-log-file)))))

(deftest solution-test
  (if-not (test-file-exists?)
    (println (str "WARNING: Test log file not found at " test-log-file ". Skipping tests."))
    ;; Use 'with-redefs' to temporarily replace the real list-log-files function
    (with-redefs [hw/list-log-files mock-list-log-files]

      (testing "Calculate total bytes and metrics for all URLs and referrers from test file"
        (let [result (hw/solution)]
          ;; Check total bytes
          (is (= 266860 (:total-bytes result)) "Total bytes calculated from test file")

          ;; Check some specific URL byte counts
          (is (= 24836 (get (:bytes-by-url result) "/")) "Bytes for URL '/'")
          (is (= 31432 (get (:bytes-by-url result) "/new/")) "Bytes for URL '/new/'")
          (is (= 52856 (get (:bytes-by-url result) "/%D0%BE%D0%BB%D0%B8%D0%BC%D0%BF%D0%B8%D0%B0%D0%B4%D0%B0-80-%D0%B2-%D0%A1%D0%A1%D0%A1%D0%A0-%D0%91%D1%80%D0%B5%D0%B6%D0%BD%D0%B5%D0%B2-%D0%BF%D0%B5%D1%80%D0%B5%D0%B4-%D1%81%D1%82%D0%B0%D0%B4%D0%B8%D0%BE%D0%BD%D0%BE%D0%BC-%D1%87%D0%B8%D1%82%D0%B0%D0%B5%D1%82/")) "Bytes for URL '/олимпиада-80.../'")
          (is (= 16 (count (:bytes-by-url result))) "Total number of unique URLs in bytes-by-url map") ; Count unique URLs found

          ;; Check some specific referrer counts
          (is (= 3 (get (:urls-by-referrer result) "https://www.google.com/")) "Unique URL count for referrer 'google.com'")
          (is (= 2 (get (:urls-by-referrer result) "https://baneks.site/")) "Unique URL count for referrer 'baneks.site/'")
          (is (= 1 (get (:urls-by-referrer result) "http://77.244.214.49:80/left.html")) "Unique URL count for referrer '77.244...left.html'")
          (is (= 10 (count (:urls-by-referrer result))) "Total number of unique referrers in urls-by-referrer map"))) ; Count unique referrers found

      (testing "Filter by specific URL"
        (let [result (hw/solution :url "/new/")]
          (is (= 266860 (:total-bytes result)) "Total bytes remain the same when filtering URL")
          (is (= {"/new/" 31432}
                 (:bytes-by-url result)) "Bytes-by-url should only contain the specified URL")
          ;; Urls-by-referrer should remain unfiltered when only URL is specified
          (is (= 10 (count (:urls-by-referrer result))) "Urls-by-referrer count remains unfiltered")))

      (testing "Filter by specific referrer"
        (let [result (hw/solution :referrer "https://www.google.com/")]
          (is (= 266860 (:total-bytes result)) "Total bytes remain the same when filtering referrer")
          ;; Bytes-by-url should remain unfiltered when only referrer is specified
          (is (= 16 (count (:bytes-by-url result))) "Bytes-by-url count remains unfiltered")
          (is (= {"https://www.google.com/" 3}
                 (:urls-by-referrer result)) "Urls-by-referrer should only contain the specified referrer")))

      (testing "Filter by specific URL and referrer"
        (let [result (hw/solution :url "/%D0%BE%D0%BB%D0%B8%D0%BC%D0%BF%D0%B8%D0%B0%D0%B4%D0%B0-80-%D0%B2-%D0%A1%D0%A1%D0%A1%D0%A0-%D0%91%D1%80%D0%B5%D0%B6%D0%BD%D0%B5%D0%B2-%D0%BF%D0%B5%D1%80%D0%B5%D0%B4-%D1%81%D1%82%D0%B0%D0%B4%D0%B8%D0%BE%D0%BD%D0%BE%D0%BC-%D1%87%D0%B8%D1%82%D0%B0%D0%B5%D1%82/"
                                  :referrer "https://www.google.com/")]
          (is (= 266860 (:total-bytes result)) "Total bytes remain the same with both filters")
          (is (= {"/%D0%BE%D0%BB%D0%B8%D0%BC%D0%BF%D0%B8%D0%B0%D0%B4%D0%B0-80-%D0%B2-%D0%A1%D0%A1%D0%A1%D0%A0-%D0%91%D1%80%D0%B5%D0%B6%D0%BD%D0%B5%D0%B2-%D0%BF%D0%B5%D1%80%D0%B5%D0%B4-%D1%81%D1%82%D0%B0%D0%B4%D0%B8%D0%BE%D0%BD%D0%BE%D0%BC-%D1%87%D0%B8%D1%82%D0%B0%D0%B5%D1%82/" 26428} ; 13214 * 2 from this referrer
                 (:bytes-by-url result)) "Bytes-by-url filters correctly")
          (is (= {"https://www.google.com/" 1} ; Only 1 unique URL matches both filters for this referrer
                 (:urls-by-referrer result)) "Urls-by-referrer filters correctly")))

      (testing "Handling non-existent URL filter"
        (let [result (hw/solution :url "/non-existent-url")]
          (is (= 266860 (:total-bytes result)))
          (is (= {} (:bytes-by-url result)))
          (is (= 10 (count (:urls-by-referrer result)))))) ; Referrer count remains unfiltered

      (testing "Handling non-existent referrer filter"
        (let [result (hw/solution :referrer "http://non-existent.com")]
          (is (= 266860 (:total-bytes result)))
          (is (= 16 (count (:bytes-by-url result)))) ; URL count remains unfiltered
          (is (= {} (:urls-by-referrer result))))))))

;; Note: To run these tests, ensure the 'otus-16/access.log.test' file exists.
;; Execute tests using Leiningen: lein test
