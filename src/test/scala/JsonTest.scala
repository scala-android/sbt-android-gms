package android.gms
import org.scalatest._

class JsonTest extends FunSuite {
  import AndroidGmsPlugin._
  import argonaut._, Argonaut._
  test("parse json") {
    val json = io.Source.fromFile(
      getClass.getClassLoader.getResource(
        "google-services.json").toURI).mkString
    val result = json.decodeEither[GoogleServices]
    val data = result.fold(fail(_), identity)
    assertResult("1")(data.version)
    assertResult("giogaecha")(data.project.id)
    assertResult("com.example.foo")(data.clients.head.info.androidInfo.pkg)
    assertResult(None)(data.clients.head.info.androidInfo.certHash)
    assertResult(Some("0000000000000000000000000000000000000000"))(
      data.clients.head.oauthClients.head.androidInfo.get.certHash)
    assertResult(3)(
      data.clients.head.oauthClients.last.tpe)
    assertResult("AIzaSyBCodF2YHhf4Dt78mSsPW09j_mgLVbkMWw")(
      data.clients.head.apiKey.head.key)
    assertResult("UA-21601941-1")(
      data.clients.head.services.analytics.get.property.get.trackingId.get)
  }
}
// vim: sw=2 ts=2 et
