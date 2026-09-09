package com.umair.smarttodo.data.categorizer

import com.umair.smarttodo.domain.Category

/**
 * A single scoring rule: one *concept* (e.g. "medicine") together with every Roman-script
 * spelling of it we are willing to recognise.
 *
 * @param category the bucket this concept votes for.
 * @param weight how diagnostic the concept is, see [CategoryKeywords.STRONG] and friends.
 * @param concept human-readable name, used only for readability and test failure messages.
 * @param variants raw (unfolded) spellings; a variant containing spaces is an n-gram and is
 *   matched against consecutive input tokens.
 */
internal data class KeywordRule(
    val category: Category,
    val weight: Int,
    val concept: String,
    val variants: Set<String>,
)

/** One category's contribution from a matched keyword. */
internal data class CategoryVote(val category: Category, val weight: Int)

/**
 * The keyword knowledge base behind [RuleBasedTaskCategorizer].
 *
 * Everything the categorizer "knows" lives here so that extending it is a data edit rather
 * than a code change. Input is expected in Roman script only: English, Hinglish or Urdulish.
 *
 * ### Weighting
 * Concepts are weighted by how strongly they pin down a category on their own. `gym` or
 * `bijli` are decisive; `lena` ("to take") or `call` merely lean. Scores from all matched
 * concepts are summed per category.
 *
 * ### Spelling variants
 * Each rule lists the spellings a user actually types. On top of that, both these variants
 * and the user input pass through [TextNormalizer.fold], which absorbs the mechanical
 * differences (`jaana`/`jana`, `dawaai`/`davai`, `naukri`/`nokri`), so the lists stay
 * readable instead of combinatorial.
 *
 * ### Precedence
 * [PRECEDENCE] breaks score ties by an explicit, fixed order rather than by map iteration
 * order, which would not be stable across Kotlin or JDK versions.
 */
internal object CategoryKeywords {

    /** Decisive on its own - the word essentially only occurs in this category. */
    const val STRONG = 3

    /** Suggestive - usually this category, but plausible elsewhere. */
    const val MEDIUM = 2

    /** Weak lean - only meaningful when nothing stronger is present. */
    const val WEAK = 1

    /**
     * Reserved for short disambiguating phrases that must beat the individual words they
     * contain, e.g. "dawai leni" (buy medicine) outranking the medicine concept.
     */
    const val PHRASE = 4

    /**
     * Tie-break order, applied when two categories end on the same score.
     *
     * Ordered from the narrowest vocabulary to the broadest. A word like `react` or `qist`
     * belongs to a small, unambiguous domain, so if it ties with something from a sprawling
     * bucket such as Work or Personal the specific reading is far more likely to be what the
     * user meant. Personal is last because it is effectively the human catch-all, and
     * [Category.OTHER] is not a candidate at all - it is only returned when nothing scored.
     */
    val PRECEDENCE: List<Category> = listOf(
        Category.CODING,
        Category.FINANCE,
        Category.HEALTH_FITNESS,
        Category.STUDY,
        Category.SHOPPING,
        Category.WORK,
        Category.PERSONAL,
    )

    private fun rule(
        category: Category,
        weight: Int,
        concept: String,
        vararg variants: String,
    ) = KeywordRule(category, weight, concept, variants.toSet())

