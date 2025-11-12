package cafe.jiahui.portalscreenhelper.utils

import com.github.promeg.pinyinhelper.Pinyin

object PinyinUtils {
    fun toPinyin(text: String): String {
        return Pinyin.toPinyin(text, "").lowercase()
    }
    
    fun compareAppNames(appName1: String, appName2: String): Int {
        val pinyin1 = toPinyin(appName1)
        val pinyin2 = toPinyin(appName2)
        return pinyin1.compareTo(pinyin2, ignoreCase = true)
    }
}