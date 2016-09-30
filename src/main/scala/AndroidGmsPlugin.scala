package android.gms
import sbt._
import android.Keys._
import scala.util.Try

object AndroidGms extends AutoPlugin {
  override def requires = android.AndroidApp
  private[this] val GMS_GROUP = "com.google.android.gms"
  private[this] val GMS_MEASUREMENT = "play-services-measurement"
  private[this] val FIREBASE_GROUP = "com.google.firebase"
  private[this] val FIREBASE_CORE = "firebase-core"
  private[this] val FIREBASE_VERSION = "9.0.0"
  private[this] val MEASUREMENT_VERSION = "8.1.0"
  private[this] val versionOrdering = new Ordering[String] {
    def compare(a: String, b: String) = {
      def comparePart(part: (String, String)) = {
        val (a, b) = part
        Try((a.toInt, b.toInt)) match {
          case util.Success((l, r)) => l compareTo r
          case util.Failure(_)      => a compareTo b
        }
      }
      val aParts = a.split('.')
      val bParts = b.split('.')
      aParts.zip(bParts).map(comparePart).find(_ != 0).getOrElse(
        aParts.length compareTo bParts.length)
    }
  }

  val googleServicesName = "google-services.json"
  case class ApiKey(key: String)
  case class ProjectInfo(
    id: String,
    number: String,
    firebaseUrl: Option[String])
  case class AndroidClientInfo(pkg: String, certHash: Option[String])
  case class ClientInfo(sdkId: String, androidInfo: AndroidClientInfo)
  case class OauthClient(
    id: Option[String], tpe: Int, androidInfo: Option[AndroidClientInfo])
  case class ClientData(
    info: ClientInfo,
    oauthClients: List[OauthClient],
    apiKey: List[ApiKey],
    services: ServicesData)
  case class AnalyticsProperty(trackingId: Option[String])
  case class AnalyticsService(status: Int, property: Option[AnalyticsProperty])
  case class AppInviteService(status: Int)
  case class AdsService(
    status: Int,
    banner: Option[String],
    interstitial: Option[String])
  case class MapsService(status: Int)
  case class ServicesData(
    analytics: Option[AnalyticsService],
    appinvite: Option[AppInviteService],
    ads: Option[AdsService],
    maps: Option[MapsService])
  case class GoogleServices(
    project: ProjectInfo,
    clients: List[ClientData],
    version: String)
  import argonaut.CodecJson._

  implicit val analyticsPropertyCodec = casecodec1(
    AnalyticsProperty.apply, AnalyticsProperty.unapply)("tracking_id")
  implicit val adsServiceCodec = casecodec3(
    AdsService.apply, AdsService.unapply)(
      "status", "test_banner_ad_unit_id", "test_interstitial_ad_unit_id")
  implicit val appInviteServiceCodec = casecodec1(
    AppInviteService.apply, AppInviteService.unapply)("status")
  implicit val mapsServiceCodec = casecodec1(
    MapsService.apply, MapsService.unapply)("status")
  implicit val analyticsServiceCodec = casecodec2(
    AnalyticsService.apply, AnalyticsService.unapply)(
      "status", "analytics_property")
  implicit val servicesDataCodec = casecodec4(
    ServicesData.apply, ServicesData.unapply)(
      "analytics_service",
      "appinvite_service",
      "ads_service",
      "maps_service")
  implicit val apiKeyCodec = casecodec1(
    ApiKey.apply, ApiKey.unapply)("current_key")
  implicit val androidClientInfoCodec = casecodec2(
    AndroidClientInfo.apply, AndroidClientInfo.unapply)(
      "package_name",
      "certificate_hash")
  implicit val oauthClientCodec = casecodec3(
    OauthClient.apply, OauthClient.unapply)(
      "client_id", "client_type", "android_info")
  implicit val clientInfoCodec = casecodec2(
    ClientInfo.apply, ClientInfo.unapply)(
      "mobilesdk_app_id",
      "android_client_info")
  implicit val clientDataCodec = casecodec4(
    ClientData.apply, ClientData.unapply)(
      "client_info",
      "oauth_client",
      "api_key",
      "services")
  implicit val projectInfoCodec = casecodec3(
    ProjectInfo.apply, ProjectInfo.unapply)(
      "project_id", "project_number", "firebase_url")
  implicit val googleServicesCodec = casecodec3(
    GoogleServices.apply, GoogleServices.unapply)(
      "project_info",
      "client",
      "configuration_version")

  val gmsResGenerator = taskKey[Seq[File]]("gms generated resource files")
  val parseGoogleServicesJson = taskKey[GoogleServices](
    "parsed google-services.json")

  def gaTrackingId(gms: GoogleServices, pkg: String): Option[String] = for {
    c <- gms.clients.find(_.info.androidInfo.pkg == pkg)
    a <- c.services.analytics
    p <- a.property
    t <- p.trackingId
  } yield t