    // ---------------------------------------------------------------------------------
    // WORK
    // ---------------------------------------------------------------------------------
    private val work: List<KeywordRule> = listOf(
        rule(Category.WORK, STRONG, "office", "office", "offis", "ofis", "ofice", "daftar", "daftr", "dafter", "daftur"),
        rule(Category.WORK, STRONG, "meeting", "meeting", "miting", "meting", "meetings", "mtg", "standup", "stand up", "scrum", "huddle"),
        rule(Category.WORK, STRONG, "client", "client", "clients", "klient", "klaint", "customer", "customers"),
        rule(Category.WORK, STRONG, "presentation", "presentation", "presentaion", "prezentation", "presentations", "ppt", "slides", "slide deck", "deck banana"),
        rule(Category.WORK, STRONG, "boss", "boss", "bos", "manager", "menejar", "supervisor", "team lead"),
        rule(Category.WORK, STRONG, "report", "report", "reports", "riport", "rapot", "reporting"),
        rule(Category.WORK, MEDIUM, "periodic report", "quarterly report", "monthly report", "weekly report", "status report", "quarterly"),
        rule(Category.WORK, STRONG, "deadline", "deadline", "dedline", "deadlines"),
        rule(Category.WORK, STRONG, "job", "naukri", "nokri", "naukari", "nokari", "job", "jaab", "employment", "resign", "istifa", "istefa"),
        rule(Category.WORK, STRONG, "interview", "interview", "intervyu", "interviews", "hiring", "recruiter"),
        rule(Category.WORK, MEDIUM, "email", "email", "emails", "e mail", "inbox", "mail bhejna", "mail karna"),
        rule(Category.WORK, MEDIUM, "project plan", "roadmap", "milestone", "kickoff", "kick off", "stakeholder"),
        rule(Category.WORK, MEDIUM, "colleague", "colleague", "collegue", "coworker", "team", "teem", "department", "shoba"),
        rule(Category.WORK, MEDIUM, "conference", "conference", "confrence", "conf call", "webinar", "seminar"),
        rule(Category.WORK, MEDIUM, "shift", "shift", "duty", "duti", "overtime", "roster", "attendance", "hazri"),
        rule(Category.WORK, MEDIUM, "proposal", "proposal", "contract", "agreement", "muahida", "tender"),
        rule(Category.WORK, MEDIUM, "minutes", "minutes", "agenda", "follow up", "followup", "escalate"),
        rule(Category.WORK, WEAK, "work", "kaam", "kam", "work", "working", "workplace"),
        rule(Category.WORK, WEAK, "call", "call", "phone karna", "phone karni", "dial"),
        rule(Category.WORK, WEAK, "send", "bhejna", "bhejni", "bhejne", "forward karna"),
    )

    // ---------------------------------------------------------------------------------
    // PERSONAL
    // ---------------------------------------------------------------------------------
    private val personal: List<KeywordRule> = listOf(
        rule(
            Category.PERSONAL, STRONG, "family",
            "ammi", "ami", "ammijan", "amma", "walida", "abbu", "abu", "abba", "walid",
            "mom", "mummy", "dad", "papa", "father", "mother", "parents",
            "bhai", "bhaiya", "bhaijan", "behen", "bahen", "bahin", "baji", "apa", "aapa",
            "brother", "sister", "beta", "beti", "bacha", "bache", "bachay", "bachon",
            "biwi", "shohar", "wife", "husband", "khala", "chacha", "mamu", "phupo",
            "dada", "dadi", "nana", "nani", "khandan", "family", "ghar wale",
        ),
        rule(Category.PERSONAL, STRONG, "home", "ghar", "gher", "home", "house", "kamra", "lawn", "bagicha"),
        rule(Category.PERSONAL, STRONG, "friends", "dost", "dosto", "doston", "friend", "friends", "yaar", "buddy"),
        rule(Category.PERSONAL, STRONG, "celebration", "shadi", "shaadi", "wedding", "birthday", "bday", "salgirah", "salgira", "mehndi", "barat", "walima", "dawat", "party", "eid", "anniversary"),
        rule(Category.PERSONAL, STRONG, "chores", "safai", "safaai", "cleaning", "laundry", "dhulai", "kapre dhone", "bartan", "jhaaru", "jharu", "kachra", "kooda", "trash", "garbage", "dusting"),
        rule(Category.PERSONAL, MEDIUM, "cooking", "khana banana", "khana pakana", "pakana", "cook", "cooking", "recipe"),
        rule(Category.PERSONAL, MEDIUM, "travel", "passport", "visa", "ticket", "tickets", "flight", "hotel", "booking", "book karna", "book karni", "trip", "safar", "vacation", "holiday", "chutti", "chuti", "chhutti"),
        rule(Category.PERSONAL, MEDIUM, "grooming", "haircut", "hair cut", "baal katwana", "salon", "barber", "hajaam", "parlour", "parlor"),
        rule(Category.PERSONAL, MEDIUM, "visit", "milne jana", "milne jaana", "visit", "guests", "mehmaan", "mehman", "invite", "invitation"),
        rule(Category.PERSONAL, MEDIUM, "religious", "namaz", "masjid", "quran", "roza", "umrah", "hajj", "dua"),
        rule(Category.PERSONAL, MEDIUM, "documents", "cnic", "shanakhti card", "id card", "license", "licence", "renew karna", "form jama"),
        rule(Category.PERSONAL, MEDIUM, "vehicle", "gaari", "gari", "car wash", "petrol", "fuel", "oil change", "tyre", "puncture"),
        rule(Category.PERSONAL, WEAK, "message", "message", "msg", "whatsapp", "text karna", "reply karna"),
        rule(Category.PERSONAL, WEAK, "leisure", "movie", "film", "netflix", "cricket match", "game khelna", "picnic"),
    )

