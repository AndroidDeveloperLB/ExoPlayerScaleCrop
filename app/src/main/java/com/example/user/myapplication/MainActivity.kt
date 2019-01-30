package com.example.user.myapplication

import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.TextureView
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

// https://stackoverflow.com/questions/54216273/how-to-have-similar-mechanism-of-center-crop-on-exoplayers-playerview-but-not
class MainActivity : AppCompatActivity() {
    companion object {
        private val FOCAL_POINT = PointF(0.5f, 0.2f)
        //        private val CENTER_CROP_FOCAL_POINT = PointF(0.5f, 0.5f)
        private const val IMAGE_RES_ID = R.drawable.test
        private const val VIDEO_RES_ID = R.raw.test
        private var cache: Cache? = null
        private const val MAX_PREVIEW_CACHE_SIZE_IN_BYTES = 20L * 1024L * 1024L

        @JvmStatic
        fun getUserAgent(context: Context): String {
            val packageManager = context.packageManager
            val info = packageManager.getPackageInfo(context.packageName, 0)
            val appName = info.applicationInfo.loadLabel(packageManager).toString()
            return Util.getUserAgent(context, appName)
        }
    }

    private var player: SimpleExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (cache == null)
            cache = SimpleCache(File(cacheDir, "media"), LeastRecentlyUsedCacheEvictor(MAX_PREVIEW_CACHE_SIZE_IN_BYTES))
        //        imageView.visibility = View.INVISIBLE
        imageView.setImageResource(IMAGE_RES_ID)
    }

    private fun prepareMatrix(view: View, mediaWidth: Float, mediaHeight: Float, focalPoint: PointF): Matrix? {
        if (view.visibility == View.GONE)
            return null
        val viewHeight = (view.height - view.paddingTop - view.paddingBottom).toFloat()
        val viewWidth = (view.width - view.paddingLeft - view.paddingRight).toFloat()
        if (viewWidth <= 0 || viewHeight <= 0)
            return null
        val matrix = Matrix()
        if (view is TextureView)
        // Restore true media size for further manipulation.
            matrix.setScale(mediaWidth / viewWidth, mediaHeight / viewHeight)
        val scaleFactorY = viewHeight / mediaHeight
        val scaleFactor: Float
        var px = 0f
        var py = 0f
        if (mediaWidth * scaleFactorY >= viewWidth) {
            // Fit height
            scaleFactor = scaleFactorY
            px = -(mediaWidth * scaleFactor - viewWidth) * focalPoint.x / (1 - scaleFactor)
        } else {
            // Fit width
            scaleFactor = viewWidth / mediaWidth
            py = -(mediaHeight * scaleFactor - viewHeight) * focalPoint.y / (1 - scaleFactor)
        }
        matrix.postScale(scaleFactor, scaleFactor, px, py)
        return matrix
    }

    private fun playVideo() {
        player = ExoPlayerFactory.newSimpleInstance(this@MainActivity, DefaultTrackSelector())
        player!!.setVideoTextureView(textureView)
        player!!.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(videoWidth: Int, videoHeight: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(videoWidth, videoHeight, unappliedRotationDegrees, pixelWidthHeightRatio)
                textureView.setTransform(prepareMatrix(textureView, videoWidth.toFloat(), videoHeight.toFloat(), FOCAL_POINT))
            }

            override fun onRenderedFirstFrame() {
                //                Log.d("AppLog", "onRenderedFirstFrame")
                player!!.removeVideoListener(this)
                imageView.animate().alpha(0f).setDuration(2000).start()
                //                imageView.visibility = View.INVISIBLE
            }
        })
        player!!.volume = 0f
        player!!.repeatMode = Player.REPEAT_MODE_ALL
        player!!.playRawVideo(this, VIDEO_RES_ID)
        player!!.playWhenReady = true
        //        player!!.playVideoFromUrl(this, "https://sample-videos.com/video123/mkv/240/big_buck_bunny_240p_20mb.mkv", cache!!)
        //        player!!.playVideoFromUrl(this, "https://sample-videos.com/video123/mkv/720/big_buck_bunny_720p_1mb.mkv", cache!!)
        //        player!!.playVideoFromUrl(this@MainActivity, "https://sample-videos.com/video123/mkv/720/big_buck_bunny_720p_1mb.mkv")
    }

    override fun onStart() {
        super.onStart()
        imageView.doOnPreDraw {
            val imageWidth: Float = imageView.drawable.intrinsicWidth.toFloat()
            val imageHeight: Float = imageView.drawable.intrinsicHeight.toFloat()
            imageView.imageMatrix = prepareMatrix(imageView, imageWidth, imageHeight, FOCAL_POINT)
        }
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

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations)
            cache?.release()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var url: String? = null
        when (item.itemId) {
            R.id.menuItem_all_my_apps -> url = "https://play.google.com/store/apps/developer?id=AndroidDeveloperLB"
            R.id.menuItem_all_my_repositories -> url = "https://github.com/AndroidDeveloperLB"
            R.id.menuItem_current_repository_website -> url = "https://github.com/AndroidDeveloperLB/ExoPlayerScaleCrop"
        }
        if (url == null)
            return true
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        startActivity(intent)
        return true
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
