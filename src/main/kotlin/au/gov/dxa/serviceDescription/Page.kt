package au.gov.dxa.serviceDescription

import au.gov.dxa.web.NaiveAPICaller
import au.gov.dxa.web.ResourceCache
import com.beust.klaxon.Klaxon
import com.vladsch.flexmark.ext.gfm.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.slf4j.LoggerFactory
import java.util.*


data class DefinitionDTO(val content:Definition)
data class Definition(val name: String = "", val domain: String ="", val status: String ="", val definition: String ="", val guidance: String ="", val identifier: String ="", val usage:List<String> = listOf(), val type: String ="", val values: List<String> = listOf(), val facets: Map<String, String> = mapOf(), val domainAcronym: String = "", val sourceURL:String = "")
class Page(val markdown:String) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    val title = title()
    val subHeaddings = subHeadings()
    val preProcessed = preProcess()

    private fun title():String {
        var splitLines = markdown.lines()
        var title = splitLines.find { it.startsWith("# ",true) }
        if(title == null) return "Untitled"
        return title.replace("# ","")
    }

    private fun subHeadings():MutableList<String> {
        val list = mutableListOf<String>()
        var splitLines = markdown.lines()
        var headings = splitLines.filter { it.startsWith("## ",true) }
        headings.forEach { it -> list.add(it.replace("## ","")) }
        return list
    }

    private fun preProcess():String{
        var processed = markdown

        processed = insertDefinitions(processed)

        return processed
    }

    private fun insertDefinitions(markdown:String):String{
        var output = markdown
        val regex = kotlin.text.Regex("defcat```([^`\\n\\r])+```")
        var foundLinks = regex.findAll(markdown)
        for (item:MatchResult in  foundLinks)
        {
            val definitionPath = item.value.split("```")[1]
            val apiLinkEndpoint = "https://definitions.ausdx.io/api/definition/$definitionPath"
            val webEndpoint = "https://definitions.ausdx.io/definition/$definitionPath"

            try {
                val definition = Page.definitionCache.get(apiLinkEndpoint).content
                output = output.replace(item.value, "[${definition.name}]($webEndpoint)")
            } catch(e:Exception){
                log.warn("Couldn't resolve definition: $apiLinkEndpoint")
                output = output.replace(item.value, "```$definitionPath```")
            }
        }
        return output
    }

    fun html():String{
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create()));
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true)
        options.set(HtmlRenderer.RENDER_HEADER_ID, true)

        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()

        // You can re-use parser and renderer instances
        val document = parser.parse(preProcessed.trimMargin())
        val html = renderer.render(document)
        return html
    }

    override fun toString() = title
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Page

        if (markdown != other.markdown) return false

        return true
    }

    override fun hashCode(): Int {
        return markdown.hashCode()
    }


    companion object {
        @JvmStatic
        var definitionCache = ResourceCache<DefinitionDTO>(NaiveAPICaller(), 60, convert = { serial -> Klaxon().parse<DefinitionDTO>(serial)!! })
    }

}