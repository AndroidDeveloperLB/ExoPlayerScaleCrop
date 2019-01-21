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
            imageView.imageMatrix = prepareMatrixForImageView(imageView, imageView.drawable.intrinsicWidth.toFloat(), imageView.drawable.intrinsicHeight.toFloat())
//            imageView.imageMatrix = prepareMatrix(imageView, imageView.drawable.intrinsicWidth.toFloat(), imageView.drawable.intrinsicHeight.toFloat())
//            imageView.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        playVideo()
    }

    private fun prepareMatrix(view: View, contentWidth: Float, contentHeight: Float): Matrix {
        var scaleX = 1.0f
        var scaleY = 1.0f
        val viewWidth = view.measuredWidth.toFloat()
        val viewHeight = view.measuredHeight.toFloat()
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
        val pivotPercentageX = 0.5f
        val pivotPercentageY = percentageY

        matrix.setScale(scaleX, scaleY, viewWidth * pivotPercentageX, viewHeight * pivotPercentageY)
        return matrix
    }

    private fun prepareMatrixForVideo(view: View, contentWidth: Float, contentHeight: Float): Matrix {
        val msWidth = view.measuredWidth
        val msHeight = view.measuredHeight
        val matrix = Matrix()
        matrix.setScale(1f, (contentHeight / contentWidth) * (msWidth.toFloat() / msHeight), msWidth / 2f, percentageY * msHeight) /*,msWidth/2f,msHeight/2f*/
        return matrix
    }

    private fun prepareMatrixForImageView(view: View, contentWidth: Float, contentHeight: Float): Matrix {
        val dw = contentWidth
        val dh = contentHeight
        val msWidth = view.measuredWidth
        val msHeight = view.measuredHeight
//        Log.d("AppLog", "viewWidth $msWidth viewHeight $msHeight contentWidth:$contentWidth contentHeight:$contentHeight")
        val scalew = msWidth.toFloat() / dw
        val theoryh = (dh * scalew).toInt()
        val scaleh = msHeight.toFloat() / dh
        val theoryw = (dw * scaleh).toInt()
        val scale: Float
        var dx = 0
        var dy = 0
        if (scalew > scaleh) { // fit width
            scale = scalew
//            dy = ((msHeight - theoryh) * 0.0f + 0.5f).toInt() // + 0.5f for rounding
        } else {
            scale = scaleh
            dx = ((msWidth - theoryw) * 0.5f + 0.5f).toInt() // + 0.5f for rounding
        }
        dy = ((msHeight - theoryh) * percentageY + 0.5f).toInt() // + 0.5f for rounding
        val matrix = Matrix()
//        Log.d("AppLog", "scale:$scale dx:$dx dy:$dy")
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx.toFloat(), dy.toFloat())
        return matrix
    }

    private fun playVideo() {
        player = ExoPlayerFactory.newSimpleInstance(this@MainActivity, DefaultTrackSelector())
        player!!.setVideoTextureView(textureView)
        player!!.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                Log.d("AppLog", "onVideoSizeChanged: $width,$height angle:$unappliedRotationDegrees")
                val videoWidth = if (unappliedRotationDegrees % 180 == 0) width else height
                val videoHeight = if (unappliedRotationDegrees % 180 == 0) height else width
                val matrix = prepareMatrixForVideo(textureView, videoWidth.toFloat(), videoHeight.toFloat())
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
