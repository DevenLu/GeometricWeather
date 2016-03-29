package wangdaye.com.geometricweather.View;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;

import java.util.Calendar;
import java.util.List;

import wangdaye.com.geometricweather.Data.JuheWeather;
import wangdaye.com.geometricweather.Data.Location;
import wangdaye.com.geometricweather.Data.MyDatabaseHelper;
import wangdaye.com.geometricweather.Data.WeatherInfoToShow;
import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.Widget.BitmapHelper;
import wangdaye.com.geometricweather.Widget.HourlyView;
import wangdaye.com.geometricweather.Widget.MyScrollView;
import wangdaye.com.geometricweather.Widget.MySwipeRefreshLayout;
import wangdaye.com.geometricweather.Widget.MyCardView;
import wangdaye.com.geometricweather.Widget.SkyView;
import wangdaye.com.geometricweather.Widget.DailyView;

/**
 * A fragment to show weather information.
 * */

public class WeatherFragment extends Fragment
        implements BDLocationListener,
        ManageDialog.SetLocationListener,
        View.OnClickListener,
        View.OnTouchListener,
        SwipeRefreshLayout.OnRefreshListener, MyScrollView.OnScrollViewListener {
    // widget
    private Toolbar toolbar;
    private SkyView skyView;
    private ImageView[] start;
    private ImageView[] weatherIcon;

    private TextView aqiTextLive;
    private TextView weatherTextLive;
    private TextView timeTextLive;
    private TextView locationTextLive;
    public ImageView locationCollect;

    private TextView weekWeatherTitle;
    private TextView lifeInfoTitle;

    private LinearLayout[] weekInfo;
    private ImageView[] weekIcon;
    private TextView[] weekText;

    private DailyView weatherDailyView;
    private TextView poweredText;
    private FrameLayout trendContainer;

    private HourlyView weatherHourlyView;
    private FrameLayout hourlyContainer;

    private TextView windKind;
    private TextView windInfo;
    private TextView pmKind;
    private TextView pmInfo;
    private TextView waterKind;
    private TextView waterInfo;
    private TextView sunKind;
    private TextView sunInfo;
    private TextView wearKind;
    private TextView wearInfo;
    private TextView illKind;
    private TextView illInfo;
    private TextView airKind;
    private TextView airInfo;
    private TextView washCarKind;
    private TextView washCarInfo;
    private TextView sportKind;
    private TextView sportInfo;

    private MySwipeRefreshLayout swipeRefreshLayout;
    private MyCardView weatherCard;
    private MyCardView lifeCard;

    private GestureDetector gestureDetectorPage;
    private GestureDetector gestureDetectorTrendView;
    private GestureDetector gestureDetectorHourlyView;

    //animator
    public AnimatorSet[] animatorSetsAnimal;
    public AnimatorSet[] animatorSetsIcon;

    // data
    private boolean iconRise;
    public boolean isCollected;

    public Location location;
    private int[] maxiTemp;
    private int[] miniTemp;
    private boolean[] showIcon;
    private boolean showStar;

    private MyDatabaseHelper databaseHelper;
    private int widthPixels;

    // baidu location
    public LocationClient mLocationClient;
    public BDLocationListener myListener;

// life cycle

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.weather_fragment, container, false);
        this.toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

        this.gestureDetectorPage = new GestureDetector(getActivity(), new PageOnGestureListener());
        this.gestureDetectorTrendView = new GestureDetector(getActivity(), new TrendViewOnGestureListener());
        this.gestureDetectorHourlyView = new GestureDetector(getActivity(), new HourlyViewOnGestureListener());

        this.initDatabaseHelper();
        this.setLocation();
        this.maxiTemp = new int[7];
        this.miniTemp = new int[7];

        this.setWindowTopColor();
        this.initWeatherView(mainView);
        this.initInformationContainer(mainView);

        if (location.info == null) {
            this.refreshAll();
        }
        return mainView;
    }