  override def projectSettings = Seq(
    Keys.libraryDependencies in Compile := {
      val dependencies = (Keys.libraryDependencies in Compile).value
      val gmsVersion = Try(dependencies.filter(
        _.organization == "com.google.android.gms").map(
          _.revision).max(versionOrdering)).toOption
      if (gmsVersion.isEmpty)
        android.PluginFail("play-services is not used in this project")
      gmsVersion.toList.flatMap { v =>
        if (versionOrdering.compare(v, FIREBASE_VERSION) >= 0) {
          List(FIREBASE_GROUP % FIREBASE_CORE % v)
        } else if (versionOrdering.compare(v, MEASUREMENT_VERSION) >= 0) {
          List(GMS_GROUP % GMS_MEASUREMENT % v)
        } else {
          Keys.sLog.value.warn("google play-services version is old: " + v)
          Nil
        }
      } ++ dependencies
    },
    parseGoogleServicesJson := {
      import argonaut._, Argonaut._
      val layout = projectLayout.value
      val base = Keys.baseDirectory.value
      // not available until sbt-android 1.6.13
      //val (buildType,flavor) = variantConfiguration.value
      val rex = """"(\S+)"""".r
      val (buildType,flavor) =
        buildConfigOptions.value.foldLeft(
          (Option.empty[String],Option.empty[String])) {
            case ((b,f),(_,n,v)) =>
        if (n == "BUILD_TYPE") {
          (rex.findFirstMatchIn(v).map(_.group(1)),f)
        } else if (n == "FLAVOR") {
          (b,rex.findFirstMatchIn(v).map(_.group(1)))
        } else {
          (b,f)
        }
      }
      val buildTypePath = for {
        b <- buildType
      } yield layout.sources / b / googleServicesName
      val combinedPath = for {
        b <- buildType
        f <- flavor
      } yield layout.sources / b / f / googleServicesName
      val searchPaths = ((base / googleServicesName) ::
        (buildTypePath.toList ++ combinedPath.toList)).reverse
      searchPaths.find(_.isFile) match {
        case None => android.PluginFail(
          s"$googleServicesName not found at any of ${searchPaths.mkString(", ")}")
        case Some(f) =>
          IO.readLines(f).mkString.decodeEither[GoogleServices].fold(
            android.PluginFail(_), identity)
      }
    },
    resValues <++= Def.task {
      val appId = applicationId.value
      val googleServices = parseGoogleServicesJson.value
      val client = googleServices.clients.find(
        _.info.androidInfo.pkg == appId)
      val projectNumber = 
        ("string", "gcm_defaultSenderId", googleServices.project.number)
      val firebaseUrl = googleServices.project.firebaseUrl.map { f =>
        ("string", "firebase_database_url", f)
      }.toList
      def crashReportingKey(c: ClientData) = (for {
        a <- c.apiKey.find(_.key.nonEmpty)
      } yield ("string", "google_crash_reporting_api_key", a.key)).toList
      def ads(c: ClientData): List[(String,String,String)] = {
        c.services.ads.toList.flatMap { a =>
          a.banner.toList.map { i =>
            ("string", "test_banner_ad_unit_id", i)
          } ++ a.interstitial.toList.map { i =>
            ("string", "test_interstitial_ad_unit_id", i)
          }
        }
      }
      def googleApp(c: ClientData): (String,String,String) = {
        ("string", "google_app_id", c.info.sdkId)
      }
      def webClient(c: ClientData): List[(String,String,String)] = (for {
        o <- c.oauthClients.find(o => o.tpe == 3 && o.id.isDefined)
        i <- o.id
      } yield ("string", "default_web_client_id", i)).toList
      def maps(c: ClientData): List[(String,String,String)] = {
        (for {
          _ <- c.services.maps
          a <- c.apiKey.find(_.key.nonEmpty)
        } yield ("string", "google_maps_key", a.key)).toList
      }
      if (client.isEmpty) android.PluginFail(
        s"No client_info found for $appId in $googleServicesName")
      client.toList.flatMap { c =>
        googleApp(c) :: projectNumber ::
          (maps(c) ++
            ads(c) ++
            firebaseUrl ++
            crashReportingKey(c) ++
            webClient(c))
      }
    },
    gmsResGenerator := {
      implicit val output = outputLayout.value
      val layout = projectLayout.value
      val appId = applicationId.value
      val googleServices = parseGoogleServicesJson.value
      gaTrackingId(googleServices, appId).toList.flatMap { t=> 
        val xml =
          s"""<?xml version="1.0" encoding="utf-8"?>
             |<resources>
             |  <string name="ga_trackingId" translatable="false">$t</string>
             |</resources>
             |""".stripMargin

        val xmlRes = layout.generatedRes / "xml"
        xmlRes.mkdirs()
        IO.write(xmlRes / "global_tracker.xml", xml)

        List(xmlRes / "global_tracker.xml")
      }
    },
    collectResources <<= collectResources dependsOn gmsResGenerator
  )
}

// vim: sw=2 ts=2 et
