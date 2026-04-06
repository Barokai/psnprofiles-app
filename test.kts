import org.jsoup.Jsoup
val doc = Jsoup.connect("https://psnprofiles.com/guide/15561-goat-simulator-3-trophy-guide").get()
val containers = doc.select("div[id^=SectionContainer]")
val c2 = containers[2] // 3rd container, I watched the intro
val innerDiv = c2.children().firstOrNull()
val zebraTable = innerDiv?.selectFirst("table.zebra")
println("Has inner div? $( != null)")
println("Has zebra table? $( != null)")
val p1 = zebraTable?.parent()
println("Zebra table parent ID: " + p1?.id() + " classes: " + p1?.className())
zebraTable?.parent()?.remove()

val foundTags = innerDiv?.select("span.tag")
println("Found tags after remove: " + foundTags?.size)
