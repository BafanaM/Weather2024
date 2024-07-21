package utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import com.airbnb.lottie.BuildConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

fun lightStatusBar(activity: Activity, value:Boolean){
    if (value){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}

fun Int.unixTimestampToDateTimeString() : String {

    try {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = this*1000.toLong()

        val outputDateFormat = SimpleDateFormat("dd MMM, yyyy - hh:mm a", Locale.ENGLISH)
        outputDateFormat.timeZone = TimeZone.getDefault()
        return outputDateFormat.format(calendar.time)

    } catch (e: Exception) {
        e.printStackTrace()
    }

    return this.toString()
}

fun showToast(context: Context, message:String, length:Int) {
    Toast.makeText(context,message,length).show()
}

fun Int.unixTimestampToTimeString() : String {

    try {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = this*1000.toLong()

        val outputDateFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        outputDateFormat.timeZone = TimeZone.getDefault()
        return outputDateFormat.format(calendar.time)

    } catch (e: Exception) {
        e.printStackTrace()
    }

    return this.toString()
}

