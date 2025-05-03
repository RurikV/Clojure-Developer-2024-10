(ns otus-16.test-solution
  (:require [otus-16.homework :as hw]))

(defn run-tests []
  (println "Running tests...")
  
  (println "\nTest 1: Calculate total bytes and metrics for all URLs and referrers")
  (let [result (hw/solution)]
    (println "Total bytes:" (:total-bytes result))
    (println "Bytes for URL '/':" (get (:bytes-by-url result) "/"))
    (println "Bytes for URL '/new/':" (get (:bytes-by-url result) "/new/"))
    (println "Unique URL count for referrer 'google.com':" (get (:urls-by-referrer result) "https://www.google.com/"))
    (println "Total unique URLs:" (count (:bytes-by-url result)))
    (println "Total unique referrers:" (count (:urls-by-referrer result))))
  
  (println "\nTest 2: Filter by specific URL")
  (let [result (hw/solution :url "/new/")]
    (println "Total bytes:" (:total-bytes result))
    (println "Bytes-by-url:" (:bytes-by-url result))
    (println "Urls-by-referrer count:" (count (:urls-by-referrer result))))
  
  (println "\nTest 3: Filter by specific referrer")
  (let [result (hw/solution :referrer "https://www.google.com/")]
    (println "Total bytes:" (:total-bytes result))
    (println "Bytes-by-url count:" (count (:bytes-by-url result)))
    (println "Urls-by-referrer:" (:urls-by-referrer result)))
  
  (println "\nTest 4: Filter by specific URL and referrer")
  (let [result (hw/solution :url "/%D0%BE%D0%BB%D0%B8%D0%BC%D0%BF%D0%B8%D0%B0%D0%B4%D0%B0-80-%D0%B2-%D0%A1%D0%A1%D0%A1%D0%A0-%D0%91%D1%80%D0%B5%D0%B6%D0%BD%D0%B5%D0%B2-%D0%BF%D0%B5%D1%80%D0%B5%D0%B4-%D1%81%D1%82%D0%B0%D0%B4%D0%B8%D0%BE%D0%BD%D0%BE%D0%BC-%D1%87%D0%B8%D1%82%D0%B0%D0%B5%D1%82/"
                            :referrer "https://www.google.com/")]
    (println "Total bytes:" (:total-bytes result))
    (println "Bytes-by-url:" (:bytes-by-url result))
    (println "Urls-by-referrer:" (:urls-by-referrer result)))
  
  (println "\nTest 5: Handling non-existent URL filter")
  (let [result (hw/solution :url "/non-existent-url")]
    (println "Total bytes:" (:total-bytes result))
    (println "Bytes-by-url:" (:bytes-by-url result))
    (println "Urls-by-referrer count:" (count (:urls-by-referrer result))))
  
  (println "\nTest 6: Handling non-existent referrer filter")
  (let [result (hw/solution :referrer "http://non-existent.com")]
    (println "Total bytes:" (:total-bytes result))
    (println "Bytes-by-url count:" (count (:bytes-by-url result)))
    (println "Urls-by-referrer:" (:urls-by-referrer result))))

(run-tests)