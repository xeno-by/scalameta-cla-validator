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
    val response = Response()
    response.setStatusCode(200)
    response.setContentString("Test!")
    println("***" + request.getContent.toString("UTF-8").substring(0, 30))
    println(request.getContent.toString("UTF-8").replace("\n", "  "))
    Future(response)
  }
}