    // ---------------------------------------------------------------------------------
    // HEALTH & FITNESS
    // ---------------------------------------------------------------------------------
    private val health: List<KeywordRule> = listOf(
        rule(Category.HEALTH_FITNESS, STRONG, "gym", "gym", "jim", "gim", "gymnasium", "gym jana", "gym jaana"),
        rule(Category.HEALTH_FITNESS, STRONG, "workout", "workout", "work out", "workouts", "exercise", "exercises", "exersize", "excercise", "warzish", "varzish", "kasrat", "cardio", "treadmill", "pushups", "push ups", "squats", "reps"),
        rule(Category.HEALTH_FITNESS, STRONG, "running", "running", "runing", "jogging", "jog", "daurna", "dorna", "morning walk", "chehal kadmi", "sair", "steps"),
        rule(Category.HEALTH_FITNESS, STRONG, "yoga", "yoga", "meditation", "meditate", "dhyan", "stretching", "pilates"),
        rule(Category.HEALTH_FITNESS, STRONG, "doctor", "doctor", "doctr", "daktar", "dakter", "hakeem", "hakim", "physician", "surgeon", "dentist", "daant", "dant"),
        rule(Category.HEALTH_FITNESS, STRONG, "clinic", "clinic", "klinic", "klinik", "hospital", "haspatal", "aspatal", "emergency room"),
        rule(Category.HEALTH_FITNESS, STRONG, "medicine", "dawai", "dawaai", "dawa", "davai", "dava", "dawaiyan", "dawain", "medicine", "medicin", "medicines", "tablet", "tablets", "goli", "golian", "golyan", "capsule", "syrup", "injection", "teeka", "tika", "vaccine", "vaccination"),
        rule(Category.HEALTH_FITNESS, STRONG, "medical test", "blood test", "blood pressure", "sugar test", "xray", "x ray", "ultrasound", "checkup", "check up", "chekup", "lab test"),
        rule(Category.HEALTH_FITNESS, MEDIUM, "diet", "diet", "dait", "dieting", "calories", "protein", "vitamin", "vitamins", "supplement", "pani peena"),
        rule(Category.HEALTH_FITNESS, MEDIUM, "health", "sehat", "sehet", "seht", "health", "healthy", "fitness", "tandurusti", "tandrusti"),
        rule(Category.HEALTH_FITNESS, MEDIUM, "weight", "weight loss", "wazan", "wajan", "belly fat", "bmi"),
        rule(Category.HEALTH_FITNESS, MEDIUM, "illness", "bukhar", "bukhaar", "fever", "khansi", "cough", "sar dard", "headache", "bimar", "beemar", "tabiyat", "tabiat"),
        rule(Category.HEALTH_FITNESS, MEDIUM, "therapy", "therapy", "therapist", "physio", "physiotherapy", "counselling", "counseling", "mental health"),
        rule(Category.HEALTH_FITNESS, WEAK, "sleep", "neend", "nind", "sleep", "jaldi sona"),
        rule(Category.HEALTH_FITNESS, WEAK, "walking", "walk", "walking", "walk karna"),
    )

