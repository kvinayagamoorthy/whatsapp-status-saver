package com.moorthy.statussaver.data.model

/**
 * Which app wrote a statuses folder. The user can grant one folder per kind, and the
 * status list merges items from every granted kind.
 */
enum class StatusSourceKind {
    WHATSAPP,
    BUSINESS,
}
