package com.zolad.videoslimmer.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

const val MEDIATYPE_NOT_AUDIO_VIDEO = -233

fun checkParmsError(
    sourcePath: String?,
    destinationPath: String?,
    nwidth: Int,
    nheight: Int,
    nbitrate: Int
): Boolean {
    return nwidth <= 0 || nheight <= 0 || nbitrate <= 0
}

fun selectTrack(extractor: MediaExtractor, audio: Boolean): Int {
    val numTracks = extractor.trackCount
    for (i in 0 until numTracks) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME)
        if (audio) {
            if (mime!!.startsWith("audio/")) {
                return i
            }
        } else {
            if (mime!!.startsWith("video/")) {
                return i
            }
        }
    }
    return MEDIATYPE_NOT_AUDIO_VIDEO
}

@Throws(Exception::class)
fun simpleReadAndWriteTrack(
    extractor: MediaExtractor,
    mediaMuxer: MediaMuxer,
    info: MediaCodec.BufferInfo,
    start: Long,
    end: Long,
    file: File?,
    isAudio: Boolean
): Long {
    val trackIndex: Int = selectTrack(extractor, isAudio)
    if (trackIndex >= 0) {
        extractor.selectTrack(trackIndex)
        val trackFormat = extractor.getTrackFormat(trackIndex)
        val muxerTrackIndex = mediaMuxer.addTrack(trackFormat)

        if (!isAudio) mediaMuxer.start()

        val maxBufferSize = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        var inputDone = false
        if (start > 0) {
            extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        } else {
            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        }
        val buffer = ByteBuffer.allocateDirect(maxBufferSize)
        var startTime: Long = -1

        while (!inputDone) {
            var eof = false
            val index = extractor.sampleTrackIndex
            if (index == trackIndex) {
                info.size = extractor.readSampleData(buffer, 0)

                if (info.size < 0) {
                    info.size = 0
                    eof = true
                } else {
                    info.presentationTimeUs = extractor.sampleTime
                    if (start > 0 && startTime == -1L) {
                        startTime = info.presentationTimeUs
                    }
                    if (end < 0 || info.presentationTimeUs < end) {
                        info.offset = 0
                        info.flags = extractor.sampleFlags
                        mediaMuxer.writeSampleData(muxerTrackIndex, buffer, info)
                        extractor.advance()
                    } else {
                        eof = true
                    }
                }
            } else if (index == -1) {
                eof = true
            }
            if (eof) {
                inputDone = true
            }
        }

        extractor.unselectTrack(trackIndex)
        return startTime
    }
    return -1
}

@Throws(java.lang.Exception::class)
fun writeAudioTrack(
    extractor: MediaExtractor,
    mediaMuxer: MediaMuxer,
    info: MediaCodec.BufferInfo,
    start: Long,
    end: Long,
    file: File?,
    muxerTrackIndex: Int
): Long {
    val trackIndex = selectTrack(extractor, true)
    if (trackIndex >= 0) {
        extractor.selectTrack(trackIndex)
        val trackFormat = extractor.getTrackFormat(trackIndex)


        val maxBufferSize = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        var inputDone = false
        if (start > 0) {
            extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        } else {
            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        }
        val buffer = ByteBuffer.allocateDirect(maxBufferSize)
        var startTime: Long = -1

        while (!inputDone) {
            var eof = false
            val index = extractor.sampleTrackIndex
            if (index == trackIndex) {
                info.size = extractor.readSampleData(buffer, 0)

                if (info.size < 0) {
                    info.size = 0
                    eof = true
                } else {
                    info.presentationTimeUs = extractor.sampleTime
                    if (start > 0 && startTime == -1L) {
                        startTime = info.presentationTimeUs
                    }
                    if (end < 0 || info.presentationTimeUs < end) {
                        info.offset = 0
                        info.flags = extractor.sampleFlags
                        mediaMuxer.writeSampleData(muxerTrackIndex, buffer, info)
                        extractor.advance()
                    } else {
                        eof = true
                    }
                }
            } else if (index == -1) {
                eof = true
            }
            if (eof) {
                inputDone = true
            }
        }

        extractor.unselectTrack(trackIndex)
        return startTime
    }
    return -1
}