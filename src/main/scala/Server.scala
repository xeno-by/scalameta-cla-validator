import com.twitter.finagle.{Http, Service}
import com.twitter.util.{Await, Future}
import com.twitter.finagle.http.Response
import org.jboss.netty.handler.codec.http._
import util.Properties
import java.net.URI

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
        val ghapi = Github.API.fromUser(Properties.envOrElse("GITHUB_USER", ""), Properties.envOrElse("GITHUB_PASSWORD", ""))
        // NOTE: see https://developer.github.com/v3/activity/events/types/#pullrequestevent
        // TODO: validate the payload stored in request.getContent().toString("UTF-8")
        val response = Response()
        response.setStatusCode(200)
        response.setContentString("TODO")
        Future(response)
      } else {
        Future(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED))
      }
    } else {
      Future(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND))
    }
  }
}
