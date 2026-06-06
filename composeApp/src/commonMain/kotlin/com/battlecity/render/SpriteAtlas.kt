package com.battlecity.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** Identifiers for each sprite sheet. */
enum class SpriteAsset {
    TILE, PLAYER1, PLAYER2, BULLET, ENEMY,
    EXPLODE1, EXPLODE2, BONUS, BORE, SHIELD,
    MISC, NUM, SPLASH, GAMEOVER, FLAG,
}

/**
 * Sprite image cache. The Compose Multiplatform 1.7+ `imageResource()` API
 * is `@Composable`, but our renderer is a `DrawScope` extension. We bridge
 * the gap by storing either a real [ImageBitmap] (loaded from a PNG asset
 * via the composable layer) or, when no bitmap is available, drawing
 * colour-coded placeholders so the game still runs before assets are
 * wired up.
 */
class SpriteAtlas {
    private val cache = mutableMapOf<SpriteAsset, ImageBitmap>()

    fun put(asset: SpriteAsset, bitmap: ImageBitmap) { cache[asset] = bitmap }
    fun get(asset: SpriteAsset): ImageBitmap? = cache[asset]

    fun draw(
        scope: DrawScope,
        asset: SpriteAsset,
        srcX: Int, srcY: Int, srcW: Int, srcH: Int,
        dstX: Int, dstY: Int,
    ) {
        val bitmap = cache[asset]
        if (bitmap == null) {
            // Fallback: draw a recognisable coloured rectangle. Distinct
            // hues per asset help spot a misnamed sprite during dev.
            scope.drawRect(
                color = placeholderColor(asset),
                topLeft = androidx.compose.ui.geometry.Offset(dstX.toFloat(), dstY.toFloat()),
                size = androidx.compose.ui.geometry.Size(srcW.toFloat(), srcH.toFloat()),
            )
            return
        }
        scope.drawImage(
            image = bitmap,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(srcW, srcH),
            dstOffset = IntOffset(dstX, dstY),
            dstSize = IntSize(srcW, srcH),
        )
    }

    private fun placeholderColor(asset: SpriteAsset): Color = when (asset) {
        SpriteAsset.TILE -> Color(0xFFB58863)
        SpriteAsset.PLAYER1, SpriteAsset.PLAYER2 -> Color(0xFFFFC000)
        SpriteAsset.BULLET -> Color.White
        SpriteAsset.ENEMY -> Color(0xFFB0B0B0)
        SpriteAsset.EXPLODE1 -> Color(0xFFFFC800)
        SpriteAsset.EXPLODE2 -> Color(0xFFFF6400)
        SpriteAsset.BONUS -> Color(0xFFFFFF00)
        SpriteAsset.BORE -> Color(0xFF646464)
        SpriteAsset.SHIELD -> Color(0xFF00C8FF)
        SpriteAsset.MISC -> Color(0xFFB4B4B4)
        SpriteAsset.NUM -> Color.White
        SpriteAsset.SPLASH -> Color(0xFF202020)
        SpriteAsset.GAMEOVER -> Color(0xFFC81E1E)
        SpriteAsset.FLAG -> Color.Red
    }
}
