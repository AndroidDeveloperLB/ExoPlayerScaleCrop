package com.example.user.myapplication

import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.LoopingMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
    private var player: SimpleExoPlayer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(ColorDrawable(0xff000000.toInt()))
        super.onCreate(savedInstanceState)
        if (cache == null) {
            cache = SimpleCache(File(cacheDir, "media"), LeastRecentlyUsedCacheEvictor(MAX_PREVIEW_CACHE_SIZE_IN_BYTES))
        }
        setContentView(R.layout.activity_main)
        imageView.visibility = View.VISIBLE
        imageView.doOnPreDraw {
            imageView.imageMatrix = prepareMatrix(imageView, imageView.drawable.intrinsicWidth.toFloat(), imageView.drawable.intrinsicHeight.toFloat())
        }
    }

    override fun onStart() {
        super.onStart()
        playVideo()
    }

    private fun prepareMatrix(view: View, contentWidth: Float, contentHeight: Float): Matrix {
        var scaleX = 1.0f
        var scaleY = 1.0f
        val viewWidth = view.width.toFloat()
        val viewHeight = view.height.toFloat()
        Log.d("AppLog", "viewWidth $viewWidth viewHeight $viewHeight contentWidth:$contentWidth contentHeight:$contentHeight")
        if (contentWidth > viewWidth && contentHeight > viewHeight) {
            scaleX = contentWidth / viewWidth
            scaleY = contentHeight / viewHeight
        } else if (contentWidth < viewWidth && contentHeight < viewHeight) {
            scaleY = viewWidth / contentWidth
            scaleX = viewHeight / contentHeight
        } else if (viewWidth > contentWidth)
            scaleY = viewWidth / contentWidth / (viewHeight / contentHeight)
        else if (viewHeight > contentHeight)
            scaleX = viewHeight / contentHeight / (viewWidth / contentWidth)
        val matrix = Matrix()
        val pivotPercentageLeft = 0.5f
        val pivotPercentageTop = 0.1f
        matrix.setScale(scaleX, scaleY, viewWidth * pivotPercentageLeft, viewHeight * pivotPercentageTop)
        return matrix
    }

    private fun playVideo() {
        player = ExoPlayerFactory.newSimpleInstance(this@MainActivity, DefaultTrackSelector())
        player!!.setVideoTextureView(textureView)
        player!!.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                Log.d("AppLog", "onVideoSizeChanged: $width $height")
                val videoWidth = if (width % 180 == 0) width else height
                val videoHeight = if (height % 180 == 0) height else width
                val matrix = prepareMatrix(textureView, videoWidth.toFloat(), videoHeight.toFloat())
                textureView.setTransform(matrix)
            }

            override fun onRenderedFirstFrame() {
                Log.d("AppLog", "onRenderedFirstFrame")
                player!!.removeVideoListener(this)
                imageView.visibility = View.INVISIBLE
            }
        })
        player!!.volume = 0f
        player!!.repeatMode = Player.REPEAT_MODE_ALL
        player!!.playRawVideo(this, R.raw.test)
        player!!.playWhenReady = true
        //        player!!.playVideoFromUrl(this, "https://sample-videos.com/video123/mkv/240/big_buck_bunny_240p_20mb.mkv", cache!!)
        //        player!!.playVideoFromUrl(this, "https://sample-videos.com/video123/mkv/720/big_buck_bunny_720p_1mb.mkv", cache!!)
        //        player!!.playVideoFromUrl(this@MainActivity, "https://sample-videos.com/video123/mkv/720/big_buck_bunny_720p_1mb.mkv")
    }

    override fun onStop() {
        super.onStop()
        player!!.setVideoTextureView(null)
        //        playerView.player = null
        player!!.release()
        player = null
    }

    companion object {
        const val MAX_PREVIEW_CACHE_SIZE_IN_BYTES = 20L * 1024L * 1024L
        var cache: com.google.android.exoplayer2.upstream.cache.Cache? = null

        @JvmStatic
        fun getUserAgent(context: Context): String {
            val packageManager = context.packageManager
            val info = packageManager.getPackageInfo(context.packageName, 0)
            val appName = info.applicationInfo.loadLabel(packageManager).toString()
            return Util.getUserAgent(context, appName)
        }
    }

    fun SimpleExoPlayer.playRawVideo(context: Context, @RawRes rawVideoRes: Int) {
        val dataSpec = DataSpec(RawResourceDataSource.buildRawResourceUri(rawVideoRes))
        val rawResourceDataSource = RawResourceDataSource(context)
        rawResourceDataSource.open(dataSpec)
        val factory: DataSource.Factory = DataSource.Factory { rawResourceDataSource }
        prepare(LoopingMediaSource(ExtractorMediaSource.Factory(factory).createMediaSource(rawResourceDataSource.uri)))
    }

    fun SimpleExoPlayer.playVideoFromUrl(context: Context, url: String, cache: Cache? = null) = playVideoFromUri(context, Uri.parse(url), cache)

    fun SimpleExoPlayer.playVideoFile(context: Context, file: File) = playVideoFromUri(context, Uri.fromFile(file))

    fun SimpleExoPlayer.playVideoFromUri(context: Context, uri: Uri, cache: Cache? = null) {
        val factory = if (cache != null)
            CacheDataSourceFactory(cache, DefaultHttpDataSourceFactory(getUserAgent(context)))
        else
            DefaultDataSourceFactory(context, MainActivity.getUserAgent(context))
        val mediaSource = ExtractorMediaSource.Factory(factory).createMediaSource(uri)
        prepare(mediaSource)
    }
}
