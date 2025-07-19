package dev.arkbuilders.drop.app.utils

import kotlin.random.Random

object RandomNameGenerator {

    private val adjectives = listOf(
        // Positive traits
        "clever", "bright", "swift", "gentle", "brave", "wise", "kind", "bold",
        "calm", "cool", "warm", "fierce", "quick", "smart", "sharp", "keen",
        "pure", "free", "wild", "true", "fresh", "alive", "awake", "aware",

        // Nature-inspired
        "misty", "sunny", "cloudy", "stormy", "windy", "frosty", "snowy", "rainy",
        "golden", "silver", "crystal", "pearl", "amber", "jade", "ruby", "emerald",

        // Movement/Energy
        "dancing", "flying", "soaring", "flowing", "gliding", "drifting", "rushing",
        "floating", "spinning", "jumping", "leaping", "racing", "wandering",

        // Emotions/Feelings
        "happy", "joyful", "cheerful", "peaceful", "serene", "content", "blissful",
        "hopeful", "dreamy", "curious", "playful", "merry", "lively", "vibrant",

        // Colors
        "crimson", "azure", "violet", "indigo", "scarlet", "turquoise", "lavender",
        "magenta", "coral", "teal", "lime", "maroon", "navy", "olive", "pink",

        // Size/Intensity
        "tiny", "small", "little", "mini", "micro", "giant", "huge", "mega",
        "super", "ultra", "grand", "mighty", "vast", "epic", "colossal",

        // Technology/Modern
        "digital", "cyber", "virtual", "quantum", "nano", "tech", "smart", "auto",
        "electric", "magnetic", "sonic", "laser", "plasma", "neon", "pixel",

        // Mystical/Fantasy
        "mystic", "magic", "cosmic", "stellar", "lunar", "solar", "astral",
        "ethereal", "divine", "enchanted", "mysterious", "legendary", "mythical"
    )

    private val nouns = listOf(
        // Animals
        "fox", "wolf", "bear", "lion", "tiger", "eagle", "hawk", "owl", "raven",
        "dove", "swan", "crane", "heron", "falcon", "shark", "whale", "dolphin",
        "turtle", "rabbit", "deer", "horse", "unicorn", "dragon", "phoenix",
        "butterfly", "bee", "ant", "spider", "cat", "dog", "mouse", "elephant",

        // Nature Elements
        "river", "ocean", "mountain", "forest", "desert", "valley", "cliff",
        "waterfall", "lake", "pond", "stream", "meadow", "grove", "hill",
        "stone", "rock", "crystal", "gem", "pearl", "shell", "leaf", "flower",
        "tree", "branch", "root", "seed", "flame", "spark", "ember", "ash",

        // Celestial Bodies
        "star", "moon", "sun", "comet", "meteor", "galaxy", "nebula", "planet",
        "cosmos", "orbit", "asteroid", "constellation", "aurora", "eclipse",

        // Weather/Elements
        "cloud", "rain", "snow", "mist", "fog", "wind", "storm", "thunder",
        "lightning", "rainbow", "frost", "ice", "fire", "earth", "water", "air",

        // Objects/Tools
        "arrow", "blade", "shield", "hammer", "key", "lock", "bridge", "tower",
        "castle", "gate", "door", "window", "mirror", "lamp", "candle", "torch",
        "compass", "map", "book", "scroll", "pen", "brush", "canvas", "lens",

        // Abstract Concepts
        "dream", "hope", "wish", "joy", "peace", "love", "trust", "faith",
        "courage", "wisdom", "truth", "freedom", "unity", "harmony", "balance",
        "energy", "power", "force", "spirit", "soul", "heart", "mind", "will",

        // Professions/Characters
        "builder", "maker", "creator", "artist", "writer", "poet", "singer",
        "dancer", "runner", "climber", "sailor", "pilot", "explorer", "seeker",
        "hunter", "guardian", "keeper", "watcher", "guide", "teacher", "student",

        // Technology/Future
        "robot", "droid", "cyber", "pixel", "byte", "code", "data", "signal",
        "wave", "pulse", "beam", "ray", "core", "chip", "circuit", "matrix",
        "nexus", "node", "hub", "link", "network", "system", "protocol", "cipher"
    )

    /**
     * Generates a random name in the format "adjective_noun"
     * Similar to Docker's naming convention
     */
    fun generateName(): String {
        val adjective = adjectives.random()
        val noun = nouns.random()
        return "${adjective}_${noun}"
    }

    /**
     * Generates a random name with a specific format
     */
    fun generateName(separator: String = "_", capitalize: Boolean = false): String {
        val adjective = adjectives.random()
        val noun = nouns.random()

        return if (capitalize) {
            "${adjective.replaceFirstChar { it.uppercaseChar() }}${separator}${noun.replaceFirstChar { it.uppercaseChar() }}"
        } else {
            "$adjective$separator$noun"
        }
    }

    /**
     * Generates a random name with additional entropy (number suffix)
     */
    fun generateNameWithNumber(maxNumber: Int = 999): String {
        val baseName = generateName()
        val number = Random.Default.nextInt(0, maxNumber + 1)
        return "${baseName}_$number"
    }

    /**
     * Generates multiple unique names
     */
    fun generateUniqueNames(count: Int): List<String> {
        val names = mutableSetOf<String>()
        var attempts = 0
        val maxAttempts = count * 10 // Avoid infinite loops

        while (names.size < count && attempts < maxAttempts) {
            names.add(generateName())
            attempts++
        }

        // If we couldn't generate enough unique names, add numbers
        while (names.size < count) {
            names.add(generateNameWithNumber())
        }

        return names.toList()
    }

    /**
     * Get a random adjective
     */
    fun getRandomAdjective(): String = adjectives.random()

    /**
     * Get a random noun
     */
    fun getRandomNoun(): String = nouns.random()

    /**
     * Check if a name follows the expected format
     */
    fun isValidGeneratedName(name: String): Boolean {
        val parts = name.split("_")
        if (parts.size < 2) return false

        val adjective = parts[0]
        val noun = parts[1]

        return adjectives.contains(adjective) && nouns.contains(noun)
    }

    /**
     * Generate a name that's guaranteed to be different from the provided name
     */
    fun generateDifferentName(excludeName: String): String {
        var newName: String
        var attempts = 0
        val maxAttempts = 50

        do {
            newName = generateName()
            attempts++
        } while (newName == excludeName && attempts < maxAttempts)

        // If we still got the same name after many attempts, add a number
        if (newName == excludeName) {
            newName = generateNameWithNumber()
        }

        return newName
    }
}