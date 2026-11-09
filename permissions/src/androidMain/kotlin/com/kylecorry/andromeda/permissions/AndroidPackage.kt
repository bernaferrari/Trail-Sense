package com.kylecorry.andromeda.permissions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.kylecorry.andromeda.core.tryOrDefault
import java.security.MessageDigest

internal object AndroidPackage {

    fun getPackageName(context: Context): String {
        return context.packageName
    }

    fun getPackageInfo(
        context: Context,
        packageName: String = getPackageName(context),
        flags: Int = 0
    ): PackageInfo {
        return if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, flags)
        }
    }

    @Suppress("DEPRECATION")
    fun getSignatureSha256Fingerprints(
        context: Context,
        packageName: String
    ): List<String> {
        val info = tryOrDefault(null) {
            getPackageInfo(
                context,
                packageName,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
            )
        } ?: return emptyList()
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptyList()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            info.signatures ?: return emptyList()
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val signatureHashes = mutableListOf<String>()
        for (sig in signatures) {
            val digest = digest.digest(sig.toByteArray())
            val hash = digest.joinToString(":") { String.format("%02X", it) }
            signatureHashes.add(hash)
        }
        return signatureHashes.distinct()
    }

}