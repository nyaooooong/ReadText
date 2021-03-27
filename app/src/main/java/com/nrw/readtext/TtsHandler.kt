package com.nrw.readtext

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener;

class TtsHandler() : UtteranceProgressListener(), TextToSpeech.OnInitListener {
    lateinit var tts: TextToSpeech;

    // for TextToSpeech.OnInitListener
    override fun onInit(status: Int) {
        TODO("Not yet implemented")
    }

    // for UtteranceProgressListener
    override fun onStart(utteranceId: String?) {
        TODO("Not yet implemented")
    }

    // for UtteranceProgressListener
    override fun onDone(utteranceId: String?) {
        TODO("Not yet implemented")
    }

    // for UtteranceProgressListener
    override fun onError(utteranceId: String?) {
        TODO("Not yet implemented")
    }

}