package com.guesswhere.app.data

import kotlinx.serialization.Serializable

/**
 * DTOs for the MediaWiki action API with formatversion=2 (arrays, not maps).
 * All fields defensive-null: Commons files are inconsistent by nature.
 */
@Serializable
data class MwResponse(
    val batchcomplete: Boolean? = null,
    /** Key is literally "continue", which is a Kotlin soft keyword. */
    @kotlinx.serialization.SerialName("continue") val continueKey: MwContinue? = null,
    val query: MwQuery? = null,
    val error: MwError? = null,
)

@Serializable
data class MwContinue(
    val grncontinue: String? = null,
    val gsroffset: Int? = null,
    val continueKey: String? = null,
)

@Serializable
data class MwError(
    val code: String? = null,
    val info: String? = null,
)

@Serializable
data class MwQuery(
    val pages: List<MwPage> = emptyList(),
)

@Serializable
data class MwPage(
    val pageid: Long = 0L,
    val ns: Int = 0,
    val title: String = "",
    val imageinfo: List<MwImageInfo> = emptyList(),
)

@Serializable
data class MwImageInfo(
    val url: String? = null,
    val thumburl: String? = null,
    val thumbwidth: Int = 0,
    val thumbheight: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val mediatype: String? = null,
    val descriptionurl: String? = null,
    val extmetadata: Map<String, MwMetaValue>? = null,
)

@Serializable
data class MwMetaValue(
    val value: String? = null,
)