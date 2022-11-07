import com.opencsv.CSVWriter
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.TimeUnit

@Throws(IOException::class)
fun hitWebPage(url: String): String? {
    val client = OkHttpClient.Builder()
        .connectTimeout(1000, TimeUnit.SECONDS)
        .readTimeout(2000, TimeUnit.SECONDS)
        .build()

    val http = Request.Builder()
        .url(url)
        .build()
    client.newCall(http).execute().use { response -> return response.body!!.string() }
}

fun extractSite(): MutableList<String> {

    val parsedData = Jsoup.parse(hitWebPage("https://www.cleartrip.com/hotels/india/"))
    val siteList: MutableList<String> = mutableListOf()
    val hotelLinks: Elements = parsedData.select("a[href]:contains(hotels)")

    hotelLinks.map { link ->
        if (link.text() == "India Hotels" || link.text() == "Hotels" || link.text() == "india Hotels" || link.text() == "India hotels" || link.text() == "Indian Hotels") {
            println("skipping these")
        } else {
            var weblink = "https://www.cleartrip.com" + link.attr("href")
            println(weblink)
            extractDataHotels(weblink)
            siteList.add(weblink)
            val extraPages: MutableList<String>? = getAllPages(weblink)
            extraPages?.map { url ->
                weblink = url
                extractDataHotels(weblink)
                siteList.add(weblink)
            }
        }
    }
    return (siteList)
}


fun getAllPages(link: String): MutableList<String> {

    val parsedWebPage = Jsoup.parse(hitWebPage(link))
    val restPages: MutableList<String> = mutableListOf()
    val nextPageLink = parsedWebPage.select(".pagination a")

    nextPageLink.map { data ->
        if (data.text() == "Next →" || data.text() == "← Previous") {
            println("skipping")
        } else {
            restPages.add("https://www.cleartrip.com" + data.select("a[href]").attr("href"))
        }
    }
    return restPages
}

fun extractDataHotels(link: String): List<String> {

    val parsedWebPage = Jsoup.parse(hitWebPage(link))
    val hotelCards = parsedWebPage.select("div[class=cth-card-detail]")
    val cityName = parsedWebPage.select("h1[class=\"truncate\"]")

    var hotelData = listOf<String>()

    hotelCards.map { hotelCard ->
        hotelData = listOf(
            cityName.text(),
            hotelCard.select("a[rel='nofollow']").text(),
            hotelCard.select("span[title~=Star]").attr(("title")),
            hotelCard.select("span[class=\"schema-TAcount\"]").text(),
            hotelCard.select("span[class=\"taReviews\"]").text()
        )
        println(hotelData)
        writeToCsv(hotelData)
    }
    return hotelData
}

fun writeToCsv(hotelRow: List<String>) {

    val fileWriter = FileWriter("hotelDataFile.csv", true)
    val csvWriter = CSVWriter(
        fileWriter,
        CSVWriter.DEFAULT_SEPARATOR,
        CSVWriter.NO_QUOTE_CHARACTER,
        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
        CSVWriter.DEFAULT_LINE_END
    )
    csvWriter.writeNext(
        arrayOf(
            hotelRow.component1(),
            hotelRow.component2(),
            hotelRow.component3(),
            hotelRow.component4(),
            hotelRow.component5()
        )
    )
    csvWriter.close()
}

fun main() {

    writeToCsv(
        listOf(
            "City",
            "Hotel Name",
            "Star Rating",
            "TripAdvisor Rating",
            "TripAdvisor Reviews"
        )
    )
    val siteListMain: MutableList<String> = extractSite()
    println(siteListMain)
}