// initialize widget

    private void initWeatherView(View view) {
        this.iconRise = false;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // show star = ! hide star
        this.showStar = ! sharedPreferences.getBoolean(getString(R.string.key_hide_star), false);

        this.skyView = (SkyView) view.findViewById(R.id.sky_view);

        this.start = new ImageView[2];
        this.widthPixels = getResources().getDisplayMetrics().widthPixels;
        int[] starSize = BitmapHelper.getStarSize(widthPixels);
        start[0] = (ImageView) view.findViewById(R.id.start_1);
        start[0].setImageBitmap(BitmapHelper.readBitMap(getActivity(), R.drawable.start_1, starSize[0], starSize[1]));
        start[1] = (ImageView) view.findViewById(R.id.start_2);
        start[1].setImageBitmap(BitmapHelper.readBitMap(getActivity(), R.drawable.start_2, starSize[0], starSize[1]));

        this.weatherIcon = new ImageView[3];
        weatherIcon[0] = (ImageView) view.findViewById(R.id.weather_icon_1);
        weatherIcon[1] = (ImageView) view.findViewById(R.id.weather_icon_2);
        weatherIcon[2] = (ImageView) view.findViewById(R.id.weather_icon_3);

        this.animatorSetsAnimal = new AnimatorSet[2];
        animatorSetsAnimal[0] = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.start_shine_1);
        animatorSetsAnimal[1] = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.start_shine_2);

        this.setModel();

        this.showCircles();
        this.showAnimals();

        this.weatherTextLive = (TextView) view.findViewById(R.id.weather_text_live);
        weatherTextLive.setVisibility(View.GONE);
        this.aqiTextLive = (TextView) view.findViewById(R.id.aqi_text_live);
        aqiTextLive.setVisibility(View.GONE);

        RelativeLayout touchLayout = (RelativeLayout) view.findViewById(R.id.touch_layout);
        touchLayout.setOnClickListener(this);
    }

    private void initInformationContainer(View view) {
        weatherCard = (MyCardView) view.findViewById(R.id.base_info_card);
        weatherCard.setOnTouchListener(this);
        LinearLayout.LayoutParams weatherCardLayoutParams = (LinearLayout.LayoutParams) weatherCard.getLayoutParams();
        weatherCard.viewStartX = weatherCard.getX() + weatherCardLayoutParams.leftMargin;
        weatherCard.viewStartY = weatherCard.getY();
        weatherCard.setVisibility(View.GONE);

        lifeCard = (MyCardView) view.findViewById(R.id.life_info_card);
        lifeCard.setOnTouchListener(this);
        LinearLayout.LayoutParams liferCardLayoutParams = (LinearLayout.LayoutParams) lifeCard.getLayoutParams();
        lifeCard.viewStartX = lifeCard.getX() + liferCardLayoutParams.leftMargin;
        lifeCard.viewStartY = lifeCard.getY();
        lifeCard.setVisibility(View.GONE);

        this.timeTextLive = (TextView) view.findViewById(R.id.time_text_live);
        this.locationTextLive = (TextView) view.findViewById(R.id.location_text_live);
        this.locationTextLive.setOnClickListener(this);

        locationCollect = (ImageView) view.findViewById(R.id.location_collect_icon);
        setCollectImage();
        locationCollect.setOnClickListener(this);

        this.weekWeatherTitle = (TextView) view.findViewById(R.id.week_weather_title);
        this.lifeInfoTitle = (TextView) view.findViewById(R.id.life_info_title);
        if (MainActivity.isDay) {
            weekWeatherTitle.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_3));
            lifeInfoTitle.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_3));
            locationTextLive.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_3));
        } else {
            weekWeatherTitle.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_1));
            lifeInfoTitle.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_1));
            locationTextLive.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_1));
        }

        this.weekText = new TextView[7];
        weekText[0] = (TextView) view.findViewById(R.id.week_text_1);
        weekText[1] = (TextView) view.findViewById(R.id.week_text_2);
        weekText[2] = (TextView) view.findViewById(R.id.week_text_3);
        weekText[3] = (TextView) view.findViewById(R.id.week_text_4);
        weekText[4] = (TextView) view.findViewById(R.id.week_text_5);
        weekText[5] = (TextView) view.findViewById(R.id.week_text_6);
        weekText[6] = (TextView) view.findViewById(R.id.week_text_7);

        this.weekIcon = new ImageView[7];
        weekIcon[0] = (ImageView) view.findViewById(R.id.icon_image_1);
        weekIcon[1] = (ImageView) view.findViewById(R.id.icon_image_2);
        weekIcon[2] = (ImageView) view.findViewById(R.id.icon_image_3);
        weekIcon[3] = (ImageView) view.findViewById(R.id.icon_image_4);
        weekIcon[4] = (ImageView) view.findViewById(R.id.icon_image_5);
        weekIcon[5] = (ImageView) view.findViewById(R.id.icon_image_6);
        weekIcon[6] = (ImageView) view.findViewById(R.id.icon_image_7);

        this.weekInfo = new LinearLayout[7];
        weekInfo[0] = (LinearLayout) view.findViewById(R.id.week_info_1);
        weekInfo[1] = (LinearLayout) view.findViewById(R.id.week_info_2);
        weekInfo[2] = (LinearLayout) view.findViewById(R.id.week_info_3);
        weekInfo[3] = (LinearLayout) view.findViewById(R.id.week_info_4);
        weekInfo[4] = (LinearLayout) view.findViewById(R.id.week_info_5);
        weekInfo[5] = (LinearLayout) view.findViewById(R.id.week_info_6);
        weekInfo[6] = (LinearLayout) view.findViewById(R.id.week_info_7);

        this.poweredText = (TextView) view.findViewById(R.id.weather_trend_view_powered_text);
        this.trendContainer = (FrameLayout) view.findViewById(R.id.trend_view_container);
        this.weatherDailyView = (DailyView) view.findViewById(R.id.weather_trend_view);
        this.weatherDailyView.setOnTouchListener(this);

        this.hourlyContainer = (FrameLayout) view.findViewById(R.id.hourly_view_container);
        this.weatherHourlyView = (HourlyView) view.findViewById(R.id.weather_hourly_view);
        this.weatherHourlyView.setOnTouchListener(this);
        hourlyContainer.setVisibility(View.GONE);

        windKind = (TextView) view.findViewById(R.id.detail_wind_kind);
        windInfo = (TextView) view.findViewById(R.id.detail_wind_info);
        pmKind = (TextView) view.findViewById(R.id.detail_pm_kind);
        pmInfo = (TextView) view.findViewById(R.id.detail_pm_info);
        waterKind = (TextView) view.findViewById(R.id.detail_water_kind);
        waterInfo = (TextView) view.findViewById(R.id.detail_water_info);
        sunKind = (TextView) view.findViewById(R.id.detail_sun_kind);
        sunInfo = (TextView) view.findViewById(R.id.detail_sun_info);
        wearKind = (TextView) view.findViewById(R.id.detail_wear_kind);
        wearInfo = (TextView) view.findViewById(R.id.detail_wear_info);
        illKind = (TextView) view.findViewById(R.id.detail_ill_kind);
        illInfo = (TextView) view.findViewById(R.id.detail_ill_info);
        airKind = (TextView) view.findViewById(R.id.detail_air_kind);
        airInfo = (TextView) view.findViewById(R.id.detail_air_info);
        washCarKind = (TextView) view.findViewById(R.id.detail_wash_car_kind);
        washCarInfo = (TextView) view.findViewById(R.id.detail_wash_car_info);
        sportKind = (TextView) view.findViewById(R.id.detail_sport_kind);
        sportInfo = (TextView) view.findViewById(R.id.detail_sport_info);

        this.swipeRefreshLayout = (MySwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        this.swipeRefreshLayout.setColorSchemeColors(R.color.lightPrimary_3, R.color.darkPrimary_1);
        this.swipeRefreshLayout.setOnRefreshListener(this);

        MyScrollView scrollView = (MyScrollView) view.findViewById(R.id.scroll_view);
        scrollView.setOnScrollViewListener(this);

        if (location.info != null) {
            this.refreshUI();
        }
    }

