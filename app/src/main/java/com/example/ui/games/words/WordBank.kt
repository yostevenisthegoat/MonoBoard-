package com.example.ui.games.words

data class ScrambleEntry(
    val word: String,
    val hint: String,
    val category: String
)

data class AnagramRack(
    val letters: String,
    val validWords: List<String>
)

data class TriviaQuestion(
    val category: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object WordBank {
    val fiveLetterTargets: List<String> = listOf(
        "ABOUT", "ABOVE", "ACTOR", "ACUTE", "ADMIT", "ADOPT", "ADULT", "AFTER", "AGAIN", "AGENT",
        "AGREE", "AHEAD", "ALARM", "ALBUM", "ALERT", "ALIEN", "ALIGN", "ALIKE", "ALIVE", "ALLOW",
        "ALONE", "ALONG", "ALTER", "AMBER", "AMONG", "ANGER", "ANGLE", "ANGRY", "APART", "APPLE",
        "APPLY", "ARENA", "ARGUE", "ARISE", "ARRAY", "ASIDE", "ASSET", "AUDIO", "AUDIT", "AVOID",
        "AWARD", "AWARE", "BADGE", "BAKER", "BASIC", "BASIS", "BEACH", "BEGAN", "BEGIN", "BEING",
        "BELOW", "BENCH", "BIRTH", "BLACK", "BLADE", "BLAME", "BLANK", "BLAST", "BLEND", "BLIND",
        "BLOCK", "BLOOM", "BOARD", "BOOST", "BOUND", "BRAIN", "BRAND", "BRAVE", "BREAD", "BREAK",
        "BRICK", "BRIEF", "BRING", "BROAD", "BROKE", "BROWN", "BUILD", "BUILT", "BUYER", "CABLE",
        "CALIF", "CAMEL", "CANAL", "CANDY", "CARGO", "CARRY", "CATCH", "CAUSE", "CHAIN", "CHAIR",
        "CHALK", "CHAMP", "CHART", "CHASE", "CHEAP", "CHECK", "CHESS", "CHEST", "CHIEF", "CHILD",
        "CHILL", "CHINA", "CHOSE", "CIVIL", "CLAIM", "CLASS", "CLEAN", "CLEAR", "CLICK", "CLOCK",
        "CLOSE", "CLOUD", "COACH", "COAST", "COULD", "COUNT", "COURT", "COVER", "CRAFT", "CRANE",
        "CRASH", "CREAM", "CRIME", "CRISP", "CROSS", "CROWD", "CROWN", "CURVE", "CYCLE", "DAILY",
        "DANCE", "DATED", "DEALT", "DEATH", "DEBUT", "DELAY", "DEPTH", "DOING", "DOUBT", "DOZEN",
        "DRAFT", "DRAMA", "DRAWN", "DREAM", "DRESS", "DRIFT", "DRILL", "DRINK", "DRIVE", "DROVE",
        "EAGER", "EARLY", "EARTH", "EIGHT", "ELITE", "EMPTY", "ENEMY", "ENJOY", "ENTER", "ENTRY",
        "EQUAL", "ERROR", "EVENT", "EVERY", "EXACT", "EXIST", "EXTRA", "FAITH", "FALSE", "FAULT",
        "FIBER", "FIELD", "FIFTH", "FIFTY", "FIGHT", "FINAL", "FIRST", "FIXED", "FLAME", "FLASH",
        "FLEET", "FLOOR", "FLUID", "FOCUS", "FORCE", "FORTH", "FORTY", "FORUM", "FOUND", "FRAME",
        "FRANK", "FRESH", "FRONT", "FRUIT", "FULLY", "FUNNY", "GIANT", "GIVEN", "GLASS", "GLOBE",
        "GLORY", "GRACE", "GRADE", "GRAIN", "GRAND", "GRANT", "GRAPE", "GRAPH", "GRASP", "GRASS",
        "GREAT", "GREEN", "GREET", "GROUP", "GROWN", "GUARD", "GUESS", "GUEST", "GUIDE", "HABIT",
        "HAPPY", "HEART", "HEAVY", "HENCE", "HORSE", "HOTEL", "HOUSE", "HUMAN", "IDEAL", "IMAGE",
        "INDEX", "INNER", "INPUT", "ISSUE", "JOINT", "JUDGE", "KNIFE", "KNOCK", "KNOWN", "LABEL",
        "LARGE", "LASER", "LATER", "LAUGH", "LAYER", "LEARN", "LEASE", "LEAST", "LEAVE", "LEGAL",
        "LEVEL", "LIGHT", "LIMIT", "LOCAL", "LOGIC", "LOOSE", "LOWER", "LUCKY", "LUNCH", "MAGIC",
        "MAJOR", "MAKER", "MARCH", "MATCH", "MAYBE", "MAYOR", "MEANT", "MEDAL", "MEDIA", "METAL",
        "METER", "MIGHT", "MINOR", "MINUS", "MIXED", "MODEL", "MONEY", "MONTH", "MORAL", "MOTOR",
        "MOUNT", "MOUSE", "MOUTH", "MOVIE", "MUSIC", "NERVE", "NEVER", "NEWLY", "NIGHT", "NOISE",
        "NORTH", "NOTED", "NOVEL", "NURSE", "OCEAN", "OFFER", "OFTEN", "ORDER", "OTHER", "OUGHT",
        "OUTER", "OWNER", "PAINT", "PANEL", "PAPER", "PARTY", "PEACE", "PHASE", "PHONE", "PHOTO",
        "PIECE", "PILOT", "PITCH", "PLACE", "PLAIN", "PLANE", "PLANT", "PLATE", "POINT", "POUND",
        "POWER", "PRESS", "PRICE", "PRIDE", "PRIME", "PRINT", "PRIOR", "PRIZE", "PROOF", "PROUD",
        "PROVE", "QUEEN", "QUICK", "QUIET", "QUITE", "RADIO", "RAISE", "RANGE", "RAPID", "RATIO",
        "REACH", "REACT", "READY", "REFER", "RIGHT", "RIVAL", "RIVER", "ROBOT", "ROGER", "ROMAN",
        "ROUGH", "ROUND", "ROUTE", "ROYAL", "RURAL", "SCALE", "SCENE", "SCOPE", "SCORE", "SENSE",
        "SERVE", "SEVEN", "SHALL", "SHAPE", "SHARE", "SHARP", "SHEET", "SHELF", "SHELL", "SHIFT",
        "SHINE", "SHIRT", "SHOCK", "SHOOT", "SHORT", "SHOWN", "SIGHT", "SINCE", "SKILL", "SLEEP",
        "SLIDE", "SMALL", "SMART", "SMILE", "SMITH", "SMOKE", "SOLID", "SOLVE", "SOUND", "SOUTH",
        "SPACE", "SPARE", "SPEAK", "SPEED", "SPEND", "SPENT", "SPLIT", "SPOKE", "SPORT", "SQUAD",
        "STACK", "STAFF", "STAGE", "STAKE", "STAND", "START", "STATE", "STEAM", "STEEL", "STICK",
        "STILL", "STOCK", "STONE", "STOOD", "STORE", "STORM", "STORY", "STRIP", "STUCK", "STUDY",
        "STUFF", "STYLE", "SUGAR", "SUITE", "SUPER", "SWEET", "TABLE", "TAKEN", "TASTE", "TEACH",
        "TEETH", "THANK", "THEIR", "THEME", "THERE", "THESE", "THICK", "THING", "THINK", "THIRD",
        "THOSE", "THREE", "THREW", "THROW", "TIGHT", "TIMER", "TITLE", "TODAY", "TOKEN", "TOPIC",
        "TOTAL", "TOUCH", "TOUGH", "TOWER", "TRACK", "TRADE", "TRAIN", "TRAIT", "TREAT", "TREND",
        "TRIAL", "TRIBE", "TRICK", "TRIED", "TRIES", "TRUCK", "TRULY", "TRUST", "TRUTH", "TWICE",
        "UNDER", "UNION", "UNITY", "UNTIL", "UPPER", "URBAN", "USAGE", "USUAL", "VALID", "VALUE",
        "VIDEO", "VIRUS", "VISIT", "VITAL", "VOICE", "WASTE", "WATCH", "WATER", "WHEEL", "WHERE",
        "WHICH", "WHILE", "WHITE", "WHOLE", "WHOSE", "WOMAN", "WORLD", "WORRY", "WORSE", "WORST",
        "WORTH", "WOULD", "WOUND", "WRITE", "WRONG", "WROTE", "YIELD", "YOUNG", "YOUTH"
    ).filter { it != "CALIF" }

    val scrambleEntries: List<ScrambleEntry> = listOf(
        ScrambleEntry("STRATEGY", "Careful long-term game plan", "Board"),
        ScrambleEntry("CHECKMATE", "Decisive winning move in chess", "Board"),
        ScrambleEntry("DOMINO", "Rectangular tile with divided pips", "Board"),
        ScrambleEntry("PUZZLE", "Challenge designed to test ingenuity", "Logic"),
        ScrambleEntry("SOLITAIRE", "Classic single-player card patience", "Cards"),
        ScrambleEntry("LABYRINTH", "Intricate network of winding paths", "Logic"),
        ScrambleEntry("MONOLITH", "Single massive upright stone block", "Design"),
        ScrambleEntry("GRAVITY", "Force pulling tokens down in Connect Four", "Science"),
        ScrambleEntry("CRYPTO", "Hidden cipher or coded message", "Logic"),
        ScrambleEntry("MANCALA", "Ancient seed-sowing count-and-capture game", "Board"),
        ScrambleEntry("NONOGRAM", "Picture logic grid using numeric clues", "Puzzle"),
        ScrambleEntry("MINIMAL", "Clean style stripped to essentials", "Design"),
        ScrambleEntry("BACKGAMMON", "24-point race game with two dice", "Board"),
        ScrambleEntry("CASTLING", "Special simultaneous king and rook move", "Chess"),
        ScrambleEntry("LIBERTY", "Empty adjacent intersection in Go", "Board"),
        ScrambleEntry("OFFLINE", "Functioning completely without network", "Tech"),
        ScrambleEntry("BISHOP", "Chess piece sliding diagonally", "Chess"),
        ScrambleEntry("STALEMATE", "Draw when no legal move remains", "Chess"),
        ScrambleEntry("ANAGRAM", "Word formed by rearranging letters", "Words"),
        ScrambleEntry("PATTERN", "Repeated visual or logical sequence", "Logic"),
        ScrambleEntry("SYMMETRY", "Balanced proportion across an axis", "Math"),
        ScrambleEntry("TRICK", "Round of four cards in Hearts or Spades", "Cards"),
        ScrambleEntry("FORTUNE", "Luck of the draw or dice roll", "General"),
        ScrambleEntry("HORIZON", "Line where earth meets sky", "Nature")
    )

    val anagramRacks: List<AnagramRack> = listOf(
        AnagramRack(
            letters = "MASTERI",
            validWords = listOf(
                "MASTER", "STREAM", "MATES", "STEAM", "TEAMS", "MEATS", "SMART", "RATES",
                "STARE", "TEARS", "TIMER", "ITEMS", "TRIES", "RATES", "MATE", "TEAM",
                "RATE", "STAR", "TIME", "ITEM", "REST", "EAST", "SEAT", "TRAM", "MIST"
            ).distinct()
        ),
        AnagramRack(
            letters = "PLASTER",
            validWords = listOf(
                "PLASTER", "PLATES", "STAPLE", "PLEATS", "PETALS", "PLATE", "PLEAT", "PETAL",
                "LEAST", "STALE", "SLATE", "STEAL", "TALES", "PARTS", "STRAP", "SPARE",
                "PEARS", "SPEAR", "LATE", "TALE", "SALE", "SEAL", "EAST", "PART", "PAST", "STAR"
            ).distinct()
        ),
        AnagramRack(
            letters = "CHARMED",
            validWords = listOf(
                "CHARMED", "MARCHED", "CHARM", "MARCH", "REACH", "CREAM", "DREAM", "ARMED",
                "HEARD", "SHADE", "CARE", "RACE", "MADE", "DAME", "HEAD", "READ", "DEAR",
                "CAME", "HARM", "ACHE", "EACH", "HERD", "CARD", "HARD"
            ).distinct()
        ),
        AnagramRack(
            letters = "GARDENS",
            validWords = listOf(
                "GARDENS", "DANGERS", "GARDEN", "DANGER", "RANGES", "GRAND", "GRADE", "RANGE",
                "ANGER", "SNARE", "EARNS", "READS", "DARE", "DEAR", "READ", "NEAR", "EARN",
                "SAND", "HAND", "SEND", "SAGE", "RAGE", "GEAR", "DEAN"
            ).distinct()
        ),
        AnagramRack(
            letters = "PAINTER",
            validWords = listOf(
                "PAINTER", "PERTAIN", "REPAINT", "PARENT", "RETAIN", "ENTRAP", "PAINT", "PRINT",
                "TRAIN", "INERT", "TAPER", "PART", "TRAP", "PAIR", "RAIN", "PAIN", "PINE",
                "RIPE", "TIER", "RATE", "TEAR", "NEAR", "EARN", "NEAT", "TAPE"
            ).distinct()
        )
    )

    val triviaQuestions: List<TriviaQuestion> = listOf(
        TriviaQuestion("Board Games", "In standard Chess, which piece can leap over other pieces?", listOf("Bishop", "Knight", "Rook", "Queen"), 1, "Only the Knight can jump over intervening pieces."),
        TriviaQuestion("Board Games", "How many points (triangles) are on a standard Backgammon board?", listOf("16", "20", "24", "32"), 2, "A Backgammon board has 24 narrow triangles called points."),
        TriviaQuestion("Board Games", "What is the maximum number of stones captured by a single stone in Go if surrounded orthogonally on an intersection?", listOf("2", "3", "4", "8"), 2, "An interior intersection has 4 orthogonal liberties."),
        TriviaQuestion("Board Games", "In Hearts, how many penalty points is the Queen of Spades worth?", listOf("5", "10", "13", "26"), 2, "The Queen of Spades carries 13 penalty points."),
        TriviaQuestion("Science", "What is the chemical symbol for Tungsten?", listOf("Tu", "Tg", "W", "Tn"), 2, "W comes from Wolfram, the mineral tungsten was extracted from."),
        TriviaQuestion("Science", "Which planet in our solar system has the highest surface temperature?", listOf("Mercury", "Venus", "Mars", "Jupiter"), 1, "Venus's dense CO2 atmosphere creates a runaway greenhouse effect."),
        TriviaQuestion("Science", "What is the approximate speed of light in a vacuum?", listOf("300,000 km/s", "150,000 km/s", "30,000 km/s", "1,080,000 km/s"), 0, "Light travels at roughly 299,792 km per second."),
        TriviaQuestion("Science", "Which blood cells carry oxygen using hemoglobin?", listOf("Platelets", "Leukocytes", "Erythrocytes", "Lymphocytes"), 2, "Erythrocytes are red blood cells."),
        TriviaQuestion("Science", "What particle determines the atomic number of an element?", listOf("Neutron", "Proton", "Electron", "Photon"), 1, "Atomic number equals the count of protons in the nucleus."),
        TriviaQuestion("History", "In what year did the Apollo 11 Moon landing take place?", listOf("1965", "1967", "1969", "1971"), 2, "Neil Armstrong and Buzz Aldrin landed on July 20, 1969."),
        TriviaQuestion("History", "Which ancient civilization built Machu Picchu?", listOf("Maya", "Aztec", "Inca", "Olmec"), 2, "Machu Picchu was built by the Inca Empire in the 15th century."),
        TriviaQuestion("History", "Who invented the movable-type printing press in Europe around 1440?", listOf("Johannes Gutenberg", "Galileo Galilei", "Martin Luther", "Leonardo da Vinci"), 0, "Gutenberg introduced mechanical movable-type printing to Europe."),
        TriviaQuestion("History", "The Rosetta Stone helped scholars decipher which script?", listOf("Cuneiform", "Egyptian Hieroglyphs", "Linear B", "Sanskrit"), 1, "The trilingual stele unlocked Egyptian hieroglyphs."),
        TriviaQuestion("Geography", "Which river flows through Cairo?", listOf("Tigris", "Euphrates", "Nile", "Congo"), 2, "Cairo sits along the Nile River near its delta."),
        TriviaQuestion("Geography", "Which country has the longest coastline in the world?", listOf("Russia", "Indonesia", "Canada", "Australia"), 2, "Canada's vast Arctic archipelago gives it over 202,000 km of coastline."),
        TriviaQuestion("Geography", "Mount Kilimanjaro is located in which country?", listOf("Kenya", "Tanzania", "Uganda", "Ethiopia"), 1, "Kilimanjaro stands in northeastern Tanzania."),
        TriviaQuestion("Geography", "What is the smallest independent country by land area?", listOf("Monaco", "San Marino", "Vatican City", "Liechtenstein"), 2, "Vatican City covers roughly 0.49 square kilometers."),
        TriviaQuestion("Geography", "Which strait separates Europe from Africa at its narrowest point?", listOf("Bering Strait", "Strait of Hormuz", "Strait of Gibraltar", "Strait of Malacca"), 2, "The Strait of Gibraltar connects the Atlantic and Mediterranean."),
        TriviaQuestion("Logic & Math", "What is the only even prime number?", listOf("0", "2", "4", "6"), 1, "2 is divisible only by 1 and itself."),
        TriviaQuestion("Logic & Math", "In Nim, a player has a winning strategy if the XOR sum of all heaps is what?", listOf("Zero", "Non-zero", "Odd", "Prime"), 1, "Non-zero Nim-sum lets the active player force Nim-sum to zero."),
        TriviaQuestion("Logic & Math", "How many faces does an icosahedron have?", listOf("12", "16", "20", "24"), 2, "A regular icosahedron is a 20-faced Platonic solid."),
        TriviaQuestion("Logic & Math", "What is the sum of interior angles of a pentagon?", listOf("360°", "450°", "540°", "720°"), 2, "(5 - 2) × 180° = 540°."),
        TriviaQuestion("Technology", "What does 'HTTP' stand for?", listOf("HyperText Transfer Protocol", "High Transmission Text Port", "Hyperlink Terminal Process", "Host Transfer Type Protocol"), 0, "HTTP is HyperText Transfer Protocol."),
        TriviaQuestion("Technology", "Which open-source kernel was created by Linus Torvalds in 1991?", listOf("FreeBSD", "Darwin", "Linux", "Minix"), 2, "Linus Torvalds released the first Linux kernel in 1991."),
        TriviaQuestion("Technology", "How many bits make up one byte?", listOf("4", "8", "16", "32"), 1, "A standard byte consists of 8 bits."),
        TriviaQuestion("Arts", "Who painted 'The Starry Night' in 1889?", listOf("Claude Monet", "Pablo Picasso", "Vincent van Gogh", "Paul Cézanne"), 2, "Van Gogh painted The Starry Night while at Saint-Rémy-de-Provence."),
        TriviaQuestion("Arts", "How many strings does a standard concert violin have?", listOf("4", "5", "6", "8"), 0, "A violin is tuned G–D–A–E across 4 strings."),
        TriviaQuestion("Arts", "Which architectural movement emphasized 'less is more' and clean geometry?", listOf("Baroque", "Rococo", "Minimalism / Bauhaus", "Gothic"), 2, "Bauhaus and Minimalism championed functional geometry."),
        TriviaQuestion("Board Games", "How many squares are on a standard Chessboard?", listOf("49", "64", "81", "100"), 1, "An 8×8 grid contains 64 squares."),
        TriviaQuestion("Board Games", "In standard Ludo, what die roll is needed to move a token out of the home yard?", listOf("1", "4", "5", "6"), 3, "Rolling a 6 deploys a token onto the starting square."),
        TriviaQuestion("Science", "Which gas makes up roughly 78% of Earth's atmosphere?", listOf("Oxygen", "Carbon Dioxide", "Nitrogen", "Argon"), 2, "Earth's atmosphere is ~78% nitrogen and ~21% oxygen."),
        TriviaQuestion("Science", "What is the hardest naturally occurring mineral on the Mohs scale?", listOf("Quartz", "Topaz", "Corundum", "Diamond"), 3, "Diamond scores 10 on the Mohs hardness scale.")
    )
}
