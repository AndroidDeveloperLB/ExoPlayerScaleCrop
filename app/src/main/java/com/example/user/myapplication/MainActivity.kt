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

// https://stackoverflow.com/questions/54216273/how-to-have-similar-mechanism-of-center-crop-on-exoplayers-playerview-but-not
class MainActivity : AppCompatActivity() {
    private val imageResId = R.drawable.test
    private val videoResId = R.raw.test
    private var player: SimpleExoPlayer? = null
    // How much image to crop from the top. 0.5 is the top half of the image. 1.0f is all of it.
    private val cropPercentage = 1.0f

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
            val imageWidth = imageView.drawable.intrinsicWidth.toFloat()
            val imageHeight = imageView.drawable.intrinsicHeight.toFloat()
            imageView.imageMatrix = prepareMatrix(imageView, imageWidth, imageHeight, cropPercentage, Matrix())
        }
    }

    private fun getViewWidth(view: View): Float {
        return (view.width - view.paddingStart - view.paddingEnd).toFloat()
    }

    private fun getViewHeight(view: View): Float {
        return (view.height - view.paddingTop - view.paddingBottom).toFloat()
    }

    private fun prepareMatrix(targetView: View, contentWidth: Float, contentHeight: Float, focalPercentageFromTop: Float, matrix: Matrix): Matrix {
        if (targetView.visibility != View.VISIBLE)
            return matrix
        val viewHeight = getViewHeight(targetView)
        val viewWidth = getViewWidth(targetView)
        val scaleFactorY = viewHeight / contentHeight
        val scaleFactor: Float
        val px: Float
        val py: Float
        if (contentWidth * scaleFactorY >= viewWidth) {
            // Fit height
            scaleFactor = scaleFactorY
            px = -(contentWidth * scaleFactor - viewWidth) / 2 / (1 - scaleFactor)
            py = 0f
        } else {
            // Fit width
            scaleFactor = viewWidth / contentWidth
            px = 0f
            py = -(contentHeight * scaleFactor - viewHeight) * focalPercentageFromTop / (1 - scaleFactor)
        }
        matrix.postScale(scaleFactor, scaleFactor, px, py)
        return matrix
    }

    private fun playVideo() {
        player = ExoPlayerFactory.newSimpleInstance(this@MainActivity, DefaultTrackSelector())
        player!!.setVideoTextureView(textureView)
        player!!.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                val matrix = Matrix()
                matrix.setScale(width / getViewWidth(textureView), height / getViewHeight(textureView))
                textureView.setTransform(prepareMatrix(textureView, width.toFloat(), height.toFloat(), cropPercentage, matrix))

            }

            override fun onRenderedFirstFrame() {
                Log.d("AppLog", "onRenderedFirstFrame")
                player!!.removeVideoListener(this)
                imageView.animate().alpha(0f).setDuration(2000).start()
//                imageView.visibility = View.INVISIBLE
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

    override fun onStart() {
        super.onStart()
        playVideo()
    }

    override fun onStop() {
        super.onStop()
        if (player != null) {
            player!!.setVideoTextureView(null)
            //        playerView.player = null
            player!!.release()
            player = null
        }
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