// refresh data

    public void refreshAll() {
        this.swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });
        this.refreshData();
    }

    public void setLocation() {
        this.location = MainActivity.lastLocation;
    }

    private void refreshData() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        this.showStar = !sharedPreferences.getBoolean(getString(R.string.key_hide_star), false);

        if (location.location.equals(getString(R.string.local))) {
            // get location
            this.initBaiduLocation();
        } else {
            MainActivity.getTotalData(getActivity(), location, location.location);
        }
    }

// refresh UI

    public void refreshTotalDataSucceed() {
        this.setLocation();

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (5 < hour && hour < 19) {
            editor.putBoolean(getString(R.string.key_isDay), true);
        } else {
            editor.putBoolean(getString(R.string.key_isDay), false);
        } editor.apply();

        for (int i = 0; i < MainActivity.locationList.size(); i ++) {
            if (MainActivity.locationList.get(i).location.equals(location.location)) {
                MainActivity.locationList.remove(i);
                MainActivity.locationList.add(i, location);
                MainActivity.lastLocation = location;
                break;
            }
        }

        swipeRefreshLayout.setRefreshing(false);
        refreshUI();

        if (location.info != null) {
            MyDatabaseHelper.writeWeatherInfo(getActivity(), location.location, location.info);
            MyDatabaseHelper.writeTodayWeather(getActivity(), location.info);
            MainActivity.sendNotification(getActivity(), location);
            MainActivity.refreshWidget(getActivity(), location, location.info, MainActivity.isDay);
        }
    }

    public void refreshHourlyDataSucceed() {
        this.setLocation();
        if (location.hourlyData == null) {
            weatherHourlyView.setData(null, null);
            weatherHourlyView.invalidate();
        } else {
            int[] temp = location.hourlyData.temp;
            float[] pop = location.hourlyData.pop;
            for (int i = 0; i <temp.length; i ++) {
                if (temp[i] > maxiTemp[0]) {
                    temp[i] = maxiTemp[0];
                } else if (temp[i] < miniTemp[0]) {
                    temp[i] = miniTemp[0];
                }
            }
            weatherHourlyView.setData(temp, pop);
            weatherHourlyView.invalidate();
        }
    }

    public void refreshTotalDataFailed() {
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(
                getActivity(),
                getString(R.string.refresh_data_failed),
                Toast.LENGTH_SHORT).show();

        location.info = MyDatabaseHelper.readWeatherInfo(getActivity(), location.location);
        if (location.info != null) {
            SharedPreferences.Editor editor1 = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
            int hour1 = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (5 < hour1 && hour1 < 19) {
                editor1.putBoolean(getString(R.string.key_isDay), true);
            } else {
                editor1.putBoolean(getString(R.string.key_isDay), false);
            } editor1.apply();

            for (int i = 0; i < MainActivity.locationList.size(); i ++) {
                if (MainActivity.locationList.get(i).location.equals(location.location)) {
                    MainActivity.locationList.remove(i);
                    MainActivity.locationList.add(i, location);
                    MainActivity.lastLocation = location;
                    break;
                }
            }

            refreshUI();
        }
    }

    public void refreshHourlyDataFailed() {
        weatherHourlyView.setData(null, null);
        weatherHourlyView.invalidate();
        Toast.makeText(
                getActivity(),
                getString(R.string.try_set_eng_location),
                Toast.LENGTH_LONG).show();
    }

    @SuppressLint("SetTextI18n")
    public void refreshUI() {
        if (location.info == null) {
            return;
        }

        if (MainActivity.needChangeTime()) {
            MainActivity.isDay = !MainActivity.isDay;
            FrameLayout statusBar = (FrameLayout) getActivity().findViewById(R.id.background_plate);
            MainActivity.setStatusBarColor(getActivity(), statusBar, true);
            this.setNavHead();
            MainActivity.initNavigationBar(getActivity(), getActivity().getWindow());
            this.skyView.showCircle(false);
            this.setWindowTopColor();
        }

        this.weatherTextLive.setText(location.info.weatherNow + " " + location.info.tempNow + "℃");
        weatherTextLive.setVisibility(View.VISIBLE);
        this.aqiTextLive.setText(location.info.aqiLevel);
        aqiTextLive.setVisibility(View.VISIBLE);
        this.timeTextLive.setText(location.info.refreshTime);
        this.locationTextLive.setText(location.info.location);

        if (MainActivity.isDay) {
            this.weekWeatherTitle.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_3));
            this.lifeInfoTitle.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_3));
            locationTextLive.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_3));
        } else {
            this.weekWeatherTitle.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_1));
            this.lifeInfoTitle.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_1));
            locationTextLive.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_1));
        }

        isCollected = false;
        for (int i = 0; i < MainActivity.locationList.size(); i ++) {
            if (MainActivity.locationList.get(i).location.equals(location.location)) {
                locationCollect.setImageResource(R.drawable.ic_collect_yes);
                isCollected = true;
                break;
            }
        }
        if (! isCollected) {
            locationCollect.setImageResource(R.drawable.ic_collect_no);
        }

        for (int i = 0; i < 7; i ++) {
            if (i == 0) {
                this.weekText[i].setText(getString(R.string.today));
            } else {
                this.weekText[i].setText(location.info.week[i]);
            }

            int[] imageId= JuheWeather.getWeatherIcon(location.info.weatherKind[i], MainActivity.isDay);
            if (imageId[3] != 0) {
                int size = BitmapHelper.getWeekIconSize(widthPixels);
                weekIcon[i].setImageBitmap(BitmapHelper.readBitMap(getActivity(), imageId[3], size, size));
                weekIcon[i].setVisibility(View.VISIBLE);
            } else {
                weekIcon[i].setVisibility(View.GONE);
            }
            weekInfo[i].setOnClickListener(this);
        }

        for (int i = 0; i < 7; i ++) {
            maxiTemp[i] = Integer.parseInt(location.info.maxiTemp[i]);
            miniTemp[i] = Integer.parseInt(location.info.miniTemp[i]);
        }
        this.trendContainer.setAlpha(1);
        this.trendContainer.setVisibility(View.VISIBLE);
        this.hourlyContainer.setVisibility(View.GONE);
        int[] yesterdayTemp = MyDatabaseHelper.readYesterdayWeather(getActivity(), location.info);
        this.weatherDailyView.setData(maxiTemp, miniTemp, yesterdayTemp);
        this.weatherDailyView.invalidate();

        if (location.location.replaceAll(" ", "").matches("[a-zA-Z]+")) {
            poweredText.setText(getString(R.string.powered_by_hefeng));
        } else {
            poweredText.setText(getString(R.string.powered_by_juhe));
        }
        this.initLifeInfo(location.info);

        if (MainActivity.isDay) {
            this.windKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_5));
            this.pmKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_5));
            this.waterKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_5));
            this.sunKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_5));
            this.wearKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_5));
            this.illKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_5));
            this.airKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_5));
            this.washCarKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_5));
            this.sportKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightPrimary_5));
        } else {
            this.windKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_5));
            this.pmKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_5));
            this.waterKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_5));
            this.sunKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_5));
            this.wearKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_5));
            this.illKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_5));
            this.airKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_5));
            this.washCarKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_5));
            this.sportKind.setTextColor(ContextCompat.getColor(getActivity(), R.color.darkPrimary_5));
        }

        final AnimatorSet[] animatorSetShowView = new AnimatorSet[2];
        animatorSetShowView[0] = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.show_view);
        animatorSetShowView[1] = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.show_view);
        animatorSetShowView[0].setTarget(this.weatherCard);
        animatorSetShowView[1].setTarget(this.lifeCard);
        animatorSetShowView[0].addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                lifeCard.setVisibility(View.VISIBLE);
                animatorSetShowView[1].start();
            }
        });
        animatorSetShowView[1].addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (MainActivity.activityVisibility) {
                    showWeatherIcon();
                }
            }
        });

        weatherCard.setVisibility(View.VISIBLE);
        animatorSetShowView[0].start();

        if (MainActivity.isDay) {
            for (int i = 0; i < 2 ; i ++) {
                start[i].setVisibility(View.GONE);
            }
        } else {
            if (showStar) {
                for (int i = 0; i < 2 ; i ++) {
                    start[i].setVisibility(View.VISIBLE);
                }
            } else {
                for (int i = 0; i < 2 ; i ++) {
                    start[i].setVisibility(View.GONE);
                }
            }
        }
    }

    public void setNavHead() {
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
        View navHeader = navigationView.getHeaderView(0);
        FrameLayout navHead = (FrameLayout) navHeader.findViewById(R.id.nav_header);
        if (MainActivity.isDay) {
            navHead.setBackgroundResource(R.drawable.nav_head_day);
        } else {
            navHead.setBackgroundResource(R.drawable.nav_head_night);
        }
    }

    public void showCirclesView() {
        this.showCircles();
        this.showWeatherIcon();
    }

    private void setModel() {
        setNavHead();
        if (MainActivity.isDay) {
            for (int i = 0; i < 2; i ++) {
                start[i].setVisibility(View.GONE);
            }
        } else {
            if (showStar) {
                for (int i = 0; i < 2; i ++) {
                    start[i].setVisibility(View.VISIBLE);
                }
            } else {
                for (int i = 0; i < 2; i ++) {
                    start[i].setVisibility(View.GONE);
                }
            }
        }

        for (int i = 0; i < 2; i ++) {
            this.animatorSetsAnimal[i].setTarget(start[i]);
        }
    }

    public void showCircles() {
        skyView.showCircle(true);
    }

    public void touchCircles() {
        skyView.touchCircle();
        if (location.info != null) {
            for (int i = 0; i < 3; i ++) {
                if (showIcon[i]) {
                    animatorSetsIcon[i].start();
                }
            }
        }
    }

    public void showAnimals() {
        this.animatorSetsAnimal[0].addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animatorSetsAnimal[0].start();
            }
        });
        this.animatorSetsAnimal[1].addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animatorSetsAnimal[1].start();
            }
        });

        for (int i = 0; i < 2; i ++) {
            this.animatorSetsAnimal[i].start();
        }
    }

    public void setCollectImage() {
        isCollected = false;
        for (int i = 0; i < MainActivity.locationList.size(); i ++) {
            if (MainActivity.locationList.get(i).location.equals(location.location)) {
                locationCollect.setImageResource(R.drawable.ic_collect_yes);
                isCollected = true;
                break;
            }
        }
        if (! isCollected) {
            locationCollect.setImageResource(R.drawable.ic_collect_no);
        }
    }

    public void showWeatherIcon() {
        if (location.info == null) {
            return;
        }

        final AnimatorSet[] animatorSetsRise = new AnimatorSet[3];
        animatorSetsRise[0] = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.weather_icon_rise);
        animatorSetsRise[1] = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.weather_icon_rise);
        animatorSetsRise[2] = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.weather_icon_rise);

        if (this.iconRise) {
            // hide weather icon at first.
            AnimatorSet[] animatorSetsFall = new AnimatorSet[3];
            animatorSetsFall[0] = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.weather_icon_fall);
            animatorSetsFall[1] = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.weather_icon_fall);
            animatorSetsFall[2] = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.weather_icon_fall);

            animatorSetsFall[2].addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    setWeatherIcon();

                    for (int i = 0; i < 3; i ++) {
                        if (showIcon[i]) {
                            animatorSetsRise[i].setTarget(weatherIcon[i]);
                            animatorSetsRise[i].start();
                        }
                    }
                }
            });

            for (int i = 0; i < 3; i ++) {
                animatorSetsFall[i].setTarget(weatherIcon[i]);
                animatorSetsFall[i].start();
            }
        } else {
            // just show weather.
            this.iconRise = true;
            this.setWeatherIcon();

            for (int i = 0; i < 3; i ++) {
                if (showIcon[i]) {
                    animatorSetsRise[i].setTarget(weatherIcon[i]);
                    animatorSetsRise[i].start();
                }
            }
        }
    }

    private void setWindowTopColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription;
            Bitmap topIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            if (MainActivity.isDay) {
                taskDescription = new ActivityManager.TaskDescription(
                        getString(R.string.app_name),
                        topIcon,
                        ContextCompat.getColor(getActivity(), R.color.lightPrimary_5));
            } else {
                taskDescription = new ActivityManager.TaskDescription(
                        getString(R.string.app_name),
                        topIcon,
                        ContextCompat.getColor(getActivity(), R.color.darkPrimary_5));
            }
            getActivity().setTaskDescription(taskDescription);
            topIcon.recycle();
        }
    }

    private void setWeatherIcon() {
        this.showIcon = new boolean[3];

        if (location.info == null) {
            for (int i = 0; i < showIcon.length; i ++) {
                showIcon[i] = false;
            }
            return;
        }

        int[] imageId = JuheWeather.getWeatherIcon(location.info.weatherKindNow, MainActivity.isDay);
        int[] animatorId = JuheWeather.getAnimatorId(location.info.weatherKindNow, MainActivity.isDay);
        this.animatorSetsIcon = new AnimatorSet[3];

        for (int i = 0; i < 3; i ++) {
            if (imageId[i] != 0) {
                showIcon[i] = true;
                weatherIcon[i].setImageResource(imageId[i]);
                weatherIcon[i].setVisibility(View.VISIBLE);
                this.animatorSetsIcon[i] = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), animatorId[i]);
                animatorSetsIcon[i].setTarget(weatherIcon[i]);
                animatorSetsIcon[i].start();
            } else {
                showIcon[i] = false;
                weatherIcon[i].setVisibility(View.GONE);
            }
        }
    }

    private void initLifeInfo(WeatherInfoToShow info) {
        windKind.setText(info.windTitle);
        windInfo.setText(info.windInfo);

        pmKind.setText(info.pmTitle);
        pmInfo.setText(info.pmInfo);

        waterKind.setText(info.humTitle);
        waterInfo.setText(info.humInfo);

        sunKind.setText(info.uvTitle);
        sunInfo.setText(info.uvInfo);

        wearKind.setText(info.dressTitle);
        wearInfo.setText(info.dressInfo);

        illKind.setText(info.coldTitle);
        illInfo.setText(info.coldInfo);

        airKind.setText(info.airTitle);
        airInfo.setText(info.airInfo);

        washCarKind.setText(info.washCarTitle);
        washCarInfo.setText(info.washCarInfo);

        sportKind.setText(info.exerciseTitle);
        sportInfo.setText(info.exerciseInfo);
    }

