(ns javamatic.core
  (:require [clojure.contrib.str-utils :as str1]
            [clojure.contrib.str-utils2 :as str2])
  (:use [clojure.contrib.str-utils2 :only [lower-case]])
  (:import [java.awt Toolkit]
           [java.awt.datatransfer StringSelection]))

(defn copy
  "Copy passed string into clipboard."
  [s]
  (let [ss (StringSelection. s)]
    (.setContents
     (.getSystemClipboard (Toolkit/getDefaultToolkit)) ss ss)
    s))

(defn placeholder?
  "Tests whether the passed string is a template placeholder."
  [s]
  (re-find #"^\{\{" s))

(defn eval-placeholder?
  "Tests whether the passed string is a template placeholder that
  needs to be evaluated."
  [s]
  (re-find #"^\{\{" s))

;;;;;;;;;;;

(defn capitalize
  "Capitalize the first letter of a string, but leave the rest untouched."
  [s]
  (str (str2/capitalize (str2/take s 1)) (str2/drop s 1)))

(defn add-spaces
  "Seperate camel-case with spaces."
  [s]
  (str2/trim (str2/replace s #"[A-Z]" #(str " " %))))

;;;;;;;;;;;

(defn first-alpha
  "Get all the characters at the beggining of the string up to the
  first non-alphanumeric character."
  [s]
  (let [m (re-find #"^\s*[A-Za-z0-9]+" s)]
    (when m (str2/trim m))))

(defn first-alphas
  "Split the passed string at new lines and apply get-first-alpha to
  each line."
  [s]
  (remove nil? (map get-first-alpha (str2/split s #"\n"))))

(defn name-from-declaration
  "Extract the variable name from a single Java declaration."
  [s]
  (second (remove (apply hash-set
                         "" (qw public protected private static volatile transient final))
                  (str2/split s #"\s+"))))

(defn names-from-declarations
  "Split the passed string at new lines and apply
  name-from-declaration to each line."
  [s]
  (remove nil? (map name-from-declaration (str2/split s #"\n"))))

;;;;;;;;;;;

(defmacro qw
  "Constructs a vector of the names (strings) of the passed symbols.
  This is to save you typing unneccesary quotes. Stolen from Perl.

  Example: (qw \"first name\" surname address)"
  [& words]
  `(vector ~@(map name words)))

(def x nil)
(defn eval-placeholder [placeholder value]
  (let [code (read-string (str2/butlast (str2/drop placeholder 2) 2))]
    (binding [x value]
      (eval code))))

(defn render-template-single [t x]
  (apply str
         (map #(cond (eval-placeholder? %) (eval-placeholder % x)
                     (placeholder? %) x
                     :else %)
              (str1/re-partition #"\{\{.+?\}\}" t))))

(defn render-template [t values]
  (apply str
         (map #(render-template-single t %) values)))

(copy (render-template
        "set{{x}}(\"{{(add-spaces x)}}\");\n"
        (qw FirstName Surname Address Email)))












