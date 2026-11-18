package com.kylecorry.trail_sense.shared.canvas

import kotlin.test.BeforeTest
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.trail_sense.shared.icons.toImageBitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeMapDrawerTest {
    @BeforeTest
    fun setUpGraphics() {
        prepareTestGraphics()
    }


    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    private fun render(size: Int = 100, block: ComposeMapDrawer.() -> Unit): ImageBitmap {
        val image = ImageBitmap(size, size)
        ComposeMapDrawer(density = 2f).apply {
            setCanvas(Canvas(image), size.toFloat(), size.toFloat())
            block()
        }
        return image
    }

    private fun ImageBitmap.at(x: Int, y: Int): Color = toPixelMap()[x, y]

    private fun assertColor(expected: Int, actual: Color, alphaTolerance: Float = 0.02f) {
        val e = Color(expected)
        assertTrue(
            kotlin.math.abs(e.red - actual.red) < 0.02f && kotlin.math.abs(e.green - actual.green) < 0.02f &&
                kotlin.math.abs(e.blue - actual.blue) < 0.02f && kotlin.math.abs(e.alpha - actual.alpha) < alphaTolerance,
            "expected $e but was $actual"
        )
    }

    @Test
    fun fillsShapesAndLeavesTheRestClear() {
        val image = render {
            noStroke()
            fill(red)
            circle(50f, 50f, 40f)
        }
        assertColor(red, image.at(50, 50))
        assertEquals(0f, image.at(2, 2).alpha)
    }

    @Test
    fun opacityReplacesTheAlphaUntilTheNextColor() {
        val image = render {
            noStroke()
            fill(red)
            opacity(128)
            rect(0f, 0f, 50f, 100f)
            fill(blue)
            rect(50f, 0f, 50f, 100f)
        }
        assertEquals(128 / 255f, image.at(25, 50).alpha, 0.02f)
        assertColor(blue, image.at(75, 50))
    }

    @Test
    fun strokesWithoutFilling() {
        val image = render {
            noFill()
            stroke(blue)
            strokeWeight(6f)
            rect(20f, 20f, 60f, 60f)
        }
        assertColor(blue, image.at(20, 50))
        assertEquals(0f, image.at(50, 50).alpha)
    }

    @Test
    fun transformsArePushedAndPopped() {
        val image = render {
            noStroke()
            fill(red)
            push()
            translate(50f, 50f)
            rect(0f, 0f, 10f, 10f)
            pop()
            fill(blue)
            rect(0f, 0f, 10f, 10f)
        }
        assertColor(red, image.at(55, 55))
        assertColor(blue, image.at(5, 5))
    }

    @Test
    fun rotatesAboutAnOrigin() {
        val image = render {
            noStroke()
            fill(red)
            // A bar right of the center, turned a quarter to below it
            rotate(90f, 50f, 50f)
            rect(60f, 45f, 30f, 10f)
        }
        assertColor(red, image.at(50, 75))
        assertEquals(0f, image.at(75, 50).alpha)
    }

    @Test
    fun layersCompositeWithTheirOpacity() {
        val image = render {
            noStroke()
            fill(red)
            withLayer(128) {
                rect(0f, 0f, 100f, 100f)
                // Overlapping shapes in a layer do not stack their translucency
                rect(0f, 0f, 100f, 100f)
            }
        }
        assertEquals(128 / 255f, image.at(50, 50).alpha, 0.02f)
    }

    @Test
    fun tintsImagesAndCentersThemInCenterMode() {
        val source = render(10) {
            noStroke()
            fill(red)
            rect(0f, 0f, 10f, 10f)
        }
        val image = render {
            tint(blue)
            imageMode(ImageMode.Center)
            image(source, 50f, 50f, 20f, 20f)
        }
        assertColor(blue, image.at(50, 50))
        assertColor(blue, image.at(41, 41))
        assertEquals(0f, image.at(38, 38).alpha)
    }

    @Test
    fun stampedEffectsDrawDotsAlongALine() {
        val image = render {
            noStroke()
            fill(red)
            pathEffect(MapPathEffects.dotted(size = 3f, advance = 20f))
            path(Path().apply {
                moveTo(0f, 50f)
                lineTo(100f, 50f)
            })
        }
        assertColor(red, image.at(0, 50), alphaTolerance = 0.5f)
        assertColor(red, image.at(20, 50), alphaTolerance = 0.5f)
        assertEquals(0f, image.at(10, 50).alpha)
    }

    @Test
    fun measuresAndDrawsText() {
        lateinit var dimensions: Pair<Float, Float>
        val image = render {
            fill(red)
            noStroke()
            textSize(40f)
            textMode(TextMode.Center)
            dimensions = textDimensions("W")
            text("W", 50f, 50f)
        }
        assertTrue(dimensions.first > 10f && dimensions.second > 10f, "$dimensions")
        var painted = 0
        val pixels = image.toPixelMap()
        for (x in 0 until 100) for (y in 0 until 100) if (pixels[x, y].alpha > 0.5f) painted++
        assertTrue(painted > 50, "painted $painted pixels")
    }

    @Test
    fun unitsFollowTheDensity() {
        val drawer = ComposeMapDrawer(density = 2f, fontScale = 1.5f)
        assertEquals(20f, drawer.dp(10f))
        assertEquals(30f, drawer.sp(10f))
    }

    @Test
    fun rastersVectorIconsWithATint() {
        val square = ImageVector.Builder("square", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 6f)
                lineTo(18f, 6f)
                lineTo(18f, 18f)
                lineTo(6f, 18f)
                close()
            }
        }.build()
        val image = square.toImageBitmap(48, tint = red)
        assertColor(red, image.at(24, 24))
        assertEquals(0f, image.at(4, 4).alpha)
    }
}
