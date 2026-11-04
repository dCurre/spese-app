package com.dcapps.spese.core.update

/**
 * Versione semantica semplice (major.minor.patch). Confronto numerico per componenti,
 * così "0.0.10" > "0.0.9" (il confronto di stringhe fallirebbe).
 */
data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        return patch - other.patch
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /** Parsea "v1.2.3" o "1.2.3"; componenti mancanti/invalide = 0. Null se non parsabile. */
        fun parse(raw: String?): SemVer? {
            if (raw.isNullOrBlank()) return null
            val cleaned = raw.trim().removePrefix("v").removePrefix("V")
            val parts = cleaned.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            return SemVer(major, minor, patch)
        }
    }
}
