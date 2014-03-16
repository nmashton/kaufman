(ns kaufman.kludges
  (:use [cheshire.core])
  (:use [clojure.string :only [split join replace]]))

;; This is where we do the work.

(def thedata (parse-string (slurp "./data/kaufman-relational.json")))

(def narrow (set ["AKA" "AWA" "AWA (o)" "AWA (u)" "CHJ" "CHL" "CHR" "CHT" "Col 
Kaq" "ColYUK" "EpM" "EpM(?Yu)" "EpM(Ch)" "EpM(Yu)" "ITZ" "IXL" "IXL (o)" "IXL (r 
)" "IXLne" "KAB" "KAQ" "KAQ*" "KAQc" "KAQcol" "KAQi" "KAQp" "KCH" "KCH (r)" "KCH 
 (u)" "KCH*" "KCHa" "KCHc" "KCHk" "KCHn" "KCHp" "KCHq" "LAK" "MAM" "MAM*" "MAMc" 
 "MAMi" "MAMn" "MAMo" "MAMs" "MAMt" "MCH" "MOP" "PCH" "PCH (i)" "PCH (r)"  
"PCHcri" "POP" "PQM" "PQMj" "PQMp" "QAN" "QEQ" "QEQ (r)" "QEQ*" "QEQc&l" " 
QEQcah" "QEQcha" "QEQe" "QEQl&c" "QEQlan" "QEQw" "QEQw/e" "SAK" "SIP" "TEK" "TOJ 
" "TOJ (a)" "TUZ" "TZE" "TZE*" "TZEb" "TZEn" "TZEs" "TZO" "TZOs" "TZOz" "TZU" "T 
ZUp" "Tze" "Tzo" "USP" "WAS" "WAS*" "WASc" "WASw" "YOK" 
 "YUK" "YUK (u)" "YUK*"]))

(def langkey (into {}
	[["AKA" 	"Akateco"] 
	["AWA" 		"Awakateko"]
	["AWA (o)" 	"Awakateko"]
	["AWA (u)" 	"Awakateko"]
	["CHJ" 		"Chuj"] 
	["CHL" 		"Ch'ol"] 
	["CHR" 		"Ch'orti7"]
	["CHT" 		"Ch'olti7"]
	["ColKaq" 	"Kaqchikeel"]
	["ColYUK" 	"Yukateko"] 
	["EpM" 		"Epigraphic Mayan"]
	["EpM(?Yu)" "Epigraphic Mayan"]
	["EpM(Ch)" 	"Epigraphic Mayan"] 
	["EpM(Yu)" 	"Epigraphic Mayan"]
	["ITZ" 		"Itzaj"]
	["IXL" 		"Ixil"]
	["IXL (o)" 	"Ixil"]
	["IXL (r)" 	"Ixil"]
	["IXLne" 	"Ixil"]
	["KAB" 		"Kabil"]
	["KAQ" 		"Kaqchikeel"] 
	["KAQ*" 	"Kaqchikeel"]
	["KAQc" 	"Kaqchikeel"]
	["KAQcol" 	"Kaqchikeel"]
	["KAQi" 	"Kaqchikeel"]
	["KAQp" 	"Kaqchikeel"]
	["KCH" 		"K'iche7"]
	["KCH (r)" 	"K'iche7"] 
	["KCH (u)" 	"K'iche7"] 
	["KCH*" 	"K'iche7"] 
	["KCHa" 	"K'iche7"]
	["KCHc" 	"K'iche7"]
	["KCHk" 	"K'iche7"]
	["KCHn" 	"K'iche7"]
	["KCHp" 	"K'iche7"] 
	["KCHq" 	"K'iche7"]
	["LAK" 		"Lakantun"]
	["MAM" 		"Mam"] 
	["MAM*" 	"Mam"] 
	["MAMc" 	"Mam"] 
	["MAMi" 	"Mam"] 
	["MAMn" 	"Mam"] 
	["MAMo" 	"Mam"] 
	["MAMs" 	"Mam"]
	["MAMt" 	"Mam"]
	["MCH" 		"Mochó"] 
	["MOP" 		"Mopan"] 
	["PCH" 		"Poqomchii7"] 
	["PCH (i)" 	"Poqomchii7"] 
	["PCH (r)" 	"Poqomchii7"]
	["PCHcri" 	"Poqomchii7"] 
	["POP" 		"Popti7"] 
	["PQM" 		"Poqomam"] 
	["PQMj" 	"Poqomam"] 
	["PQMp" 	"Poqomam"]
	["QAN" 		"Q'anjob'al"]
	["QEQ" 		"Q'eqchi7"] 
	["QEQ (r)" 	"Q'eqchi7"]
	["QEQ*" 	"Q'eqchi7"]
	["QEQc&l" 	"Q'eqchi7"] 
	["QEQcah" 	"Q'eqchi7"] 
	["QEQcha" 	"Q'eqchi7"] 
	["QEQe" 	"Q'eqchi7"] 
	["QEQl&c" 	"Q'eqchi7"] 
	["QEQlan" 	"Q'eqchi7"]
	["QEQw" 	"Q'eqchi7"]
	["QEQw/e" 	"Q'eqchi7"]
	["SAK" 		"Sakapulteko"]
	["SIP" 		"Sipakapense"]
	["TEK" 		"Teko"]
	["TOJ" 		"Tojol 7ab'al"]
	["TOJ (a)" 	"Tojol 7ab'al"]
	["TUZ" 		"Tuzanteco"]
	["TZE" 		"Tzeltal"] 
	["TZE*" 	"Tzeltal"] 
	["TZEb" 	"Tzeltal"] 
	["TZEn" 	"Tzeltal"] 
	["TZEs" 	"Tzeltal"] 
	["TZO" 		"Tzotzil"] 
	["TZOs" 	"Tzotzil"] 
	["TZOz" 	"Tzotzil"] 
	["TZU" 		"Tz'utujiil"] 
	["TZUp" 	"Tz'utujiil"] 
	["Tze" 		"Tzeltal"]
	["Tzo" 		"Tzotzil"] 
	["USP" 		"Uspanteko"] 
	["WAS" 		"Wasteko"] 
	["WAS*" 	"Wasteko"] 
	["WASc" 	"Wasteko"]
	["WASw" 	"Wasteko"] 
	["YOK" 		"Yokot'an"] 
	["YUK" 		"Yukateko"] 
	["YUK (u)" 	"Yukateko"] 
	["YUK*" 	"Yukateko"]]))

