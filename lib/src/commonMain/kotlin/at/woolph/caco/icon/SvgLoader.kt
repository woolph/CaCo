package at.woolph.caco.icon

import arrow.core.Either
import at.woolph.utils.Uri
import at.woolph.utils.ktor.useHttpClient
import io.ktor.client.request.request
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.Dispatchers

interface SvgLoader {
  suspend fun loadSvg(uri: Uri): Either<Throwable, String>
}

class ModifyingSvgLoader(
  val svgLoader: SvgLoader,
  val modificator: (String) -> String,
): SvgLoader {
  override suspend fun loadSvg(uri: Uri): Either<Throwable, String> =
    svgLoader.loadSvg(uri).map(modificator)

  companion object {
    fun gradientModded(
      svgLoader: SvgLoader,
      strokeColor: String,
      color1: String,
      color2: String,
    ) =
      ModifyingSvgLoader(
        svgLoader = svgLoader,
        modificator = {
          it.replace(
            "<path ",
            "<defs>\n" +
              "    <linearGradient id=\"uncommon-gradient\" x2=\"0.35\" y2=\"1\">\n" +
              "        <stop offset=\"0%\" stop-color=\"$color1\" />\n" +
              "        <stop offset=\"50%\" stop-color=\"$color2\" />\n" +
              "        <stop offset=\"100%\" stop-color=\"$color1\" />\n" +
              "      </linearGradient>\n" +
              "  </defs>\n" +
              "<path stroke=\"$strokeColor\" stroke-width=\"2%\" style=\"fill:url(#uncommon-gradient)\" ",
          )
        },
      )
  }
}

object BasicSvgLoader: SvgLoader {
  override suspend fun loadSvg(uri: Uri): Either<Throwable, String> = Either.catch {
    useHttpClient(Dispatchers.IO) {
      it.request(uri.toString()).readRawBytes().toString(Charsets.UTF_8)
    }
  }
}

val MythicSvgLoader = ModifyingSvgLoader.gradientModded(BasicSvgLoader, "black", "#c54326", "#f7971c")
val RareSvgLoader = ModifyingSvgLoader.gradientModded(BasicSvgLoader, "black", "#8d7431", "#f6db94")
val UncommonSvgLoader = ModifyingSvgLoader.gradientModded(BasicSvgLoader, "black", "#626e77", "#c8e2f2")
val CommonSvgLoader = BasicSvgLoader
