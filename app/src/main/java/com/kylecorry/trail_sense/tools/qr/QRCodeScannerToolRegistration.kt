package com.kylecorry.trail_sense.tools.qr

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object QRCodeScannerToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            ToolIds.QR_CODE_SCANNER,
            context.getString(R.string.qr_code_scanner),
            TrailIcons.QrCode,
            Route.ScanQr,
            ToolCategory.Other,
            guideId = RawTexts.guide_tool_qr_code_scanner,
            diagnostics = listOf(
                ToolDiagnosticFactory.camera(context)
            )
        )
    }
}