    // ---------------------------------------------------------------------------------
    // CODING
    // ---------------------------------------------------------------------------------
    private val coding: List<KeywordRule> = listOf(
        rule(Category.CODING, STRONG, "code", "code", "coding", "kod", "program", "programming", "script", "scripting", "snippet", "boilerplate"),
        rule(Category.CODING, STRONG, "bug", "bug", "bugs", "debug", "debugging", "bugfix", "bug fix", "hotfix", "stacktrace", "stack trace", "null pointer", "exception", "crash", "regression"),
        rule(Category.CODING, STRONG, "version control", "git", "github", "gitlab", "bitbucket", "commit", "commits", "branch", "rebase", "merge", "merge conflict", "pull request", "pr", "cherry pick", "revert"),
        rule(Category.CODING, STRONG, "api", "api", "apis", "endpoint", "endpoints", "rest api", "graphql", "backend", "back end", "frontend", "front end", "fullstack", "microservice", "webhook", "json", "payload"),
        rule(Category.CODING, STRONG, "database work", "sql", "sqlite", "postgres", "mysql", "mongodb", "schema", "migration", "orm", "room db"),
        rule(
            Category.CODING, STRONG, "tech stack",
            "react", "reactjs", "react native", "angular", "vue", "svelte", "nextjs", "next js",
            "kotlin", "java", "python", "javascript", "typescript", "golang", "rust",
            "android", "ios", "swift", "flutter", "jetpack compose",
            "node", "nodejs", "django", "flask", "spring boot", "laravel",
            "docker", "kubernetes", "terraform", "firebase", "supabase", "gradle", "npm", "pip",
        ),
        rule(Category.CODING, STRONG, "deployment", "deploy", "deployment", "ci cd", "pipeline", "jenkins", "release build", "staging", "rollback", "devops"),
        rule(Category.CODING, MEDIUM, "refactor", "refactor", "refactoring", "code review", "review pr", "lint", "linting", "typecheck"),
        rule(Category.CODING, MEDIUM, "tests", "unit test", "unit tests", "test case", "test cases", "testcase", "integration test", "coverage"),
        rule(Category.CODING, MEDIUM, "issue tracking", "jira", "backlog", "sprint", "story points", "feature branch"),
        rule(Category.CODING, MEDIUM, "build", "build fail", "build karna", "compile", "compiler", "sdk", "dependency", "dependencies", "version bump"),
        rule(Category.CODING, WEAK, "fix", "fix", "fix karna", "fix karni", "patch"),
        rule(Category.CODING, WEAK, "app", "app", "website", "web site", "ui bug", "feature"),
    )