// database

    private void initDatabaseHelper() {
        this.databaseHelper = new MyDatabaseHelper(getActivity(),
                MyDatabaseHelper.DATABASE_NAME,
                null,
                MyDatabaseHelper.VERSION_CODE);
    }

    private void writeLocation() {
        SQLiteDatabase database = this.databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MyDatabaseHelper.COLUMN_LOCATION, location.location);
        database.insert(MyDatabaseHelper.TABLE_LOCATION, null, values);
        values.clear();
        database.close();
    }

    private void deleteLocation() {
        SQLiteDatabase database = this.databaseHelper.getWritableDatabase();
        String[] location = new String[] {this.location.location};
        database.delete(
                MyDatabaseHelper.TABLE_LOCATION,
                MyDatabaseHelper.COLUMN_LOCATION + " = ?",
                location);
        database.close();
    }

// baidu location

    private void initBaiduLocation(){
        myListener = this;
        mLocationClient = new LocationClient(getActivity());     //声明LocationClient类
        mLocationClient.registerLocationListener( myListener );
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span=0;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(false);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.setLocOption(option);
        mLocationClient.start();
    }

    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        //Receive Location
        String location = null;

        StringBuilder sb = new StringBuilder(256);
        sb.append("time : ");
        sb.append(bdLocation.getTime());
        sb.append("\nerror code : ");
        sb.append(bdLocation.getLocType());
        sb.append("\nlatitude : ");
        sb.append(bdLocation.getLatitude());
        sb.append("\nlontitude : ");
        sb.append(bdLocation.getLongitude());
        sb.append("\nradius : ");
        sb.append(bdLocation.getRadius());
        if (bdLocation.getLocType() == BDLocation.TypeGpsLocation){// GPS定位结果
            location = bdLocation.getCity();
        } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation){// 网络定位结果
            location = bdLocation.getCity();
        } else if (bdLocation.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
            sb.append("\ndescribe : ");
            sb.append("离线定位成功，离线定位结果也是有效的");
            location = bdLocation.getCity();
        } else if (bdLocation.getLocType() == BDLocation.TypeServerError) {
            sb.append("\ndescribe : ");
            sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
        } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkException) {
            sb.append("\ndescribe : ");
            sb.append("网络不同导致定位失败，请检查网络是否通畅");
        } else if (bdLocation.getLocType() == BDLocation.TypeCriteriaException) {
            sb.append("\ndescribe : ");
            sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
        }

        if(location == null) {
            Toast.makeText(getActivity(),
                    getString(R.string.get_location_failed),
                    Toast.LENGTH_SHORT).show();
            refreshTotalDataFailed();
        } else {
            MainActivity.getTotalData(getActivity(), this.location, location);
        }

        sb.append("\nlocationdescribe : ");
        sb.append(bdLocation.getLocationDescribe());// 位置语义化信息
        List<Poi> list = bdLocation.getPoiList();// POI数据
        if (list != null) {
            sb.append("\npoilist size = : ");
            sb.append(list.size());
            for (Poi p : list) {
                sb.append("\npoi= : ");
                sb.append(p.getId()).
                        append(" ").
                        append(p.getName()).
                        append(" ").
                        append(p.getRank());
            }
        }
        Log.i("BaiduLocationApiDem", sb.toString());
        mLocationClient.stop();
    }

