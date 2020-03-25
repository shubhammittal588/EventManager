package nerd.tuxmobil.fahrplan.congress.details;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import nerd.tuxmobil.fahrplan.congress.BuildConfig;
import nerd.tuxmobil.fahrplan.congress.MyApp;
import nerd.tuxmobil.fahrplan.congress.R;
import nerd.tuxmobil.fahrplan.congress.base.BaseActivity;
import nerd.tuxmobil.fahrplan.congress.contract.BundleKeys;
import nerd.tuxmobil.fahrplan.congress.models.Lecture;
import nerd.tuxmobil.fahrplan.congress.navigation.RoomForC3NavConverter;

public class EventDetail extends BaseActivity {

    public static void startForResult(@NonNull Activity activity,
                                      @NonNull Lecture lecture) {
        Intent intent = new Intent(activity, EventDetail.class);
        intent.putExtra(BundleKeys.EVENT_ID, lecture.lectureId);
        intent.putExtra(BundleKeys.EVENT_ROOM, lecture.room);
        activity.startActivityForResult(intent, MyApp.EVENTVIEW);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int actionBarColor = ContextCompat.getColor(this, R.color.colorActionBar);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(actionBarColor));

        setContentView(R.layout.detail_frame);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }

        if (intent != null && findViewById(R.id.detail) != null) {
            EventDetailFragment eventDetailFragment = new EventDetailFragment();
            Bundle args = new Bundle();
            args.putString(BundleKeys.EVENT_ID,
                    intent.getStringExtra(BundleKeys.EVENT_ID));
            args.putString(BundleKeys.EVENT_ROOM,
                    intent.getStringExtra(BundleKeys.EVENT_ROOM));
            eventDetailFragment.setArguments(args);
            replaceFragment(R.id.detail, eventDetailFragment,
                    EventDetailFragment.FRAGMENT_TAG);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isVisible = !getRoomConvertedForC3Nav().isEmpty();
        menu.findItem(R.id.menu_item_navigate).setVisible(isVisible);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_navigate:
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(BuildConfig.C3NAV_URL + getRoomConvertedForC3Nav()));
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    private String getRoomConvertedForC3Nav() {
        final String currentRoom = getIntent().getStringExtra(BundleKeys.EVENT_ROOM);
        return RoomForC3NavConverter.convert(currentRoom);
    }

}
