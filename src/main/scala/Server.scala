import com.twitter.finagle.{Http, Service}
import com.twitter.util.{Await, Future}
import com.twitter.finagle.http.Response
import org.jboss.netty.handler.codec.http._
import util.Properties
import java.net.URI
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Server {
  def main(args: Array[String]) {
    val port = Properties.envOrElse("PORT", "8080").toInt
    println("Starting on port: "+port)

    val server = Http.serve(":" + port, new Hello)
    Await.ready(server)
  }
}

class Hello extends Service[HttpRequest, HttpResponse] {
  def apply(request: HttpRequest): Future[HttpResponse] = {
    if (request.getUri() == "/validate-pull-request") {
      if (request.getMethod().toString() == "POST") {
        val payload = request.getContent().toString("UTF-8")
        val actualSignature = request.getHeader("X-Hub-Signature")
        val expectedSignature = {
          val mac = Mac.getInstance("HmacSHA1")
          mac.init(new SecretKeySpec(Properties.envOrElse("WEBHOOK_SECRET", "").getBytes(), "HmacSHA1"))
          "sha1=" + mac.doFinal(payload.getBytes("UTF-8")).map("%02x".format(_)).mkString
        }
        if (actualSignature == expectedSignature) {
          try {
            processPullRequestEvent(payload)
            Future(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
          } catch {
            case ex: Exception =>
              val response = Response()
              response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
              val writer = new java.io.StringWriter()
              ex.printStackTrace(new java.io.PrintWriter(writer))
              response.setContentString(writer.toString)
              Future(response)
          }
        } else {
          Future(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN))
        }
      } else {
        Future(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED))
      }
    } else {
      Future(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND))
    }
  }
  private def processPullRequestEvent(event: String): Unit = {
    // NOTE: see https://developer.github.com/v3/activity/events/types/#pullrequestevent
    import net.liftweb.json._
    val eventJson = parse(event)
    val JString(action) = eventJson \ "action"
    if (action == "opened") {
      val JString(contributor) = eventJson \ "pull_request" \ "user" \ "login"
      val statusJson = parse({
        try dispatch.classic.Http(dispatch.classic.:/("typesafe.com")/("contribute/cla/scala/check/" + contributor) >- Predef.identity)
        catch { case dispatch.classic.StatusCode(404, contents) => contents }
      })
      val JBool(signed) = statusJson \ "signed"
      if (!signed) {
        val JString(user) = eventJson \ "pull_request" \ "base" \ "repo" \ "owner" \ "login"
        val JString(repo) = eventJson \ "pull_request" \ "base" \ "repo" \ "name"
        val JInt(number) = eventJson \ "pull_request" \ "number"
        val message = s"Hello, @$contributor! Thank you for your interest in contributing to the Scala project. Please sign the [Scala CLA](http://typesafe.com/contribute/cla/scala), so that we can proceed with reviewing your pull request."
        val ghapi = Github.API.fromUser(Properties.envOrElse("GITHUB_USER", ""), Properties.envOrElse("GITHUB_PASSWORD", ""))
        ghapi.addPRComment(user, repo, number.toString, message)
      }
    }
  }
}
