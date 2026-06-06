package com.battlecity.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Splash screen rendering, matching UEFI's `GameShowSplash`. */
fun DrawScope.drawSplash(atlas: SpriteAtlas) {
    val sw = 376; val sh = 222
    val sx = ((size.width - sw) / 2).toInt()
    val sy = ((size.height - sh) / 2).toInt()

    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
        size = androidx.compose.ui.geometry.Size(size.width, size.height))
    atlas.draw(this, SpriteAsset.SPLASH, 0, 0, sw, sh, sx, sy)
    atlas.draw(this, SpriteAsset.PLAYER1, 0, 28, 28, 28, sx + 65, sy + 170)
}