(defn handle-epigraphic [x]
  (re-find #"(?<=/).+(?=/)" x))

(defn epi-okay? [x]
  (let [l (get x "language")]
    (or
      (not (seq (#{"EpM" "EpM(?Yu)" "EpM(Ch)" "EpM(Yu)"} l)))
      (handle-epigraphic (get x "lexeme")))))

(defn ipa-ize [s]
  (-> s
    (split #"[,~>]")
    (first)
    (replace "7" "ʔ")
    (replace "?" "")
    (replace "b'" "ɓ")
    (replace "'" "ˀ")
    (replace "H" "h")
    (replace " from some GTz lankua" "") 
    (replace "+ animal name" "")
    (replace #" \(.+\)" "")
    (replace #" \[.+\]" "")
    (replace #"\{.+\}" "")
;    (replace "A" "a")
    (replace "g" "k")
    (replace "b" "ɓ")
    (replace "j" "χ")
    (replace "y" "j")
    (replace "x" "ʃ")
    (replace "ch" "ʧ")
    (replace "CH" "ʧ")
    (replace "c" "k")
    (replace "tz" "ʦ")
    (replace "a\"" "ʌ")
    (replace "o\"" "ø")
    (replace "u\"" "y")
    (replace "i\"" "i")
    (replace "e\"" "e")
    (replace "@" "ʌ")
    (replace ":" "ː")
    (replace "aa" "aː")
    (replace "ee" "eː")
    (replace "oo" "oː")
    (replace "uu" "uː")
    (replace "ii" "iː")
    (replace #"[A-Z]+" "")
    (replace #"[+-=#*()\[\]\./]" "")))

(defn fix-gloss [s]
  (replace s #"[!\"#$%&'\(\)\*+,-./:\;<=>?@\[\]^_`{|}~]" "")) 	;"

(def uncorrupt-data
  (->> thedata
    (filter #(narrow (get % "language")))
    (map #(update-in % ["language"] langkey))
    (filter #(seq (get % "lexeme")))
    (map #(update-in % ["lexeme"] ipa-ize))
    (filter #(seq (get % "lexeme")))
    (map #(update-in % ["gloss"] fix-gloss))
    (filter epi-okay?)))
           
(def blocked-data
  (->> uncorrupt-data
    (group-by #(get % "cognate-block"))
    (map (fn [[k v]] [k (group-by (fn [y] (get y "language")) v)]))))

(def the-languages (sort (keys (group-by #(get % "language") uncorrupt-data))))

(defn process-langed-cognate-set [cogs f]
  (let [initcases (map #(get cogs %) the-languages)
        fst (first (first (filter seq initcases)))
      	i (get fst "cognate-block")
      	word (get fst "gloss")]
  	(loop [cases initcases
           acc []]
      (if (apply = nil cases) acc
	    (recur (map next cases)
	           (conj acc (f i word (map first cases))))))))

(defn handle-row [i word xs]
  (let [lexemes (map #(get % "lexeme") xs)
        no (re-find (re-pattern "\\d+") i)
        lexid (flatten (map #(vector (if % % "-") (if % no 0)) lexemes))]
    (join "\t" (concat [no word] lexid))))

(def blocks (map second blocked-data))

(def output-data
  (->> blocks
    (map #(process-langed-cognate-set % handle-row))
    (map #(join "\n" %))
    (join "\n")
    (str "ID\tWords\t" (join "\t" (flatten (map #(seq [% "COG"]) the-languages))) "\n")))
