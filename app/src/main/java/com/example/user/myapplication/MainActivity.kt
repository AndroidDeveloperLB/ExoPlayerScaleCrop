package com.example.user.myapplication

import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private val imageResId = R.drawable.test
    private val videoResId = R.raw.test
    private val percentageY = 0.2f
    private var player: SimpleExoPlayer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(ColorDrawable(0xff000000.toInt()))
        super.onCreate(savedInstanceState)
        if (cache == null) {
            cache = SimpleCache(File(cacheDir, "media"), LeastRecentlyUsedCacheEvictor(MAX_PREVIEW_CACHE_SIZE_IN_BYTES))
        }
        setContentView(R.layout.activity_main)
//        imageView.visibility = View.INVISIBLE
        imageView.setImageResource(imageResId)
        imageView.doOnPreDraw {
            imageView.scaleType = ImageView.ScaleType.MATRIX
            val matrix = Matrix()
            val imageWidth: Float = ContextCompat.getDrawable(this, imageResId)!!.intrinsicWidth.toFloat()
            val viewWidth: Float = imageView.width.toFloat()
            val scaleFactor = viewWidth / imageWidth
            matrix.postScale(scaleFactor, scaleFactor, 0f, 0f)
//            matrix.postTranslate(0f, additionalTranslationY)
            imageView.imageMatrix = matrix
            imageView.setImageResource(imageResId)
//            imageView.imageMatrix = prepareMatrix(imageView, imageView.drawable.intrinsicWidth.toFloat(), imageView.drawable.intrinsicHeight.toFloat())
//            imageView.imageMatrix = prepareMatrix(imageView, imageView.drawable.intrinsicWidth.toFloat(), imageView.drawable.intrinsicHeight.toFloat())
//            imageView.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        playVideo()
    }

    private fun playVideo() {
        player = ExoPlayerFactory.newSimpleInstance(this@MainActivity, DefaultTrackSelector())
        player!!.setVideoTextureView(textureView)
        player!!.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                Log.d("AppLog", "onVideoSizeChanged: $width,$height angle:$unappliedRotationDegrees")
//                val matrix = prepareMatrix(textureView, videoWidth.toFloat(), videoHeight.toFloat())
                val matrix = Matrix()
                val viewWidth = textureView.width.toFloat()
                val viewHeight = textureView.height.toFloat()
                val videoWidth = width.toFloat()
                val videoHeight = height.toFloat()
                val scaleX = 1.0f
                val scaleY: Float = (viewWidth / viewHeight) * (videoHeight / videoWidth)
                matrix.postScale(scaleX, scaleY, 0f, 0f)
//                matrix.postTranslate(0f, additionalTranslationY)
                textureView.setTransform(matrix)
            }

            override fun onRenderedFirstFrame() {
                Log.d("AppLog", "onRenderedFirstFrame")
                player!!.removeVideoListener(this)
//                imageView.animate().alpha(0f).setDuration(5000).start()
                imageView.visibility = View.INVISIBLE
            }
        })
        player!!.volume = 0f
        player!!.repeatMode = Player.REPEAT_MODE_ALL
        player!!.playRawVideo(this, videoResId)
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
