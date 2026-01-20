package cx.aswin.boxcast.core.designsystem.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.math.cos
import kotlin.math.sin

/**
 * Expressive Shapes for BoxCast.
 * Comprehensive implementation of Material 3 Expressive Shapes.
 * Uses androidx.graphics.shapes.RoundedPolygon for radial shapes,
 * and standard Path/GenericShape for irregular shapes.
 */
object ExpressiveShapes {

    // --- Basic Shapes ---
    val Circle = CircleShape
    val Square = RoundedCornerShape(0)
    val Pill = CircleShape // Pill is usually just CircleShape on a rectangle
    val Oval = CircleShape // Compose CircleShape adapts to bounds, forming Oval
    val Full = RoundedCornerShape(50) // For progress indicators, fully rounded ends

    val Slanted: GenericShape
        get() = GenericShape { size, _ ->
            val slant = size.width * 0.15f
            moveTo(slant, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - slant, size.height)
            lineTo(0f, size.height)
            close()
        }

    val Triangle: GenericShape
        get() = GenericShape { size, _ ->
            val polygon = RoundedPolygon(
                numVertices = 3,
                radius = size.minDimension / 2,
                centerX = size.width / 2,
                centerY = size.height / 2,
                rounding = CornerRounding(size.minDimension * 0.1f)
            )
            addPath(polygon.toPath().asComposePath())
        }

    val Semicircle: GenericShape
        get() = GenericShape { size, _ ->
            addArc(Rect(0f, 0f, size.width, size.height * 2), 180f, 180f)
            close()
        }

