package com.iattend.app.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Loads an illustration lottie (flat multi-color art, e.g. app/src/main/assets/lottie/Student.lottie)
 * and recolors its single most-used *non-neutral* fill/stroke color to [accent] before parsing.
 *
 * These are flat illustrations, not simple line icons - LottieMorphIcon's uniform
 * COLOR_FILTER/SRC_ATOP tint (built for nav-bar icons) would flatten all of them into a solid
 * silhouette and lose the artwork. Structural tones (near-black outlines, near-white highlights,
 * greys) are left untouched by the saturation/lightness filter below; only the illustration's own
 * "brand" color swaps to the current theme, so each cycles in on-theme without losing detail.
 */
@Composable
fun rememberThemedLottieComposition(assetPath: String, accent: Color) = run {
    val context = LocalContext.current
    val json = remember(assetPath, accent) { recolorLottieAsset(context, assetPath, accent) }
    rememberLottieComposition(LottieCompositionSpec.JsonString(json))
}

private fun readLottieJson(context: Context, assetPath: String): String =
    context.assets.open(assetPath).use { stream ->
        if (assetPath.endsWith(".lottie", ignoreCase = true)) {
            // dotLottie is a zip bundle (manifest.json + animations/*.json) - unwrap to the plain
            // Lottie JSON LottieCompositionSpec.JsonString expects.
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null && !entry.name.startsWith("animations/")) entry = zip.nextEntry
                requireNotNull(entry) { "No animation JSON found in $assetPath" }
                zip.bufferedReader().readText()
            }
        } else {
            stream.bufferedReader().readText()
        }
    }

private fun isNeutral(r: Int, g: Int, b: Int): Boolean {
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val saturation = if (maxC == 0) 0f else (maxC - minC) / maxC.toFloat()
    return saturation < 0.15f || maxC < 40 || minC > 235
}

private fun colorKey(arr: JSONArray): Triple<Int, Int, Int> =
    Triple((arr.getDouble(0) * 255).toInt(), (arr.getDouble(1) * 255).toInt(), (arr.getDouble(2) * 255).toInt())

/** Hides each composition's own baked-in background shape - either a layer explicitly named "bg"/
 * "background" (e.g. Student.lottie, Girl Studying on Laptop.lottie), or, when no such name exists
 * (e.g. Academic Hut banner.lottie's anonymous "Shape Layer 1"), a shape layer whose path bounding
 * box is far larger than the composition canvas (a common AE-export pattern for full-bleed
 * background fills). Illustrations that never had a baked background (Online Learning
 * Platform.lottie) simply match nothing here. Hiding is done by zeroing the layer's own transform
 * opacity, so nothing else about the shape/animation needs to change.
 */
private fun hideBackgroundLayers(root: JSONObject) {
    val canvasArea = root.optDouble("w", 0.0) * root.optDouble("h", 0.0)
    if (canvasArea <= 0.0) return

    fun bboxFraction(layer: JSONObject): Double {
        var minX = Double.MAX_VALUE; var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE; var maxY = -Double.MAX_VALUE
        var found = false
        fun consider(x: Double, y: Double) {
            found = true
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
        }
        fun walk(o: Any?) {
            if (o !is JSONObject) return
            if (o.optString("ty") == "sh") {
                val v = (o.optJSONObject("ks")?.opt("k") as? JSONObject)?.optJSONArray("v")
                if (v != null) for (i in 0 until v.length()) {
                    val pt = v.optJSONArray(i)
                    if (pt != null && pt.length() >= 2) consider(pt.getDouble(0), pt.getDouble(1))
                }
            }
            if (o.optString("ty") == "rc") {
                val s = o.optJSONObject("s")?.opt("k") as? JSONArray
                val p = o.optJSONObject("p")?.opt("k") as? JSONArray
                if (s != null && p != null && s.length() >= 2 && p.length() >= 2) {
                    val (sw, sh) = s.getDouble(0) to s.getDouble(1)
                    val (px, py) = p.getDouble(0) to p.getDouble(1)
                    consider(px - sw / 2, py - sh / 2); consider(px + sw / 2, py + sh / 2)
                }
            }
            for (key in listOf("it", "shapes")) {
                val arr = o.optJSONArray(key) ?: continue
                for (i in 0 until arr.length()) walk(arr.opt(i))
            }
        }
        walk(layer)
        if (!found) return 0.0
        return ((maxX - minX) * (maxY - minY)) / canvasArea
    }

    fun hide(layer: JSONObject) {
        val ks = layer.optJSONObject("ks") ?: JSONObject().also { layer.put("ks", it) }
        ks.put("o", JSONObject().put("a", 0).put("k", 0))
    }

    fun processLayers(layers: JSONArray?) {
        layers ?: return
        for (i in 0 until layers.length()) {
            val layer = layers.optJSONObject(i) ?: continue
            val name = layer.optString("nm", "")
            val nameMatches = Regex("(?i)\\bbg\\b|background").containsMatchIn(name)
            val isShapeLayer = layer.optInt("ty", -1) == 4
            val geomMatches = !nameMatches && isShapeLayer && bboxFraction(layer) > 2.0
            if (nameMatches || geomMatches) hide(layer)
        }
    }

    processLayers(root.optJSONArray("layers"))
    val assets = root.optJSONArray("assets") ?: return
    for (i in 0 until assets.length()) processLayers(assets.optJSONObject(i)?.optJSONArray("layers"))
}

private fun recolorLottieAsset(context: Context, assetPath: String, accent: Color): String {
    val root = JSONObject(readLottieJson(context, assetPath))
    hideBackgroundLayers(root)

    val counts = HashMap<Triple<Int, Int, Int>, Int>()
    fun tally(o: Any?) {
        when (o) {
            is JSONObject -> {
                val ty = o.optString("ty")
                if (ty == "fl" || ty == "st") {
                    val k = o.optJSONObject("c")?.opt("k")
                    if (k is JSONArray && k.length() >= 3) {
                        val key = colorKey(k)
                        if (!isNeutral(key.first, key.second, key.third)) counts.merge(key, 1, Int::plus)
                    }
                }
                o.keys().forEach { tally(o.opt(it)) }
            }
            is JSONArray -> for (i in 0 until o.length()) tally(o.opt(i))
        }
    }
    tally(root)

    val target = counts.maxByOrNull { it.value }?.key ?: return root.toString()
    val newR = accent.red.toDouble()
    val newG = accent.green.toDouble()
    val newB = accent.blue.toDouble()
    fun recolor(o: Any?) {
        when (o) {
            is JSONObject -> {
                val ty = o.optString("ty")
                if (ty == "fl" || ty == "st") {
                    val k = o.optJSONObject("c")?.opt("k")
                    if (k is JSONArray && k.length() >= 3 && colorKey(k) == target) {
                        k.put(0, newR); k.put(1, newG); k.put(2, newB)
                    }
                }
                o.keys().forEach { recolor(o.opt(it)) }
            }
            is JSONArray -> for (i in 0 until o.length()) recolor(o.opt(i))
        }
    }
    recolor(root)
    return root.toString()
}
