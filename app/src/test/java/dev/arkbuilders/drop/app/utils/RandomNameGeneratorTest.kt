package dev.arkbuilders.drop.app.utils

import org.junit.Test
import org.junit.Assert.*

class RandomNameGeneratorTest {

    @Test
    fun testGenerateName() {
        val name = RandomNameGenerator.generateName()
        
        // Check format (should contain underscore)
        assertTrue("Name should contain underscore", name.contains("_"))
        
        // Check parts
        val parts = name.split("_")
        assertEquals("Name should have exactly 2 parts", 2, parts.size)
        
        // Check if it's a valid generated name
        assertTrue("Should be a valid generated name", 
                  RandomNameGenerator.isValidGeneratedName(name))
        
        println("Generated name: $name")
    }

    @Test
    fun testGenerateNameWithCustomSeparator() {
        val name = RandomNameGenerator.generateName("-", true)
        
        assertTrue("Name should contain hyphen", name.contains("-"))
        
        val parts = name.split("-")
        assertEquals("Name should have exactly 2 parts", 2, parts.size)
        
        // Check capitalization
        assertTrue("First part should be capitalized", 
                  parts[0].first().isUpperCase())
        assertTrue("Second part should be capitalized", 
                  parts[1].first().isUpperCase())
        
        println("Generated name with hyphen and capitalization: $name")
    }

    @Test
    fun testGenerateNameWithNumber() {
        val name = RandomNameGenerator.generateNameWithNumber(100)
        
        val parts = name.split("_")
        assertEquals("Name should have exactly 3 parts", 3, parts.size)
        
        // Last part should be a number
        val number = parts[2].toIntOrNull()
        assertNotNull("Last part should be a number", number)
        assertTrue("Number should be between 0 and 100", 
                  number!! in 0..100)
        
        println("Generated name with number: $name")
    }

    @Test
    fun testGenerateUniqueNames() {
        val names = RandomNameGenerator.generateUniqueNames(10)
        
        assertEquals("Should generate 10 names", 10, names.size)
        
        // Check uniqueness
        val uniqueNames = names.toSet()
        assertEquals("All names should be unique", names.size, uniqueNames.size)
        
        println("Generated unique names:")
        names.forEach { println("  $it") }
    }

    @Test
    fun testGenerateDifferentName() {
        val originalName = "clever_fox"
        val newName = RandomNameGenerator.generateDifferentName(originalName)
        
        assertNotEquals("New name should be different from original", 
                       originalName, newName)
        
        println("Original: $originalName")
        println("New: $newName")
    }

    @Test
    fun testRandomComponents() {
        val adjective = RandomNameGenerator.getRandomAdjective()
        val noun = RandomNameGenerator.getRandomNoun()
        
        assertNotNull("Adjective should not be null", adjective)
        assertNotNull("Noun should not be null", noun)
        assertTrue("Adjective should not be empty", adjective.isNotEmpty())
        assertTrue("Noun should not be empty", noun.isNotEmpty())
        
        println("Random adjective: $adjective")
        println("Random noun: $noun")
    }

    @Test
    fun testMultipleGenerations() {
        println("Sample generated names:")
        repeat(20) {
            val name = RandomNameGenerator.generateName()
            println("  $name")
        }
    }

    @Test
    fun testVariousFormats() {
        println("Different formats:")
        
        // Standard format
        println("Standard: ${RandomNameGenerator.generateName()}")
        
        // With hyphen
        println("Hyphenated: ${RandomNameGenerator.generateName("-")}")
        
        // Capitalized with space
        println("Capitalized: ${RandomNameGenerator.generateName(" ", true)}")
        
        // With number
        println("With number: ${RandomNameGenerator.generateNameWithNumber()}")
        
        // CamelCase style
        val camelCase = RandomNameGenerator.generateName("", true)
        println("CamelCase: $camelCase")
    }
}