package ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.weather2024.R
import kotlinx.android.synthetic.main.item_forecast.view.iv_weather_icon
import kotlinx.android.synthetic.main.item_forecast.view.tv_time_forecast
import kotlinx.android.synthetic.main.item_forecast.view.tv_weather_condition
import kotlinx.android.synthetic.main.layout_forecast_info.view.tv_day_feel
import kotlinx.android.synthetic.main.layout_forecast_info.view.tv_day_temp
import kotlinx.android.synthetic.main.layout_forecast_info.view.tv_eve_feel
import kotlinx.android.synthetic.main.layout_forecast_info.view.tv_eve_temp
import kotlinx.android.synthetic.main.layout_forecast_info.view.tv_max_temp
import kotlinx.android.synthetic.main.layout_forecast_info.view.tv_min_temp
import kotlinx.android.synthetic.main.layout_forecast_info.view.tv_morn_feel
import kotlinx.android.synthetic.main.layout_forecast_info.view.tv_night_feel
import kotlinx.android.synthetic.main.layout_forecast_info.view.tv_night_temp
import kotlinx.android.synthetic.main.layout_forecast_info.view.tv_sunrise_time
import kotlinx.android.synthetic.main.layout_forecast_info.view.tv_sunset_time
import model.Daily
import utils.DiffUtilCallbackForecast
import utils.unixTimestampToDateTimeString
import utils.unixTimestampToTimeString

class ForecastAdapter : RecyclerView.Adapter<ForecastAdapter.Holder>() {

    val differ = AsyncListDiffer(this, DiffUtilCallbackForecast())

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_forecast, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val data = differ.currentList[position]
        Log.d("ForecastAdapter", "Binding data at position $position: $data")
        bindData(holder, data)
    }

    @SuppressLint("SetTextI18n")
    private fun bindData(holder: Holder, data: Daily?) {
        if (data == null) {
            Log.e("ForecastAdapter", "Data is null at this position")
            return
        }

        val weatherConditionIconUrl = "http://openweathermap.org/img/w/${data.weather[0].icon}.png"
        holder.itemView.apply {
            tv_time_forecast.text = data.dt.unixTimestampToDateTimeString()
            if (!(context as Activity).isFinishing) Glide.with(context).load(weatherConditionIconUrl).into(iv_weather_icon)
            tv_weather_condition.text = data.weather[0].main
            tv_day_temp.text = "Day\n${data.temp.day}${context.getString(R.string.degree_in_celsius)}"
            tv_eve_temp.text = "Evening\n${data.temp.eve}${context.getString(R.string.degree_in_celsius)}"
            tv_night_temp.text = "Night\n${data.temp.night}${context.getString(R.string.degree_in_celsius)}"
            tv_max_temp.text = "Max\n${data.temp.max}${context.getString(R.string.degree_in_celsius)}"
            tv_min_temp.text = "Min\n${data.temp.min}${context.getString(R.string.degree_in_celsius)}"

            tv_morn_feel.text = "Morning\n${data.feelsLike.morn}${context.getString(R.string.degree_in_celsius)}"
            tv_day_feel.text = "Day\n${data.feelsLike.day}${context.getString(R.string.degree_in_celsius)}"
            tv_eve_feel.text = "Evening\n${data.feelsLike.eve}${context.getString(R.string.degree_in_celsius)}"
            tv_night_feel.text = "Night\n${data.feelsLike.night}${context.getString(R.string.degree_in_celsius)}"

            tv_sunrise_time.text = data.sunrise.unixTimestampToTimeString()
            tv_sunset_time.text = data.sunset.unixTimestampToTimeString()
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}