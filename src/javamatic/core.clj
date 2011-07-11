(ns javamatic.core
  (:require [clojure.contrib.str-utils :as str1]
            [clojure.contrib.str-utils2 :as str2])
  (:use [clojure.contrib.str-utils2 :only [lower-case upper-case]])
  (:import [java.awt GridBagConstraints Insets Dimension Font]
           [java.awt Toolkit]
           [java.awt.datatransfer StringSelection]
           [javax.swing JLabel JOptionPane]))

(def placeholder-re #"\{\{\s*.+?\s*\}\}")
(def placeholder-capture-re #"\{\{\s*(.+?)\s*\}\}")

(defmacro qw
  "Constructs a vector of the names (strings) of the passed symbols.
  This is to save you typing unneccesary quotes. Stolen from Perl.

  Example: (qw \"first name\" surname address)"
  [& words]
  `(vector ~@(map name words)))

(defmacro on-action [component event & body]
  `(. ~component addActionListener
      (proxy [java.awt.event.ActionListener] []
        (actionPerformed [~event] ~@body))))

(defmacro set-grid! [constraints field value]
  `(set! (. ~constraints ~(symbol (name field)))
         ~(if (keyword? value)
            `(. java.awt.GridBagConstraints
                ~(symbol (name value)))
            value)))

(defmacro grid-bag-layout [container & body]
  (let [c (gensym "c")
        cntr (gensym "cntr")]
    `(let [~c (new java.awt.GridBagConstraints)
           ~cntr ~container]
       ~@(loop [result '() body body]
           (if (empty? body)
             (reverse result)
             (let [expr (first body)]
               (if (keyword? expr)
                 (recur (cons `(set-grid! ~c ~expr
                                          ~(second body))
                              result)
                        (next (next body)))
                 (recur (cons `(.add ~cntr ~expr ~c)
                              result)
                        (next body)))))))))

(defn copy
  "Copy passed string into clipboard."
  [s]
  (let [ss (StringSelection. s)]
    (.setContents
     (.getSystemClipboard (Toolkit/getDefaultToolkit)) ss ss)
    s))

(defn pastebox []
  (let [font (Font. "Consolas" Font/PLAIN 12)
        text-area (javax.swing.JTextArea.)
        text-field (javax.swing.JTextField. "source")]
    (doto (javax.swing.JFrame.)
      (.setTitle "javamatic pastebox")
      (.setLayout (java.awt.GridBagLayout.))
      (grid-bag-layout
       :fill :HORIZONTAL
       :insets (Insets. 5 5 5 5)
       :gridx 0 :gridy 0
       :gridwidth 4
       (doto (JLabel. " javamatic pastebox")
         (.setFont (Font. "SansSerif" Font/BOLD 22)))
       :gridy 1
       :gridwidth 1
       (javax.swing.JLabel. "var")
       :gridx 1
       :weightx 1
       (doto text-field
         (.setFont font)
         (.setCaretPosition (count (.getText text-field))))
       :gridx 2
       :weightx 0
       (doto (javax.swing.JButton. "intern")
         (on-action e
                    (let [var-sym (symbol (.trim (.getText text-field)))
                          the-ns (symbol
                                  (if (namespace var-sym)
                                    (namespace var-sym)
                                    'javamatic.core))]
                      ;(JOptionPane/showMessageDialog
                      ; text-area (str "Interning " (name var-sym) " into ns " the-ns))
                      (intern
                       the-ns
                       var-sym
                       (.getText text-area)))))
       :gridx 3
       (doto (javax.swing.JButton. "copy literal")
         (on-action e
                    (copy (str2/replace
                           (prn-str (.getText text-area))
                           #"\\n"
                           "\n"))))
       :gridx 0, :gridy 2
       :weightx 1, :weighty 1
       :fill :BOTH
       :gridwidth 4
       (javax.swing.JScrollPane.
        (doto text-area
          (.setFont font))))
      (.pack)
      (.setSize (Dimension. 800 600))
      (.setLocationRelativeTo nil)
      (.setVisible true))))

;;;;; end of pastebox ;;;;;

(defn value-map-to-records [the-map]
  (let [first-seq (second (first the-map))]
    (loop [current-map the-map
           records []
           s first-seq]
      (if (seq s)
        (recur
         (into {} (map (fn [[k [v & vs]]] [k vs]) current-map)) ;;current-map
         (conj records
               (into {} (map (fn [[k [v & vs]]] [k v]) current-map))) ;;records
         (next s)) ;;s
        records))))

(defn placeholder?
  "Tests whether the passed string is a template placeholder."
  [s]
  (re-find #"^\{\{" s))

(defn eval-placeholder?
  "Tests whether the passed string is a template placeholder that
  needs to be evaluated."
  [s]
  (re-find #"^\{\{\(" s))

;;;;; string manipulation ;;;;;;

(defn split
  "Split string on whitespace"
  [s]
  (str2/split s #"\s+"))

(defn capitalize
  "Capitalize the first letter of a string, but leave the rest untouched."
  [s]
  (str (str2/capitalize (str2/take s 1)) (str2/drop s 1)))

(defn first-lower
  "Convert the first letter of a string to lower case, but leave the
  rest untouched."
  [s]
  (str (str2/lower-case (str2/take s 1)) (str2/drop s 1)))

(defn add-spaces
  "Seperate camel-case with spaces."
  [s]
  (str2/trim (str2/replace s #"[A-Z]" #(str " " %))))

(defn camel-case
  "Concat a collection of strings into a camel-case string."
  [xs]
  (apply str (map capitalize xs)))

(defn CamelCase
  "Concat a collection of strings into a camel-case string (first
  letter will be upper case)."
  [xs]
  (camel-case xs))

(defn camelCase
  "Concat a collection of strings into a camel-case string (first
  letter will be lower case)."  
  [xs]
  (first-lower (camel-case xs)))

(defn reorder
  "Split string at whitespace and re-order the pieces according to the passed indices.

  Example: (reorder \"public name\" [1 0])
           \"name public\""
  [s indices]
  (let [pieces (split s)]
    (str2/join " " (map #(nth pieces %) indices))))

(defn reorder-camel
  "Reorder the pieces of a camel-cased string according to the passed
  indices. The original case of the first character is retained in the
  resulting string.

  Example: (reorder-camel \"publicName\" [1 0])
           \"namePublic\""
  [s indices]
  (let [pieces (split (reorder (add-spaces s) indices))]
    (if (Character/isLowerCase (first s)) (camelCase pieces)
        (CamelCase pieces))))

;;;;; input processing ;;;;;;

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
  (remove nil? (map first-alpha (str2/split s #"\n"))))

(defn name-from-declaration
  "Extract the variable name from a single Java declaration."
  [s]
  (when (not (empty? (.trim s)))
    (first-alpha
      (second (remove
                (apply hash-set
                  "" (qw public protected private static volatile transient final))
                (str2/split s #"\s+"))))))

(defn names-from-declarations
  "Split the passed string at new lines and apply
  name-from-declaration to each line."
  [s]
  (remove nil? (map name-from-declaration (str2/split s #"\n"))))

;;;;; templates ;;;;;;

(defn let-eval
  "Eval the code within a let using the passed bindings. Note that
  this is not a macro, so the symbols of the bindings have to be
  quoted like so: ['a 5 'b 3]."
  [bindings & code]
  (eval `(let ~bindings ~@code)))

(defn eval-placeholder [placeholder record]
  (let [values (into [] (flatten
                         (map
                          (fn [x] [(symbol (name (first x))) (second x)]) record)))
        code (read-string (str2/butlast (str2/drop placeholder 2) 2))]
    (try (let-eval values code)
         (catch Exception e placeholder))))

(defn placeholder-var [placeholder]
  {:pre [placeholder? placeholder]}
  (second (re-find placeholder-capture-re placeholder)))

(defn replace-placeholder [placeholder record]
  (let [key (keyword (placeholder-var placeholder))]
    (if (contains? record key)
      (get record key)
      placeholder)))

(defn render-template-single [template record]
  (apply str
         (map (fn [token]
                (cond (eval-placeholder? token) (eval-placeholder token record)
                      (placeholder? token) (replace-placeholder token record)
                      :else token))
                (str1/re-partition placeholder-re template))))

(defn render-template [t values]
  {:pre [(string? t),
         (or (map? values) (sequential? values))]}
  (let [v (if (map? values) values {:x values})]
    (apply str
           (map #(render-template-single t %) (value-map-to-records v)))))

;;example
;(copy (render-template
;        "set{{x}}(\"{{(upper-case x)}}\");\n"
;        (qw FirstName Surname Address Email)))

;(print (copy (render-template
;              "this.set{{x}}(other.get{{x}});\n"
;              (qw FirstName Surname Email
;                  DayTelephone MobileTelephone))))
