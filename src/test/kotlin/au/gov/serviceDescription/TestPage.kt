package au.gov.serviceDescription

import au.gov.dxa.serviceDescription.Definition
import au.gov.dxa.serviceDescription.DefinitionDTO
import au.gov.dxa.serviceDescription.Page
import au.gov.dxa.web.ResourceCache
import au.gov.web.MockURIFetcher
import com.beust.klaxon.Klaxon
import org.junit.Assert
import org.junit.Test

class TestPage {

    val pageText = """
# Page Heading

some content

## SubHead 1

some content

## SubHead 2

some content

"""

    @Test
    fun test_get_heading(){
        val page = Page(pageText)
        Assert.assertEquals("Page Heading", page.title)
    }

    @Test
    fun test_get_subheadings(){
        val page = Page(pageText)
        Assert.assertEquals(listOf("SubHead 1", "SubHead 2"), page.subHeaddings)
    }

    @Test
    fun test_markdown(){
        val page = Page("This is **Sparta**")
        Assert.assertEquals("<p>This is <strong>Sparta</strong></p>\n", page.html())
    }


    @Test
    fun will_replace_defcat(){

        var fetcher = MockURIFetcher()
        var cache = ResourceCache<DefinitionDTO>(fetcher, 1, convert = { serial -> Klaxon().parse<DefinitionDTO>(serial)!! })

        Page.definitionCache = cache

        var testDefinition = Definition(name = "Course Code")
        var testDefinitionString = Klaxon().toJsonString(DefinitionDTO(testDefinition))
        fetcher.map["https://definitions.ausdx.io/api/definition/edu/edu307"] = testDefinitionString

        val md = "Use element defcat```edu/edu307``` here"
        val page = Page(md)
        val preProcessed = "Use element [Course Code](https://definitions.ausdx.io/definition/edu/edu307) here"

        Assert.assertEquals(preProcessed, page.preProcessed)
    }

    @Test
    fun wont_replace_defcat_if_resolution_fails(){

        var fetcher = MockURIFetcher()
        var cache = ResourceCache<DefinitionDTO>(fetcher, 1, convert = { serial -> Klaxon().parse<DefinitionDTO>(serial)!! })

        Page.definitionCache = cache

        val md = "Use element defcat```edu/edu307``` here"
        val page = Page(md)
        val preProcessed = "Use element ```edu/edu307``` here"

        Assert.assertEquals(preProcessed, page.preProcessed)
    }
}