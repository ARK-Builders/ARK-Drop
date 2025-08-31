package dev.arkbuilders.drop.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design System Tokens for ARK Drop
 * Following Material Design 3 and Apple HIG principles
 */
object DesignTokens {
    
    // Spacing Scale - 8pt grid system
    object Spacing {
        val xs = 4.dp      // Micro spacing
        val sm = 8.dp      // Small spacing
        val md = 12.dp     // Medium spacing
        val lg = 16.dp     // Large spacing
        val xl = 24.dp     // Extra large spacing
        val xxl = 32.dp    // Double extra large spacing
        val xxxl = 48.dp   // Triple extra large spacing
        val huge = 64.dp   // Huge spacing
    }
    
    // Elevation Scale
    object Elevation {
        val none = 0.dp
        val xs = 1.dp      // Subtle elevation
        val sm = 3.dp      // Small elevation
        val md = 6.dp      // Medium elevation
        val lg = 8.dp      // Large elevation
        val xl = 12.dp     // Extra large elevation
        val xxl = 16.dp    // Maximum elevation
    }
    
    // Corner Radius Scale
    object CornerRadius {
        val xs = 4.dp      // Small corners
        val sm = 8.dp      // Medium corners
        val md = 12.dp     // Default corners
        val lg = 16.dp     // Large corners
        val xl = 20.dp     // Extra large corners
        val xxl = 24.dp    // Maximum corners
        val round = 50.dp  // Fully rounded (pills)
    }
    
    // Touch Targets
    object TouchTarget {
        val minimum = 48.dp    // Minimum touch target size
        val comfortable = 56.dp // Comfortable touch target size
        val large = 64.dp      // Large touch target size
    }
    
    // Animation Durations
    object Animation {
        const val fast = 150
        const val normal = 300
        const val slow = 500
        const val extraSlow = 800
    }
    
    // Content Width Constraints
    object Layout {
        val maxContentWidth = 600.dp
        val minTouchTarget = 48.dp
        val cardMaxWidth = 400.dp
    }
}