    // ---------------------------------------------------------------------------------
    // STUDY
    // ---------------------------------------------------------------------------------
    private val study: List<KeywordRule> = listOf(
        rule(Category.STUDY, STRONG, "exam", "exam", "exams", "imtihan", "imtehan", "imtihaan", "imtehaan", "paper", "papers", "quiz", "quizzes", "midterm", "mid term", "finals", "viva", "board exam"),
        rule(Category.STUDY, STRONG, "study", "padhna", "parhna", "padhai", "parhai", "padhna hai", "study", "studying", "studies", "revise", "revision", "dohrana", "yaad karna"),
        rule(Category.STUDY, MEDIUM, "preparation", "tayari", "tayyari", "taiyari", "tyari", "tayaari", "preparation"),
        rule(Category.STUDY, STRONG, "homework", "homework", "home work", "hw", "assignment", "assignments", "asignment", "submission"),
        rule(Category.STUDY, STRONG, "class", "lecture", "lectures", "lekchar", "class", "classes", "klass", "tuition", "tution", "coaching", "academy", "madrasa", "sabaq", "sabak"),
        rule(Category.STUDY, STRONG, "institution", "school", "skul", "iskool", "college", "kalij", "university", "uni", "campus", "semester", "degree", "admission", "dakhla"),
        rule(Category.STUDY, MEDIUM, "material", "notes", "note banana", "books", "kitab", "kitaab", "kitaben", "chapter", "syllabus", "course", "lesson", "textbook", "handout"),
        rule(Category.STUDY, MEDIUM, "research", "thesis", "dissertation", "research", "research paper", "literature review", "citation", "bibliography"),
        rule(Category.STUDY, MEDIUM, "standardised test", "ielts", "toefl", "gre", "gmat", "sat exam", "css exam", "ppsc", "nts", "mdcat", "ecat"),
        rule(Category.STUDY, MEDIUM, "results", "result", "results", "natija", "marks", "grades", "gpa", "cgpa", "transcript"),
        rule(Category.STUDY, WEAK, "learning", "learn", "learning", "seekhna", "sikhna", "practice karna", "tutorial dekhna"),
    )

    // ---------------------------------------------------------------------------------
    // SHOPPING
    // ---------------------------------------------------------------------------------
    private val shopping: List<KeywordRule> = listOf(
        rule(Category.SHOPPING, STRONG, "buy", "kharidna", "khareedna", "kharidni", "khareedni", "kharidne", "khareedne", "kharidari", "khareedari", "buy", "buying", "purchase", "shopping", "shoping"),
        rule(Category.SHOPPING, STRONG, "market", "bazaar", "bazar", "market", "markit", "mall", "dukan", "dukaan", "store", "supermarket", "utility store"),
        rule(Category.SHOPPING, STRONG, "groceries", "grocery", "groceries", "grosery", "sauda", "sauda sulf", "rashan", "ration", "grocery list", "shopping list"),
        rule(
            Category.SHOPPING, STRONG, "food items",
            "sabzi", "sabzee", "subzi", "sabziyan", "vegetables", "veggies", "fruit", "fruits",
            "phal", "gosht", "meat", "murghi", "chicken", "anday", "ande", "eggs",
            "doodh", "dudh", "milk", "dahi", "atta", "chawal", "cheeni", "sugar",
            "namak", "cooking oil", "chai patti", "double roti",
        ),
        rule(Category.SHOPPING, STRONG, "online order", "amazon", "daraz", "aliexpress", "cart", "online order", "order karna", "order karni", "parcel", "delivery", "courier"),
        rule(Category.SHOPPING, MEDIUM, "clothing", "kapre", "kapray", "clothes", "shirt", "suit silwana", "darzi", "tailor", "shoes", "joota", "jutay", "chappal"),
        rule(Category.SHOPPING, MEDIUM, "gift", "gift", "gifts", "tohfa", "tohfay"),
        rule(Category.SHOPPING, MEDIUM, "household goods", "detergent", "surf", "sabun", "shampoo", "toothpaste", "tissue", "diapers", "batteries"),
        rule(Category.SHOPPING, WEAK, "fetch", "lena", "leni", "lene", "lana", "lani", "le ana", "le aana", "pick up", "pickup"),

        // Purchase-intent phrases. These deliberately outrank the topical category of the
        // object being bought; see the KDoc of RuleBasedTaskCategorizer.
        rule(
            Category.SHOPPING, PHRASE, "buy medicine",
            "dawai leni", "dawai lani", "dawai lena", "dawa leni", "dawa lani",
            "dawai khareedni", "dawai kharidni", "medicine leni", "medicine lena",
        ),
    )

