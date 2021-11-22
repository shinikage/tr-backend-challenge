import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import java.time.temporal.ChronoUnit

class Server(
  port: Int = 9000,
) {

  var service : CandlestickManager = CandlestickManagerImpl(30, 1, ChronoUnit.MINUTES, TimestampManagerImpl())

  private val routes = routes(
    "candlesticks" bind Method.GET to { getCandlesticks(it) }
  )

  private val server: Http4kServer = routes.asServer(Netty(port))

  fun start() {
    server.start()
  }

  fun onQuote(quote: Quote) {
    service.addQuote(quote)
  }

  fun onInstrument(instrument: InstrumentEvent) {
    if (instrument.type == InstrumentEvent.Type.ADD) {
      service.addInstrument(instrument.data)
    }
    if (instrument.type == InstrumentEvent.Type.DELETE) {
      service.deleteInstrument(instrument.data)
    }
  }

  private fun getCandlesticks(req: Request): Response {
    val isin = req.query("isin")
      ?: return Response(Status.BAD_REQUEST).body("{'reason': 'missing_isin'}")

    val body = jackson.writeValueAsBytes(service.getCandlesticks(isin))

    return Response(Status.OK).body(body.inputStream())
  }
}
