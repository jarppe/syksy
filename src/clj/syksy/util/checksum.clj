(ns syksy.util.checksum
  (:require [clojure.java.io :as io])
  (:import (java.util.zip Adler32)
           (java.nio ByteBuffer)
           (java.io InputStream)
           (java.nio.charset StandardCharsets)))

(defn make-hash ^Adler32 []
  (Adler32.))

(defn update-bytes
  (^Adler32 [^Adler32 hash ^bytes buf]
   (.update hash buf)
   hash)
  (^Adler32 [^Adler32 hash ^bytes buf start len]
   (.update hash buf start len)
   hash))

(defn update-byte-buffer ^Adler32 [^Adler32 hash ^ByteBuffer buf]
  (.update hash buf)
  hash)

(defn value ^long [^Adler32 hash]
  (.getValue hash))

(defn hash-input-stream ^long [^InputStream in]
  (let [hash (make-hash)
        buffer (byte-array 8192)]
    (loop [c (.read in buffer)]
      (when (pos? c)
        (update-bytes hash buffer 0 c)
        (recur (.read in buffer))))
    (value hash)))

(defn hash-string ^long [^String s]
  (-> (make-hash)
      (update-bytes (.getBytes s StandardCharsets/UTF_8))
      (value)))

(comment

  (with-open [in (-> "logback.xml" io/resource io/input-stream)]
    (hash-input-stream in))

  (-> (make-hash)
      (update-bytes (.getBytes "Hullo"))
      (update-bytes (.getBytes "world!"))
      (value)))