    // ---------------------------------------------------------------------------------
    // FINANCE
    // ---------------------------------------------------------------------------------
    private val finance: List<KeywordRule> = listOf(
        rule(Category.FINANCE, STRONG, "bill", "bill", "bills", "bil", "utility bill", "phone bill", "internet bill"),
        rule(Category.FINANCE, STRONG, "utilities", "bijli", "bijlee", "electricity", "wapda", "k electric", "sui gas", "gas bill", "pani ka bill", "water bill", "meter reading"),
        rule(Category.FINANCE, MEDIUM, "payment", "pay", "payment", "payments", "pemant", "adaigi", "ada karna", "jama karwana", "jama karana"),
        rule(Category.FINANCE, STRONG, "bank", "bank", "bink", "banking", "atm", "cheque", "chequebook", "khata", "iban", "branch visit"),
        rule(Category.FINANCE, STRONG, "money", "paisa", "paise", "paisay", "paison", "rupay", "rupee", "rupees", "cash", "naqad", "raqam"),
        rule(Category.FINANCE, STRONG, "salary", "salary", "tankhwah", "tankhwa", "tanqah", "tankhuwa", "wages", "ujrat", "payslip", "bonus", "increment"),
        rule(Category.FINANCE, STRONG, "rent", "rent", "kiraya", "kiraaya", "kiraye", "landlord", "malik makan"),
        rule(Category.FINANCE, STRONG, "loan", "loan", "qarz", "qarza", "karz", "karza", "udhar", "udhaar", "mortgage", "repay", "installment", "instalment", "qist", "kist", "emi"),
        rule(Category.FINANCE, STRONG, "tax", "tax", "taxes", "income tax", "withholding", "fbr", "filer", "tax return", "zakat"),
        rule(Category.FINANCE, STRONG, "budgeting", "budget", "bajat", "savings", "saving", "bachat", "expenses", "kharcha", "kharchay", "kharch", "hisab", "hisaab", "ledger"),
        rule(Category.FINANCE, STRONG, "investing", "invest", "investment", "stocks", "shares", "mutual fund", "crypto", "bitcoin", "portfolio", "psx", "sarmaya"),
        rule(Category.FINANCE, MEDIUM, "transfer", "transfer", "easypaisa", "jazzcash", "raast", "remittance", "ibft", "credit card", "debit card"),
        rule(Category.FINANCE, MEDIUM, "billing docs", "invoice", "invoices", "receipt", "rasid", "refund", "reimbursement"),
        rule(Category.FINANCE, MEDIUM, "insurance", "insurance", "bima", "policy premium", "premium"),
        rule(Category.FINANCE, WEAK, "fees", "fee", "fees", "subscription", "renewal charge"),
    )

    /** Every rule, in category order. Exposed for tests and tooling. */
    val RULES: List<KeywordRule> = work + personal + health + coding + study + shopping + finance

    /**
     * Folded keyword to the votes it casts.
     *
     * A key may map to several votes when two categories legitimately share a spelling; both
     * are scored and any resulting tie is resolved by [PRECEDENCE].
     */
    val INDEX: Map<String, List<CategoryVote>> = buildIndex()

    /** Longest keyword length in tokens, i.e. the widest window the matcher must slide. */
    val MAX_NGRAM: Int = INDEX.keys.maxOfOrNull { key -> key.count { it == ' ' } + 1 } ?: 1

    private fun buildIndex(): Map<String, List<CategoryVote>> {
        val index = LinkedHashMap<String, MutableList<CategoryVote>>()
        for (rule in RULES) {
            for (variant in rule.variants) {
                val key = TextNormalizer.foldPhrase(variant)
                if (key.isEmpty()) continue
                val votes = index.getOrPut(key) { mutableListOf() }
                if (votes.none { it.category == rule.category }) {
                    votes.add(CategoryVote(rule.category, rule.weight))
                } else {
                    // Same category reached by two rules: keep the strongest signal only,
                    // so a duplicated spelling cannot inflate a score.
                    val existing = votes.first { it.category == rule.category }
                    if (rule.weight > existing.weight) {
                        votes[votes.indexOf(existing)] = CategoryVote(rule.category, rule.weight)
                    }
                }
            }
        }
        return index.mapValues { (_, votes) -> votes.toList() }
    }
}
