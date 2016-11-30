val checkResources = taskKey[Unit]("check for google services resources")

checkResources := {
  import scala.xml.XML
  val p = projectLayout.value
  implicit val output = outputLayout.value
  val res = p.generatedRes / "values" / "generated.xml"
  val root = XML.loadFile(res)
  val node = root \ "string"
  if (node.isEmpty) sys.error("string node not found")
  val found = node.find(_.attribute("name").exists(_.toString == "google_api_key"))
  if (!found.exists(_.text == "BIzaSyBCodF2YHhf4Dt78mSsPW09j_mgLVbkMTw"))
    sys.error("Unable to locate `google_api_key` resource: " + found)
  ()
}