// touch

    private class PageOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(e2.getX() - e1.getX()) < Math.abs(e2.getY() - e1.getY())) {
                return false;
            } else if (Math.abs(e2.getX() - e1.getX()) < 10)
            weatherCard.getParent().requestDisallowInterceptTouchEvent(true);
            lifeCard.getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(e2.getX() - e1.getX()) < Math.abs(e2.getY() - e1.getY())) {
                return false;
            }
            weatherCard.getParent().requestDisallowInterceptTouchEvent(true);
            lifeCard.getParent().requestDisallowInterceptTouchEvent(true);
            weatherCard.setX(weatherCard.viewStartX + (e2.getX() - e1.getX()));
            lifeCard.setX(lifeCard.viewStartX + (e2.getX() - e1.getX()));
            weatherCard.setVisibility(View.GONE);
            weatherCard.setX(weatherCard.viewStartX);
            weatherCard.setY(weatherCard.viewStartY);
            lifeCard.setVisibility(View.GONE);
            lifeCard.setX(weatherCard.viewStartX);
            lifeCard.setY(weatherCard.viewStartY);

            for (int i = 0; i < MainActivity.locationList.size(); i ++) {
                if (location.location.equals(MainActivity.locationList.get(i).location)) {
                    if (e2.getX() < e1.getX()) {
                        i ++;
                        if (i >= MainActivity.locationList.size()) {
                            i = 0;
                        }
                    } else {
                        i --;
                        if (i < 0) {
                            i = MainActivity.locationList.size() - 1;
                        }
                    }
                    location = MainActivity.locationList.get(i);
                    break;
                }
            }
            int[] temp = new int[] {100, 100, 100, 100, 100, 100, 100, 100};
            float[] pop = new float[] {0, 0, 0, 0, 0, 0, 0, 0};
            weatherHourlyView.setData(temp, pop);
            weatherTextLive.setVisibility(View.GONE);
            aqiTextLive.setVisibility(View.GONE);
            refreshAll();
            return true;
        }
    }

    private class TrendViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(e2.getX() - e1.getX()) < Math.abs(e2.getY() - e1.getY())) {
                return false;
            }
            weatherCard.getParent().requestDisallowInterceptTouchEvent(true);
            lifeCard.getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(e2.getX() - e1.getX()) < Math.abs(e2.getY() - e1.getY())) {
                return false;
            }
            weatherCard.getParent().requestDisallowInterceptTouchEvent(true);
            lifeCard.getParent().requestDisallowInterceptTouchEvent(true);
            weatherCard.setX(weatherCard.viewStartX + (e2.getX() - e1.getX()));
            lifeCard.setX(lifeCard.viewStartX + (e2.getX() - e1.getX()));
            weatherCard.setVisibility(View.GONE);
            weatherCard.setX(weatherCard.viewStartX);
            weatherCard.setY(weatherCard.viewStartY);
            lifeCard.setVisibility(View.GONE);
            lifeCard.setX(weatherCard.viewStartX);
            lifeCard.setY(weatherCard.viewStartY);

            for (int i = 0; i < MainActivity.locationList.size(); i ++) {
                if (location.location.equals(MainActivity.locationList.get(i).location)) {
                    if (e2.getX() < e1.getX()) {
                        i ++;
                        if (i >= MainActivity.locationList.size()) {
                            i = 0;
                        }
                    } else {
                        i --;
                        if (i < 0) {
                            i = MainActivity.locationList.size() - 1;
                        }
                    }
                    location = MainActivity.locationList.get(i);
                    break;
                }
            }
            int[] temp = new int[] {100, 100, 100, 100, 100, 100, 100, 100};
            float[] pop = new float[] {0, 0, 0, 0, 0, 0, 0, 0};
            weatherHourlyView.setData(temp, pop);
            weatherTextLive.setVisibility(View.GONE);
            aqiTextLive.setVisibility(View.GONE);
            refreshAll();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            if (location.hourlyData == null) {
                MainActivity.getHourlyData(getActivity());
            }
            final AnimatorSet animatorSetShow = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.opaque);
            animatorSetShow.setTarget(hourlyContainer);
            final AnimatorSet animatorSetHide = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.trans);
            animatorSetHide.setTarget(trendContainer);
            animatorSetHide.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    trendContainer.setVisibility(View.GONE);
                    hourlyContainer.setVisibility(View.VISIBLE);
                    weatherHourlyView.invalidate();
                    animatorSetShow.start();
                }
            });
            animatorSetHide.start();
            return true;
        }
    }

    private class HourlyViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(e2.getX() - e1.getX()) < Math.abs(e2.getY() - e1.getY())) {
                return false;
            }
            weatherCard.getParent().requestDisallowInterceptTouchEvent(true);
            lifeCard.getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(e2.getX() - e1.getX()) < Math.abs(e2.getY() - e1.getY())) {
                return false;
            }
            weatherCard.getParent().requestDisallowInterceptTouchEvent(true);
            lifeCard.getParent().requestDisallowInterceptTouchEvent(true);
            weatherCard.setX(weatherCard.viewStartX + (e2.getX() - e1.getX()));
            lifeCard.setX(lifeCard.viewStartX + (e2.getX() - e1.getX()));
            weatherCard.setVisibility(View.GONE);
            weatherCard.setX(weatherCard.viewStartX);
            weatherCard.setY(weatherCard.viewStartY);
            lifeCard.setVisibility(View.GONE);
            lifeCard.setX(weatherCard.viewStartX);
            lifeCard.setY(weatherCard.viewStartY);

            for (int i = 0; i < MainActivity.locationList.size(); i ++) {
                if (location.location.equals(MainActivity.locationList.get(i).location)) {
                    if (e2.getX() < e1.getX()) {
                        i ++;
                        if (i >= MainActivity.locationList.size()) {
                            i = 0;
                        }
                    } else {
                        i --;
                        if (i < 0) {
                            i = MainActivity.locationList.size() - 1;
                        }
                    }
                    location = MainActivity.locationList.get(i);
                    break;
                }
            }
            int[] temp = new int[] {100, 100, 100, 100, 100, 100, 100, 100};
            float[] pop = new float[] {0, 0, 0, 0, 0, 0, 0, 0};
            weatherHourlyView.setData(temp, pop);
            weatherTextLive.setVisibility(View.GONE);
            aqiTextLive.setVisibility(View.GONE);
            refreshAll();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            final AnimatorSet animatorSetShow = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.opaque);
            animatorSetShow.setTarget(trendContainer);
            AnimatorSet animatorSetHide = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.trans);
            animatorSetHide.setTarget(hourlyContainer);
            animatorSetHide.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    hourlyContainer.setVisibility(View.GONE);
                    trendContainer.setVisibility(View.VISIBLE);
                    animatorSetShow.start();
                }
            });
            animatorSetHide.start();
            return true;
        }
    }

    @Override
    public void onSetLocation(String location, boolean isSearch) {
        if (location.equals(getString(R.string.search_null))) {
            Toast.makeText(getActivity(),
                    getString(R.string.search_null),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (isSearch) {
            MainActivity.lastLocation = new Location(location);
            setLocation();
            refreshAll();
        } else {
            for (int i = 0; i < MainActivity.locationList.size(); i ++) {
                if (MainActivity.locationList.get(i).location.equals(location)) {
                    MainActivity.lastLocation = MainActivity.locationList.get(i);
                    setLocation();
                    refreshAll();
                    return;
                }
            }
        }
    }

    @Override
    public void onRefresh() {
        this.refreshData();
    }

    @Override
    public void onScrollChanged(MyScrollView scrollView, int x, int y, int oldX, int oldY) {
        switch (scrollView.getId()) {
            case R.id.scroll_view:
                float dpi = getResources().getDisplayMetrics().density;
                if (y > oldY) {
                    float oldAlpha = toolbar.getAlpha();
                    float newAlpha = (float) (oldAlpha - Math.abs(y - oldY) / (400 / 2.625 * dpi));
                    if (newAlpha < 0) {
                        newAlpha = 0;
                    }
                    toolbar.setAlpha(newAlpha);
                } else {
                    float oldAlpha = toolbar.getAlpha();
                    float newAlpha = (float) (oldAlpha + Math.abs(y - oldY) / (400 / 2.625 * dpi));
                    if (newAlpha > 1) {
                        newAlpha = 1;
                    }
                    toolbar.setAlpha(newAlpha);
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int widgetId = v.getId();

        switch (widgetId) {
            case R.id.touch_layout:
                touchCircles();
                break;
            case R.id.location_text_live:
                ManageDialog dialog = new ManageDialog();
                dialog.addLocationListener(WeatherFragment.this);
                dialog.show(getFragmentManager(), "ManageDialog");
                break;
            case R.id.location_collect_icon:
                if (isCollected && MainActivity.locationList.size() > 1) {
                    for (int i = 0; i < MainActivity.locationList.size(); i ++) {
                        if (location.location.equals(MainActivity.locationList.get(i).location)) {
                            MainActivity.locationList.remove(i);
                            break;
                        }
                    }
                    deleteLocation();
                    locationCollect.setImageResource(R.drawable.ic_collect_no);
                    isCollected = false;
                    Toast.makeText(getActivity(),
                            getString(R.string.delete_succeed),
                            Toast.LENGTH_SHORT).show();
                } else if (isCollected) {
                    Toast.makeText(getActivity(),
                            getString(R.string.location_list_cannot_be_null),
                            Toast.LENGTH_SHORT).show();
                } else {
                    MainActivity.locationList.add(location);
                    writeLocation();
                    locationCollect.setImageResource(R.drawable.ic_collect_yes);
                    isCollected = true;
                    Toast.makeText(getActivity(),
                            getString(R.string.collect_succeed),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.week_info_1:
                WeatherDialog weatherDialog1 = new WeatherDialog();
                weatherDialog1.setData(location.info, 0);
                weatherDialog1.show(getFragmentManager(), "WeatherDialog");
                break;
            case R.id.week_info_2:
                WeatherDialog weatherDialog2 = new WeatherDialog();
                weatherDialog2.setData(location.info, 1);
                weatherDialog2.show(getFragmentManager(), "WeatherDialog");
                break;
            case R.id.week_info_3:
                WeatherDialog weatherDialog3 = new WeatherDialog();
                weatherDialog3.setData(location.info, 2);
                weatherDialog3.show(getFragmentManager(), "WeatherDialog");
                break;
            case R.id.week_info_4:
                WeatherDialog week_info_4 = new WeatherDialog();
                week_info_4.setData(location.info, 3);
                week_info_4.show(getFragmentManager(), "WeatherDialog");
                break;
            case R.id.week_info_5:
                WeatherDialog weatherDialog5 = new WeatherDialog();
                weatherDialog5.setData(location.info, 4);
                weatherDialog5.show(getFragmentManager(), "WeatherDialog");
                break;
            case R.id.week_info_6:
                WeatherDialog weatherDialog6 = new WeatherDialog();
                weatherDialog6.setData(location.info, 5);
                weatherDialog6.show(getFragmentManager(), "WeatherDialog");
                break;
            case R.id.week_info_7:
                WeatherDialog weatherDialog7 = new WeatherDialog();
                weatherDialog7.setData(location.info, 6);
                weatherDialog7.show(getFragmentManager(), "WeatherDialog");
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.base_info_card:
                return gestureDetectorPage.onTouchEvent(event);
            case R.id.life_info_card:
                return gestureDetectorPage.onTouchEvent(event);
            case R.id.weather_trend_view:
                return gestureDetectorTrendView.onTouchEvent(event);
            case R.id.weather_hourly_view:
                return gestureDetectorHourlyView.onTouchEvent(event);
        }
        return false;
    }
}