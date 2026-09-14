/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.innertube.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.rukamori.archivetune.innertube.models.ResponseContext
import moe.rukamori.archivetune.innertube.models.Thumbnails

/**
 * PlayerResponse with [moe.rukamori.archivetune.innertube.models.YouTubeClient.WEB_REMIX] client
 */
@Serializable
data class PlayerResponse(
    val responseContext: ResponseContext,
    val playabilityStatus: PlayabilityStatus,
    val playerConfig: PlayerConfig?,
    val streamingData: StreamingData?,
    val videoDetails: VideoDetails?,
    val captions: Captions? = null,
    @SerialName("playbackTracking")
    val playbackTracking: PlaybackTracking?,
) {
    @Serializable
    data class PlayabilityStatus(
        val status: String,
        val reason: String?,
    )

    @Serializable
    data class PlayerConfig(
        val audioConfig: AudioConfig,
    ) {
        @Serializable
        data class AudioConfig(
            val loudnessDb: Double?,
            val perceptualLoudnessDb: Double?,
        )
    }

    @Serializable
    data class StreamingData(
        val formats: List<Format>?,
        val adaptiveFormats: List<Format>,
        val expiresInSeconds: Int? = null,
    ) {
        @Serializable
        data class Format(
            val itag: Int,
            val url: String?,
            val mimeType: String,
            val bitrate: Int,
            val width: Int?,
            val height: Int?,
            val contentLength: Long?,
            val quality: String,
            val fps: Int?,
            val qualityLabel: String?,
            val averageBitrate: Int?,
            val audioQuality: String?,
            val approxDurationMs: String?,
            val audioSampleRate: Int?,
            val audioChannels: Int?,
            val loudnessDb: Double?,
            val lastModified: Long?,
            val signatureCipher: String?,
            val cipher: String?,
            /**
             * Present only when the video carries more than one audio track — YouTube's dubbing.
             * Absent on the ordinary single-track video, which is why [isDefaultAudioTrack] treats
             * null as the original rather than as unknown.
             */
            val audioTrack: AudioTrack? = null,
        ) {
            @Serializable
            data class AudioTrack(
                /** Human label, e.g. "English original" or "Hindi (dubbed)". Locale-dependent. */
                val displayName: String? = null,
                /** Track id carrying the language, e.g. "en-US.4". */
                val id: String? = null,
                /** True on the track the video was published in. Exactly one track sets it. */
                val audioIsDefault: Boolean? = null,
            )

            val isAudio: Boolean
                get() = width == null

            /**
             * Whether this is the track the song was actually released in.
             *
             * Multi-language videos list every dub as a separate adaptive format, identical in
             * every field a picker normally sorts on — bitrate, codec, sample rate. Without this,
             * choosing purely on quality picks a dub whenever one happens to be encoded better,
             * and the same song plays in a different language depending on which itag won.
             */
            val isDefaultAudioTrack: Boolean
                get() = audioTrack == null || audioTrack.audioIsDefault == true
        }
    }

    @Serializable
    data class VideoDetails(
        val videoId: String? = null,
        val title: String? = null,
        val author: String? = null,
        val channelId: String? = null,
        val lengthSeconds: String? = null,
        val musicVideoType: String? = null,
        val viewCount: String? = null,
        val thumbnail: Thumbnails? = null,
    )

    @Serializable
    data class PlaybackTracking(
        @SerialName("videostatsPlaybackUrl")
        val videostatsPlaybackUrl: VideostatsPlaybackUrl?,
        @SerialName("videostatsWatchtimeUrl")
        val videostatsWatchtimeUrl: VideostatsWatchtimeUrl?,
        @SerialName("atrUrl")
        val atrUrl: AtrUrl?,
    ) {
        @Serializable
        data class VideostatsPlaybackUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )

        @Serializable
        data class VideostatsWatchtimeUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )

        @Serializable
        data class AtrUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )
    }

    /**
     * Caption track for music video subtitle support.
     * Each [CaptionTrack] has a `baseUrl` that points to a timedtext endpoint.
     *
     * Restored from older versions of this file — the upstream refactor in
     * commit 994b137 ("feat: port innertubex facade from Metrolist") removed
     * it, but the fork's VideoArtworkPlayer.kt still depends on it for the
     * music video subtitle overlay.
     */
    @Serializable
    data class CaptionTrack(
        @SerialName("baseUrl")
        val baseUrl: String,
        @SerialName("name")
        val name: Name? = null,
        @SerialName("vssId")
        val vssId: String? = null,
        @SerialName("languageCode")
        val languageCode: String? = null,
        @SerialName("kind")
        val kind: String? = null,
        @SerialName("isTranslatable")
        val isTranslatable: Boolean? = null,
    ) {
        @Serializable
        data class Name(
            @SerialName("simpleText")
            val simpleText: String? = null,
            @SerialName("runs")
            val runs: List<Run>? = null,
        ) {
            @Serializable
            data class Run(val text: String? = null)

            val displayName: String?
                get() = simpleText ?: runs?.joinToString("") { it.text.orEmpty() }?.takeIf { it.isNotBlank() }
        }

        /** ASR tracks are auto-generated; treat them as lower priority than human-authored. */
        val isAutoGenerated: Boolean get() = kind == "asr"

        /**
         * Returns the caption track URL with `fmt=vtt` appended so the
         * response is served as WebVTT, which ExoPlayer parses natively.
         */
        fun webVttUrl(): String {
            val separator = if (baseUrl.contains("?")) "&" else "?"
            return if (baseUrl.contains("fmt=")) baseUrl else "$baseUrl${separator}fmt=vtt"
        }
    }

    /**
     * Container for caption tracks extracted from the player response.
     * Used by the music video subtitle overlay in VideoArtworkPlayer.kt.
     */
    @Serializable
    data class Captions(
        @SerialName("playerCaptionsTracklistRenderer")
        val playerCaptionsTracklistRenderer: PlayerCaptionsTracklistRenderer? = null,
    ) {
        @Serializable
        data class PlayerCaptionsTracklistRenderer(
            @SerialName("captionTracks")
            val captionTracks: List<CaptionTrack>? = null,
            @SerialName("defaultAudioTrackIndex")
            val defaultAudioTrackIndex: Int? = null,
        )
    }
}
