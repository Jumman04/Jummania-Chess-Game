package com.jummania.chess_game.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jummania.ChessView
import com.jummania.chess_game.MainActivity
import com.jummania.chess_game.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


/**
 * Created by Jummania on 13/4/25.
 * Email: sharifuddinjumman@gmail.com
 * Dhaka, Bangladesh.
 */
class GameFragment : Fragment(R.layout.fragment_game) {
    private val client by lazy { (activity as MainActivity).client }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mActivity = activity ?: return

        val chessView = view.findViewById<ChessView>(R.id.chessView)

        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(mActivity)

        preferenceManager?.apply {

            /*
            val playMusic = getBoolean("playMusic", false)
            if (playMusic) {
                mediaPlayer = MediaPlayer.create(mActivity, R.raw.music)
                playBackground(mediaPlayer!!)
            }

             */

            chessView.setSoundEffectEnabled(getBoolean("clickSound", true))

            chessView.setBackgroundColor(getInt("backgroundColor", Color.WHITE))

            chessView.setPieceStyle(
                getBoolean("isLightFilled", true),
                getBoolean("isDarkFilled", true),
                getInt("pieceLightColor", Color.WHITE),
                getInt("pieceDarkColor", Color.BLACK)
            )

            chessView.setSquareColors(
                getInt("lightSquareColor", "#fadeaf".toColorInt()),
                getInt("darkSquareColor", "#8e4f19".toColorInt())
            )

            chessView.setEnableStroke(
                getBoolean("enableStroke", true),
                getInt("strokeLightColor", Color.BLACK),
                getInt("strokeDarkColor", Color.BLACK)
            )

            chessView.setSymbolStyle(
                getString("symbolStyle", "3")!!.toInt(), getBoolean("isBoldSymbol", false)
            )
        }

        MaterialAlertDialogBuilder(mActivity).setTitle("Play with Gemini?")
            .setPositiveButton("Yes") { _, _ ->
                chessView.withOnlinePlayer { friends, enemies ->
                    val prompt =
                        "You are a chess AI playing as White. The board is indexed from 0 (top-left, A8) to 63 (bottom-right, H1).\n\nYour pieces:\n$friends\n\nOpponent's pieces:\n$enemies\n\nFollow these movement rules:\n- Pawn: +8 (move), +7/+9 (capture)\n- Knight: ±6, ±10, ±15, ±17 (L-shape)\n- Bishop: ±7, ±9 (diagonals)\n- Rook: ±1 (row), ±8 (column)\n- Queen: bishop + rook moves\n- King: ±1, ±7, ±8, ±9 (1 square any direction)\n\nIt's your turn. Respond with one legal move only in this format:\nfromPosition, toPosition\nExample: 52, 36"

                    submit(prompt) { from, to ->
                        mActivity.runOnUiThread {
                            chessView.swapTo(from, to, true)
                        }

                    }
                }
            }.setNegativeButton("No", null).show()


        val mWindow = mActivity.window
        WindowInsetsControllerCompat(
            mWindow, mActivity.findViewById(android.R.id.content)
        ).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        mWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            MaterialAlertDialogBuilder(mActivity).setTitle("Are you sure you want to exit?")
                .setMessage("Any unsaved progress may be lost.").setPositiveButton("Exit") { _, _ ->
                    mActivity.finish()
                }.setNegativeButton("Cancel", null).show()
        }
    }

    private fun submit(prompt: String, onResponse: (from: Int, to: Int) -> Unit) {
        val request =
            Request.Builder().url("https://lmnx9.xyz/ai/gemini.php?text=$prompt").get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Jjj", "onFailure: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    it.body?.use { body ->

                        val jsonObject = JSONObject(body.string())
                        val string = jsonObject.getJSONObject("LMNx9").getString("Response")
                        if (string.contains(",")) {
                            val split = string.split(",").map { number -> number.trim().toInt() }
                            onResponse(split.first(), split.last())
                        } else if (string == "false") {
                            call.clone().enqueue(this)
                        }

                    }
                }

            }
        })

    }

    /*
    private fun playBackground(mediaPlayer: MediaPlayer) {
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.seekTo(mediaPlayer.duration / 2)
            mediaPlayer.start()
        }

        mediaPlayer.start()
    }

     */


}