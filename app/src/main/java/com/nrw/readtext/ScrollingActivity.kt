package com.nrw.readtext

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.*

class ScrollingActivity : AppCompatActivity(), TextToSpeech.OnInitListener, GoToDialogFragment.GotoDialogListener, ViewTreeObserver.OnGlobalLayoutListener {
    private lateinit var mainTextScroll: NestedScrollView;
    private lateinit var ttsEngine: TextToSpeech; // TextToSpeech should be used in main activity
    private lateinit var ttsWorker: TtsHandler;
    private lateinit var textHandler: TextHandler;
    private var position: Int = 0;

    companion object {
        internal const val REQUEST_CODE = 0
        internal const val TAG: String = "READ_TEXT";
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        position = 0;
        ttsEngine = TextToSpeech(this, this);
        textHandler = TextHandler(this);

        setContentView(R.layout.activity_scrolling)
        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = title
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener( fun(_: View) {
                    if (textHandler.getTextLength() > 0) {
                        ttsEngine.speak(textHandler.getTextName(), TextToSpeech.QUEUE_ADD, null, null);
                    }
                })
        mainTextScroll = findViewById<NestedScrollView>(R.id.main_text_scroll);
        mainTextScroll.setOnScrollChangeListener { view, x, y, oldx, oldy ->
            position = y; //y == mainTextScroll.computeVerticalScrollOffset()
             /* mainTextScroll.computeVerticalScrollRange() is size of all contents. It is not mainTextScroll.getMaxScrollAmount()
             * mainTextScroll.computeVerticalScrollExtent() is size of screen
             */
            // Snackbar can have only one action. if you want to add more action, use dialog. but it is annoying for user.
            Snackbar.make(view, "position:" + y, Snackbar.LENGTH_LONG)
                    .setAction("Mark", View.OnClickListener() {
                        fun onClick(view: View) {
                        }
                    }).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.menu_item_select_file -> {
                startActivityForResult(TextHandler.intentForTextFileUri, REQUEST_CODE)
                return true;
            }
            R.id.menu_item_last_read -> {
                val pref = this.getPreferences(Context.MODE_PRIVATE) ?: return false;
                val uriString = pref.getString(getString(R.string.key_uri), "")
                position = pref.getInt(getString(R.string.key_position), 0)

                if (uriString!!.length > 0) {
                    onSuccessGetUri(Uri.parse(uriString));
                    Log.i(TAG, "URI:" + uriString + ",Pos:" + position)
                }

                return true;
            }
            R.id.menu_item_go_to -> {
                val gotodialog = GoToDialogFragment();
                gotodialog.show(supportFragmentManager, "gotodialog");

                return true;
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // User navigate to this activity from onStop
    override fun onRestart() {
        super.onRestart()
        Log.v(TAG, "onRestart");

    }

    // User returns this activity from onPause
    override fun onResume() {
        super.onResume();
        Log.v(TAG, "onResume");

    }

    // Another activity comes into the foreground
    // In this state, this activity can be killed when need more memory
    override fun onPause() {
        super.onPause()
        Log.v(TAG, "onPause");
    }

    // This activity no longer visible
    // In this state, this activity can be killed when need more memory
    override fun onStop() {
        super.onStop()
        Log.v(TAG, "onStop");
    }

    // This activity is finishing or destroyed by system
    override fun onDestroy() {
        Log.v(TAG, "onDestroy");
        val pref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        with (pref.edit()) {
            putInt(getString(R.string.key_position), position)
            putString(getString(R.string.key_uri), textHandler.getUriString())
            commit()
        }
        Log.i(TAG, "URI:" + textHandler.getUriString() + ",Pos:" + position)
        ttsEngine.stop();
        ttsEngine.shutdown();
        super.onDestroy()
    }

    // implements TextToSpeech.OnInitListener which is the second parameter of TextToSpeech()
    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(ScrollingActivity.TAG, "Fail to instantiate TextToSpeech");
            return;
        }
        val result = this.ttsEngine.setLanguage(Locale.KOREAN)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(ScrollingActivity.TAG, "Fail to setLanguage of TTS");
            return;
        }
    }

    // when top : 30 line
    // when !top : 35 line
    override fun onActivityResult(requestCode: Int, resultCode: Int, returnIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode != REQUEST_CODE)
            return;
        if (resultCode != RESULT_OK) {
            Snackbar.make(findViewById(R.id.toolbar), "Failed", Snackbar.LENGTH_SHORT).show();
            return;
        }
        returnIntent?.data?.let { returnUri -> // intent.data는 Uri 임.
            onSuccessGetUri(returnUri);
        }
    }

    private fun onSuccessGetUri(uri: Uri) {
        textHandler.initialize(uri)
        findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = textHandler.getTextName();
        findViewById<TextView>(R.id.main_text).text = textHandler.readText() // !! 는 nullable을 non-null로 변경해 줌. 여기서는 흐름상 data가 null일 수 없음.
        mainTextScroll.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onDialogPositiveClick(dialog: DialogFragment, position: String) {
        var rect: Rect = Rect();
        val lineBound = findViewById<TextView>(R.id.main_text).getLineBounds(position.toInt(), rect);
        findViewById<NestedScrollView>(R.id.main_text_scroll).scrollTo(0, lineBound)
        findViewById<TextView>(R.id.main_text).getLineBounds(1, rect);
    }

    override fun getMaxLineCount(): String {
        return textHandler.total_viewline_count.toString();
    }

    override fun onGlobalLayout() {
        textHandler.total_viewline_count = findViewById<TextView>(R.id.main_text).getLineCount();
        findViewById<NestedScrollView>(R.id.main_text_scroll).scrollTo(0, position)
        mainTextScroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
}