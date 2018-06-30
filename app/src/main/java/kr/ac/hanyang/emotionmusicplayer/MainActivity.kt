package kr.ac.hanyang.emotionmusicplayer

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.DialogInterface
import android.hardware.Camera
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.widget.Toast
import android.graphics.Bitmap
import android.media.AudioManager
import com.wonderkiln.camerakit.*
import android.media.FaceDetector
import android.view.*
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.services.vision.v1.Vision
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import com.google.api.services.vision.v1.model.*
import java.io.*
import java.util.*
import android.media.MediaPlayer
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.widget.SeekBar
import kotlinx.android.synthetic.main.popupwindow.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    var mSurfaceView: SurfaceView? = null
    var mSurfaceHolder: SurfaceHolder? = null
    var alert: AlertDialog? = null
    var mp: MediaPlayer? = null
    var start: Runnable? = null

    var handler: Handler = Handler()
    var handlerPlayer: Handler = Handler()

    var musicPlayedTime = 0
    var playWhichSong = 0

    var mCamera: Camera? = null
    var bitmapEx: Bitmap? = null
    var startChecking: Boolean = false
    var UsedGoogleAPI: Boolean = false
    var faceDetectCompleted: Boolean = false
    var playOrNot: Boolean = false

    var joy = ""
    var anger = ""
    var sorrow = ""
    var surprise = ""
    var currentEmotion = ""
    var fourKindsOfEmotion = arrayOf(joy, sorrow, anger, surprise)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        seekBar.isEnabled = false
        cameraView.setMethod(CameraKit.Constants.METHOD_STILL)
        cameraView.addCameraKitListener(object : CameraKitEventListener {
            override fun onVideo(p0: CameraKitVideo?) {}

            override fun onEvent(p0: CameraKitEvent?) {}

            override fun onImage(p0: CameraKitImage?) {
                bitmapEx = p0!!.bitmap
                Log.e(">>>>>>>>>>", "taking photo")
                Log.e(">>>>>>>>>>", "taking face" + findFace(bitmapEx!!))
                handler.sendEmptyMessage(3)
            }

            override fun onError(p0: CameraKitError?) {}
        })

        var mRunnable = Runnable {
            run {
                while (startChecking == false) {
                    Thread.sleep(500)
                    handler.sendEmptyMessage(0)

                    Thread.sleep(1500)
                    handler.sendEmptyMessage(1)
                }
            }
        }

        round_iv.setOnTouchListener { v, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                round_iv.isEnabled = false
                // 防止线程闭塞
                Thread(mRunnable).start()
            }

            return@setOnTouchListener false
        }

        handler = object : Handler() {
            override fun handleMessage(msg: android.os.Message) {
                when (msg.what) {
                    0 -> {
                        setTipsAndTips2Text("Scaning your face now", "please wait patiently")
                        startChecking = true
                        loadingbutton.setTargetProgress(100f)
                        cameraView.captureImage()
                    }
                    1 -> {
                        setTipsAndTips2Text("Analyzing your emotion", "please wait patiently")
                        loadingbutton.setTargetProgress(180f)
                        if (findFace(bitmapEx!!) > 0) {
                            googleApi()
                        }
                    }
                    2 -> {

                        setTips3AndTips4Text("Happy", "Sasha Grey")
                        textAE(1)
                        when (playWhichSong) {
                            1 -> {
                                try {
                                    if (mp!!.isPlaying == true) {
                                        mp!!.reset()
                                    }
                                } catch (e: Exception) {
                                }
                                mp = MediaPlayer.create(this@MainActivity, R.raw.sorrow2joy)
                            }
                        }
                        mp!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                        mp!!.isLooping = true
                        // 音乐文件持续时间
                        time_end.text = returnTime(getAllSongTime())
                        seekBar.setMax(getAllSongTime())
                        playOrNot_iv.setImageDrawable(getDrawable(R.drawable.ic_pause_black_24dp))
                        playOrNot = true
                        handlerPlayer.post(start)
                        playOrNot_iv.isEnabled = true
                        seekBar.isEnabled = true
                        round_cover.visibility = View.VISIBLE
                        faceDetectCompleted = true
                    }
                    3 -> {
                        roundImage.visibility = View.VISIBLE
                        cameraView.stop()
                        cameraView.visibility = View.INVISIBLE
                        textAE(0)


                    }
                }
            }
        }

        val updatesb = object : Runnable {

            override fun run() {
                Log.e("fucking SETprogress>>>>", (mp!!.getCurrentPosition() / 1000).toString())
                seekBar.setProgress(musicPlayedTime)
                if (mp!!.isPlaying) {
                    musicPlayedTime = musicPlayedTime + 1
                    time_start.text = returnTime(musicPlayedTime)
                }
                //每秒钟更新一次
                handlerPlayer.postDelayed(this, 1000)
            }

        }

        start = Runnable {
            mp!!.start()
            handlerPlayer.post(updatesb)
            //用一个handler更新SeekBar
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // TODO Auto-generated method stub
                if (fromUser) {
                    musicPlayedTime = progress
                    time_start.text = returnTime(musicPlayedTime)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                //SeekBar确定位置后，跳到指定位置
                Log.e(">>>>>>>>>>>>>>>>>>>>>", seekBar.getProgress().toString())
                mp!!.seekTo(seekBar.getProgress() * 1000)
            }
        })

        loadingbutton.setCallback(LoadingButton.Callback {
            cameraView.stop()
            dialog()
        })

        playOrNot_iv.setOnClickListener { v ->

            if (faceDetectCompleted == false) {
                Snackbar.make(v, "Please have the face detection first", Snackbar.LENGTH_LONG).setAction("Got it", null).show()
            } else {
                if (playOrNot) {
                    playOrNot_iv.setImageDrawable(getDrawable(R.drawable.ic_play_arrow_black_24dp))
                    playOrNot = false
                    mp!!.pause()
                } else {
                    playOrNot_iv.setImageDrawable(getDrawable(R.drawable.ic_pause_black_24dp))
                    playOrNot = true
                    mp!!.start()
                }
            }
        }

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        faceType_iv.setOnClickListener { v ->
            dialog()
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_add -> {
//                playWhichSong = 1
//                handler.sendEmptyMessage(2)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.about -> {
                var alertAbout = AlertDialog.Builder(this).create()
                alertAbout.setTitle("About APP")
                alertAbout.setMessage("It's our team project of Sound Computing Course. Thank you for downloading this APP and using it.")
                alertAbout.setButton(DialogInterface.BUTTON_POSITIVE, "OK", DialogInterface.OnClickListener { dialogInterface: DialogInterface, i: Int ->
                    alertAbout.dismiss()
                })
                alertAbout.show()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onPause() {
        cameraView.stop()
        super.onPause()
    }

    fun findFace(bitmap: Bitmap): Int {
        // 检测前必须转化为RGB_565格式
        val bitmapEx = bitmap.copy(Bitmap.Config.RGB_565, true)
        //设置最多检测多少个 Face
        val maxFace = 1
        val mFaceDetector = FaceDetector(bitmapEx.width, bitmapEx.height, maxFace)
        val mFace = arrayOfNulls<FaceDetector.Face>(maxFace)
        // 实际检测到的脸数
        return mFaceDetector.findFaces(bitmapEx, mFace)
    }

    fun textAE(int: Int) {
        val alphaAnimator = ObjectAnimator.ofFloat(roundImage, "alpha", 0f, 1f)

        when (int) {
            0 -> {
                alphaAnimator.duration = 3000
                alphaAnimator.interpolator = BreatheInterpolator()
                alphaAnimator.repeatCount = ValueAnimator.INFINITE
                alphaAnimator.start()
            }
            1 -> {
                tips.visibility = View.INVISIBLE
                tips2.visibility = View.INVISIBLE
                tips3.visibility = View.VISIBLE
                tips4.visibility = View.VISIBLE
            }
            2 -> {

            }
        }
    }

    fun setTipsAndTips2Text(str: String, str2: String) {
        tips.text = str
        tips2.text = str2
    }

    fun setTips3AndTips4Text(str: String, str2: String) {
        tips3.text = str
        tips4.text = str2
    }

    fun googleApi() {
        val visionBuilder = Vision.Builder(NetHttpTransport(), AndroidJsonFactory(), null)
        visionBuilder.setVisionRequestInitializer(VisionRequestInitializer("AIzaSyBlQm5S9O_fV0p8s5juH_yIc02jM2DDO7s"))
        val vision = visionBuilder.build()

        // Create new thread
        AsyncTask.execute {

            var inputImage = bitmapToImage(bitmapEx!!)
            // inputImage.encodeContent(photoData)
            val desiredFeature = Feature()
            desiredFeature.setType("FACE_DETECTION")
            var request: AnnotateImageRequest = AnnotateImageRequest()
            request.setImage(inputImage);
            request.setFeatures(Arrays.asList(desiredFeature))
            var batchRequest: BatchAnnotateImagesRequest = BatchAnnotateImagesRequest()
            batchRequest.setRequests(Arrays.asList(request))
            var batchResponse: BatchAnnotateImagesResponse = vision.images().annotate(batchRequest).execute()
            var faces = batchResponse.responses[0].faceAnnotations
            // Count faces
            // var numberOfFaces = faces.size

            try {

                when (faces[0].joyLikelihood) {
                    "VERY_UNLIKELY" -> fourKindsOfEmotion[0] = "0.2"
                    "UNLIKELY" -> fourKindsOfEmotion[0] = "0.4"
                    "POSSIBLE" -> fourKindsOfEmotion[0] = "0.6"
                    "LIKELY" -> fourKindsOfEmotion[0] = "0.8"
                    "VERY_LIKELY" -> fourKindsOfEmotion[0] = "1"
                }

                when (faces[0].sorrowLikelihood) {
                    "VERY_UNLIKELY" -> fourKindsOfEmotion[1] = "0.2"
                    "UNLIKELY" -> fourKindsOfEmotion[1] = "0.4"
                    "POSSIBLE" -> fourKindsOfEmotion[1] = "0.6"
                    "LIKELY" -> fourKindsOfEmotion[1] = "0.8"
                    "VERY_LIKELY" -> fourKindsOfEmotion[1] = "1"
                }

                when (faces[0].angerLikelihood) {
                    "VERY_UNLIKELY" -> fourKindsOfEmotion[2] = "0.2"
                    "UNLIKELY" -> fourKindsOfEmotion[2] = "0.4"
                    "POSSIBLE" -> fourKindsOfEmotion[2] = "0.6"
                    "LIKELY" -> fourKindsOfEmotion[2] = "0.8"
                    "VERY_LIKELY" -> fourKindsOfEmotion[2] = "1"
                }

                when (faces[0].surpriseLikelihood) {
                    "VERY_UNLIKELY" -> fourKindsOfEmotion[3] = "0.2"
                    "UNLIKELY" -> fourKindsOfEmotion[3] = "0.4"
                    "POSSIBLE" -> fourKindsOfEmotion[3] = "0.6"
                    "LIKELY" -> fourKindsOfEmotion[3] = "0.8"
                    "VERY_LIKELY" -> fourKindsOfEmotion[3] = "1"
                }

                when (max()) {
                    0 -> currentEmotion = "joy"
                    1 -> currentEmotion = "sorrow"
                    2 -> currentEmotion = "anger"
                    3 -> currentEmotion = "surprise"
                    4 -> currentEmotion = "calm"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Unrecognized face", Toast.LENGTH_LONG).show()
                    startChecking = false

                }
            }

            runOnUiThread {
                Toast.makeText(this@MainActivity, "Current emotion is $currentEmotion", Toast.LENGTH_LONG).show()
                startChecking = false
                faceType_iv.setImageDrawable(getDrawable(R.drawable.ic_sentiment_very_satisfied_black_24dp))
                setTipsAndTips2Text("Matching the suitable music", "ready to play")

                loadingbutton.setTargetProgress(360f)
            }
        }


    }

    fun bitmapToImage(bitmap: Bitmap): Image {
        var base64EncodedImage: Image = Image()
        var byteArrayOutputStream: ByteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        var imageBytes = byteArrayOutputStream.toByteArray()
        // Base64 encode the JPEG
        base64EncodedImage.encodeContent(imageBytes)
        bitmap.recycle()
        return base64EncodedImage
    }

    fun max(): Int {
        var index = 0
        var count = 0
        var t = 0.0
        for (i in 0..3) {
            Log.e(">>>>>>>>>>", "$i>>>>" + fourKindsOfEmotion[i])
            if (t < fourKindsOfEmotion[i].toDouble()) {
                t = fourKindsOfEmotion[i].toDouble()
                index = i
            }
            if (fourKindsOfEmotion[i].toDouble() == 0.2) {
                count = count + 1

            }
        }
        if (count == 4) {
            index = 4
        }
        return index
    }

    fun main() {
        // update GUI
        // have face detection
        // make sure that there is one more face in image
        // using by google api to analysis the face emotion
        // wait for user to choose the target emotion like...


        // matching the music with user face emotion
        // end>>>
    }

    fun joy(v: View) {
        alert!!.dismiss()
        Toast.makeText(this, "Target emotion is joy", Toast.LENGTH_LONG).show()

        if (currentEmotion == "sorrow") {
            playWhichSong = 1
            handler.sendEmptyMessage(2)
        }
    }

    fun sorrow(v: View) {
        alert!!.dismiss()
    }

    fun anger(v: View) {
        alert!!.dismiss()
    }

    fun calm(v: View) {
        alert!!.dismiss()
        Toast.makeText(this, "Target emotion is joy", Toast.LENGTH_LONG).show()
        if (currentEmotion == "anger") {
            playWhichSong = 1
            handler.sendEmptyMessage(2)
        }


    }

    fun dialog() {
        alert = AlertDialog.Builder(this).create()
        var inflater: LayoutInflater = getLayoutInflater()
        var layout: View = inflater.inflate(R.layout.popupwindow, null)
        alert!!.setTitle("Choose target emotion")
        alert!!.setCancelable(false)
        alert!!.setView(layout)
        alert!!.show()
    }

    fun returnTime(t: Int): String {
        var min = (t / 60).toInt()
        var sec = t % 60
        var ready2returnValue: String
        if (sec <= 9) {
            ready2returnValue = "$min:0$sec"
        } else {
            ready2returnValue = "$min:$sec"
        }
        return ready2returnValue
    }

    fun getAllSongTime(): Int {
        Log.e("fucking time>>>>", mp!!.duration.toString())
        return (mp!!.duration / 1000).toInt()
    }

}