    val Arch: GenericShape
        get() = GenericShape { size, _ ->
            // Rect bottom, Semicircle top
            moveTo(0f, size.height)
            lineTo(0f, size.height / 2)
            arcTo(
                rect = Rect(0f, 0f, size.width, size.height),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(size.width, size.height)
            close()
        }
    
    val Fan: GenericShape
        get() = GenericShape { size, _ ->
             // Top-Left rounded quarter circle (0,0 to center) is not a Fan?
             // A "Fan" is usually a sector. M3 Fan is often a square with one rounded corner.
             // Let's implement M3 Fan: Square with Top-Right corner rounded fully?
             // Or strict quarter circle.
             
             // Quarter Circle (Top Left)
             moveTo(0f, size.height)
             lineTo(0f, 0f)
             arcTo(Rect(0f, 0f, size.width * 2, size.height * 2), 180f, 90f, false)
             lineTo(size.width, size.height)
             close()
        }

    val Arrow: GenericShape
        get() = GenericShape { size, _ ->
            // Triangle pointing up with rounded corners logic or path
            // M3 Arrow is usually a soft triangle
             val polygon = RoundedPolygon(
                numVertices = 3,
                radius = size.minDimension / 2,
                centerX = size.width / 2,
                centerY = size.height / 2,
                rounding = CornerRounding(size.minDimension * 0.2f)
            )
            addPath(polygon.toPath().asComposePath())
        }

    // --- Polygonal / Stars ---
    val Star: GenericShape
        get() = GenericShape { size, _ ->
            val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 5,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension / 2 * 0.4f,
                rounding = CornerRounding(radius = size.minDimension * 0.05f),
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }

    val Sunny: GenericShape
        get() = GenericShape { size, _ ->
            val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 8,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension / 2 * 0.7f,
                rounding = CornerRounding(radius = size.minDimension * 0.05f),
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }
    
    val VerySunny: GenericShape
        get() = GenericShape { size, _ ->
            val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 12,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension / 2 * 0.75f,
                rounding = CornerRounding(radius = size.minDimension * 0.05f),
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }

    val Diamond: GenericShape
        get() = GenericShape { size, _ ->
            val polygon = RoundedPolygon(
                numVertices = 4,
                radius = size.minDimension / 2,
                centerX = size.width / 2,
                centerY = size.height / 2,
                rounding = CornerRounding(size.minDimension * 0.1f)
            )
            // Rotate 45deg? RoundedPolygon starts with vertex? default orientation might be diamond-like for 4
            // Default 4 is square rotated? let's check. 
            // Often requires rotation.
            addPath(polygon.toPath().asComposePath())
        }
        
    val Pentagon: GenericShape
        get() = createRegularPolygon(5)
    
    val Hexagon: GenericShape
        get() = createRegularPolygon(6)

    val Gem: GenericShape
        get() = GenericShape { size, _ ->
            // 6-sided but slightly different proportions often
            val polygon = RoundedPolygon(
                numVertices = 6,
                radius = size.minDimension / 2,
                centerX = size.width / 2,
                centerY = size.height / 2,
                rounding = CornerRounding(size.minDimension * 0.15f)
            )
             addPath(polygon.toPath().asComposePath())
        }

    // --- Cookies (Rounded N-gons) ---
    val Cookie4: GenericShape get() = createCookie(4)
    val Cookie6: GenericShape get() = createCookie(6)
    val Cookie7: GenericShape get() = createCookie(7)
    val Cookie9: GenericShape get() = createCookie(9)
    val Cookie12: GenericShape get() = createCookie(12)


    // --- Expressive ---
    
    val GhostIsh: GenericShape
        get() = GenericShape { size, _ ->
            // Arch top, subtle wavy bottom
            val w = size.width
            val h = size.height
            // Top Arch
            moveTo(0f, h * 0.5f)
            arcTo(Rect(0f, 0f, w, h), 180f, 180f, false)
            lineTo(w, h * 0.8f)
            // Simpler subtle wave at bottom
             cubicTo(w * 0.75f, h, w * 0.25f, h * 0.6f, 0f, h * 0.8f)
            close()
        }

    val Clover4: GenericShape
        get() = GenericShape { size, _ ->
            val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 4,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension * 0.1f, // Tight center
                rounding = CornerRounding(size.minDimension * 0.25f), // Heavy rounding
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }
        
    val Clover8: GenericShape
        get() = GenericShape { size, _ ->
             val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 8,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension * 0.3f, 
                rounding = CornerRounding(size.minDimension * 0.15f), 
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }

    val Burst: GenericShape
        get() = GenericShape { size, _ ->
            val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 12,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension / 2 * 0.6f,
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }
        
    val SoftBurst: GenericShape
        get() = GenericShape { size, _ ->
             val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 10,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension / 2 * 0.7f,
                rounding = CornerRounding(size.minDimension * 0.05f),
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }
        
    val Boom: GenericShape
        get() = GenericShape { size, _ ->
             // Sharp 16 point star
              val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 16,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension / 2 * 0.4f,
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }
    
    val SoftBoom: GenericShape
        get() = GenericShape { size, _ ->
              val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 16,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension / 2 * 0.4f,
                 rounding = CornerRounding(size.minDimension * 0.03f),
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }

    val Flower: GenericShape
        get() = GenericShape { size, _ ->
             val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 8,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension / 2 * 0.6f,
                rounding = CornerRounding(size.minDimension * 0.05f),
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }

    // Puffy / Cloud-like
    val Puffy: GenericShape
        get() = GenericShape { size, _ ->
            // Low vertex count star with heavy rounding creates puffy look
            val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 8,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension / 2 * 0.7f,
                rounding = CornerRounding(size.minDimension * 0.2f),
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }
    
    val PuffyDiamond: GenericShape
        get() = GenericShape { size, _ ->
            val polygon = RoundedPolygon.star(
                numVerticesPerRadius = 4,
                radius = size.minDimension / 2,
                innerRadius = size.minDimension / 2 * 0.5f,
                rounding = CornerRounding(size.minDimension * 0.2f),
                centerX = size.width / 2,
                centerY = size.height / 2
            )
            addPath(polygon.toPath().asComposePath())
        }

    val Bun: GenericShape
        get() = GenericShape { size, _ ->
            // Two pills merged or broad rounded rect with pinch
            // Simulating with RoundedPolygon 4 sides heavily modified or path
            // Path: Top oval, Bottom oval slightly overlapping
            addOval(Rect(0f, 0f, size.width, size.height * 0.65f))
            addOval(Rect(0f, size.height * 0.35f, size.width, size.height))
        }

    val Heart: GenericShape
        get() = GenericShape { size, _ ->
            val width = size.width
            val height = size.height
            val path = Path()
            path.moveTo(width / 2, height * 0.25f)
            path.cubicTo(width, 0f, width, height * 0.5f, width / 2, height)
            path.cubicTo(0f, height * 0.5f, 0f, 0f, width / 2, height * 0.25f)
            addPath(path)
        }
        
    val Clamshell: GenericShape
        get() = GenericShape { size, _ ->
             // Wide Arch / Fan
             // Reverse Fan
              addArc(Rect(0f, 0f, size.width, size.height), 0f, 180f)
              // This is just a semicircle, Clamshell usually has scallops or specific width
              // Stick to simple wide arc for now
        }

    // --- Helpers ---
    private fun createRegularPolygon(vertices: Int): GenericShape = GenericShape { size, _ ->
        val polygon = RoundedPolygon(
            numVertices = vertices,
            radius = size.minDimension / 2,
            centerX = size.width / 2,
            centerY = size.height / 2
        )
        addPath(polygon.toPath().asComposePath())
    }

    private fun createCookie(sides: Int): GenericShape = GenericShape { size, _ ->
         val polygon = RoundedPolygon(
            numVertices = sides,
            radius = size.minDimension / 2,
            centerX = size.width / 2,
            centerY = size.height / 2,
            rounding = CornerRounding(size.minDimension * 0.2f)
        )
        addPath(polygon.toPath().asComposePath())
    }

    // --- Raw Polygons (for LoadingIndicator) ---
    object Polygons {
        // Standardized size for morphing compatibility
        private const val RADIUS = 1f
        
        val Star: RoundedPolygon = RoundedPolygon.star(
            numVerticesPerRadius = 5,
            radius = RADIUS,
            innerRadius = RADIUS * 0.4f,
            rounding = CornerRounding(radius = RADIUS * 0.05f)
        )

        val Sunny: RoundedPolygon = RoundedPolygon.star(
            numVerticesPerRadius = 8,
            radius = RADIUS,
            innerRadius = RADIUS * 0.7f,
            rounding = CornerRounding(radius = RADIUS * 0.05f)
        )

        val Burst: RoundedPolygon = RoundedPolygon.star(
            numVerticesPerRadius = 12,
            radius = RADIUS,
            innerRadius = RADIUS * 0.6f
        )
        
        val Cookie4: RoundedPolygon = RoundedPolygon(
            numVertices = 4,
            radius = RADIUS,
            rounding = CornerRounding(RADIUS * 0.2f)
        )
        
        val Cookie12: RoundedPolygon = RoundedPolygon(
            numVertices = 12,
            radius = RADIUS,
            rounding = CornerRounding(RADIUS * 0.2f)
        )
    }
}
