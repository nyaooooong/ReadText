# ReadText Project

텍스트파일을 읽어서 화면에 내용을 출력하고 Google TTS로 읽어주는 안드로이드 앱을 만드는 프로젝트입니다.
[editor on GitHub](https://github.com/nyaooooong/ReadText/edit/main/README.md)

## 1단계 : Scroll App 
안드로이드 scroll activity sample을 기초로 시작합니다.

## 2단계 : Intent.ACTION_OPEN_DOCUMENT로 file open
intent 의 type과 category를 다음과 같이 설정해줍니다.
```kotlin
val intentForTextFileUri: Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "text/plain"
}
```

intent를 다른 앱에 던져서 결과를 받아오려면 startActivityForResult를 사용해야 합니다.
REQUEST_CODE는 내가 무슨 용도로 던진 것인지 확인하기 위한 flag 같은 것이다.
```kotlin
startActivityForResult(intentForTextFileUri, REQUEST_CODE)
```
intent를 던진 결과를 받아보려면 onActivityResult를 구현해야 합니다.
이 때 requestCode와 resultCode로 내가 보낸 용도와 수행결과를 확인합니다.
결과가 정상이면 Intent의 data로 open할 file의 URI를 받습니다.
Intent의 data는 항상 URI입니다.
```kotlin
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
        Log.i(TAG, """${mainTextView.getMaxScrollAmount()}/${mainTextView.computeVerticalScrollRange()}""");
    }
}
```

## 3단계 : TTS적용
Google TTS engine을 이용하여 읽어들인 파일의 text를 TTS로 읽도록 합니다.
TTS API를 이용하려면 TextToSpeech.OnInitListener를 상속받아 onInit함수를 구현해야 합니다.
```kotlin
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
```
TTS API의 instance는 아무 때나 사용할 때 생성하면 되지만 main activity의 class에서 생성하고 사용해야 한다는 것에 주의 해야 합니다.
이 프로젝트의 경우 onCreate에서 생성하고 FloatingActionButton의 onClickListener에서 사용하도록 했습니다.
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    ...
    ttsEngine = TextToSpeech(this, this);
    ...
    findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
        ttsEngine.speak(textHandler.getText(), TextToSpeech.QUEUE_ADD, null, null);
    }
```

## 4단계 : 자동 Encoding 확인
Mozilla의 UniversalDetector를 이용하여 해당 text파일의 encoding을 자동으로 확인하도록 합니다.
[org.mozilla.universalchardet](https://github.com/albfernandez/juniversalchardet)를 다운받아서 app/src/main/java에 넣고 import합니다.
```kotlin
import org.mozilla.universalchardet.UniversalDetector
```
build.gradle에 "implementation 'com.github.albfernandez:juniversalchardet:2.4.0'" 넣어주면 된다는데 하는 법을 모르겠어서 그냥 소스를 넣었습니다.
UniversalDetector의 사용법은 간단합니다. 2단계에서 얻은 URI로 input stream을 열어서 UniversalDetector.detectCharset에 넣어주면 됩니다.
return값은 String 입니다.
```kotlin
contentResolver.openInputStream(uri).use { inputStream -> 
    this.encoding = UniversalDetector.detectCharset(inputStream)
}
```

## 5단계 : preference 마지막으로 읽은 위치 save & load
onDestroy 시 마지막으로 읽은 위치를 저장했다가 나중에 다시 앱을 기동했을 때 여기서부터 읽을 수 있다면 편할 것입니다.
이를 위해 [Preference API](https://developer.android.com/training/data-storage/shared-preferences?hl=ko)를 사용합니다.
onDestroy 에 다음과 같은 코드를 넣어서 마지막으로 읽은 위치를 저장하고
```kotlin
override fun onDestroy() {
...
    val pref = this.getPreferences(Context.MODE_PRIVATE) ?: return
    with (pref.edit()) {
        putString(getString(R.string.key_uri), textHandler.getUriString())
        putLong(getString(R.string.key_position), position)
        commit()
    }
...
}
```
menu에 last read 라는 item을 넣어서 해당 아이템을 선택하면 마지막으로 읽은 URI와 위치를 loading합니다.
```kotlin
R.id.menu_item_last_read -> {
    val pref = this.getPreferences(Context.MODE_PRIVATE) ?: return false;
    val uriString = pref.getString(getString(R.string.key_uri), "")
    position = pref.getLong(getString(R.string.key_position), 0)

    return true;
}
```

아직 이 기능은 다 구현되지 않았습니다.