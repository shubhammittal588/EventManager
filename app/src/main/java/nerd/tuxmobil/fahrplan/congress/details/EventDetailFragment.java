package nerd.tuxmobil.fahrplan.congress.details;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import info.metadude.android.eventfahrplan.commons.logging.Logging;
import info.metadude.android.eventfahrplan.commons.temporal.DateFormatter;
import kotlin.Unit;
import nerd.tuxmobil.fahrplan.congress.BuildConfig;
import nerd.tuxmobil.fahrplan.congress.MyApp;
import nerd.tuxmobil.fahrplan.congress.R;
import nerd.tuxmobil.fahrplan.congress.alarms.AlarmTimePickerFragment;
import nerd.tuxmobil.fahrplan.congress.calendar.CalendarSharing;
import nerd.tuxmobil.fahrplan.congress.commons.LiveDataExtensions;
import nerd.tuxmobil.fahrplan.congress.commons.ObservableType;
import nerd.tuxmobil.fahrplan.congress.contract.BundleKeys;
import nerd.tuxmobil.fahrplan.congress.models.Lecture;
import nerd.tuxmobil.fahrplan.congress.navigation.RoomForC3NavConverter;
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepository;
import nerd.tuxmobil.fahrplan.congress.schedule.FahrplanFragment;
import nerd.tuxmobil.fahrplan.congress.sharing.JsonLectureFormat;
import nerd.tuxmobil.fahrplan.congress.sharing.LectureSharer;
import nerd.tuxmobil.fahrplan.congress.sharing.SimpleLectureFormat;
import nerd.tuxmobil.fahrplan.congress.sidepane.OnSidePaneCloseListener;
import nerd.tuxmobil.fahrplan.congress.utils.EventUrlComposer;
import nerd.tuxmobil.fahrplan.congress.utils.FahrplanMisc;
import nerd.tuxmobil.fahrplan.congress.utils.FeedbackUrlComposer;
import nerd.tuxmobil.fahrplan.congress.utils.StringUtils;
import nerd.tuxmobil.fahrplan.congress.wiki.WikiEventUtils;


public class EventDetailFragment extends Fragment {

    private static final String LOG_TAG = "Detail";

    public static final String FRAGMENT_TAG = "detail";

    public static final int EVENT_DETAIL_FRAGMENT_REQUEST_CODE = 546;

    private static final String SCHEDULE_FEEDBACK_URL = BuildConfig.SCHEDULE_FEEDBACK_URL;

    private static final boolean SHOW_FEEDBACK_MENU_ITEM = !TextUtils.isEmpty(SCHEDULE_FEEDBACK_URL);

    private AppRepository appRepository;

    private String eventId;

    private Typeface boldCondensed;

    private Typeface black;

    private Typeface light;

    private Typeface regular;

    private Typeface bold;

    private boolean sidePane = false;

    private boolean hasArguments = false;

    /**
     * A single lecture which can be observed. Once it changes then its observers are notified
     * so they can immediately update the user interface.
     */
    private final ObservableType<Lecture> observableLecture = new ObservableType<>(Logging.Companion.get());

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appRepository = AppRepository.INSTANCE;
        observableLecture.addObserver(() -> {
            updateViews();
            return Unit.INSTANCE;
        });
        LiveDataExtensions.observeNonNullOrThrow(new LectureLiveData(eventId), this, lecture -> {
            observableLecture.setValue(lecture);
            //noinspection ConstantConditions
            showContent(getView());
            return Unit.INSTANCE;
        });
    }

    @Override
    public void onDetach() {
        observableLecture.deleteObservers();
        super.onDetach();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        MyApp.LogDebug(LOG_TAG, "onCreate");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout;
        if (sidePane) {
            layout = inflater.inflate(R.layout.detail_narrow, container, false);
        } else {
            layout = inflater.inflate(R.layout.detail, container, false);
        }
        showProgressBar(layout);
        return layout;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        eventId = args.getString(BundleKeys.EVENT_ID);
        sidePane = args.getBoolean(BundleKeys.SIDEPANE, false);
        hasArguments = true;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Activity activity = requireActivity();
        if (hasArguments) {
            AssetManager assetManager = activity.getAssets();
            boldCondensed = Typeface.createFromAsset(assetManager, "Roboto-BoldCondensed.ttf");
            black = Typeface.createFromAsset(assetManager, "Roboto-Black.ttf");
            light = Typeface.createFromAsset(assetManager, "Roboto-Light.ttf");
            regular = Typeface.createFromAsset(assetManager, "Roboto-Regular.ttf");
            bold = Typeface.createFromAsset(assetManager, "Roboto-Bold.ttf");
        }
        activity.setResult(Activity.RESULT_CANCELED);
    }

    private void showProgressBar(@NonNull View layout) {
        layout.findViewById(R.id.event_detail_progress_bar).setVisibility(View.VISIBLE);
        layout.findViewById(R.id.event_detail_bar_layout).setVisibility(View.GONE);
        layout.findViewById(R.id.event_detail_content_layout).setVisibility(View.GONE);
    }

    private void showContent(@NonNull View layout) {
        layout.findViewById(R.id.event_detail_progress_bar).setVisibility(View.GONE);
        layout.findViewById(R.id.event_detail_bar_layout).setVisibility(View.VISIBLE);
        layout.findViewById(R.id.event_detail_content_layout).setVisibility(View.VISIBLE);
    }

    private void updateViews() {
        Activity activity = requireActivity();
        View view = getView();

        Lecture lecture = observableLecture.getValue();

        // Detailbar

        TextView t;
        t = view.findViewById(R.id.lecture_detailbar_date_time);
        if (lecture.dateUTC > 0) {
            t.setText(DateFormatter.newInstance().getFormattedDateTimeShort(lecture.dateUTC));
        } else {
            t.setText("");
        }

        t = view.findViewById(R.id.lecture_detailbar_location);
        if (TextUtils.isEmpty(lecture.room)) {
            t.setText("");
        } else {
            t.setText(lecture.room);
        }

        t = view.findViewById(R.id.lecture_detailbar_lecture_id);
        if (TextUtils.isEmpty(eventId)) {
            t.setText("");
        } else {
            t.setText("ID: " + eventId);
        }

        // Title

        t = view.findViewById(R.id.event_detail_content_title_view);
        setUpTextView(t, boldCondensed, lecture.title);

        // Subtitle

        t = view.findViewById(R.id.event_detail_content_subtitle_view);
        if (TextUtils.isEmpty(lecture.subtitle)) {
            t.setVisibility(View.GONE);
        } else {
            setUpTextView(t, light, lecture.subtitle);
        }

        // Speakers

        t = view.findViewById(R.id.event_detail_content_speakers_view);
        if (TextUtils.isEmpty(lecture.speakers)) {
            t.setVisibility(View.GONE);
        } else {
            setUpTextView(t, black, lecture.speakers);
        }

        // Abstract

        t = view.findViewById(R.id.event_detail_content_abstract_view);
        if (TextUtils.isEmpty(lecture.abstractt)) {
            t.setVisibility(View.GONE);
        } else {
            String html = StringUtils.getHtmlLinkFromMarkdown(lecture.abstractt);
            setUpHtmlTextView(t, bold, html);
        }

        // Description

        t = view.findViewById(R.id.event_detail_content_description_view);
        if (TextUtils.isEmpty(lecture.description)) {
            t.setVisibility(View.GONE);
        } else {
            String html = StringUtils.getHtmlLinkFromMarkdown(lecture.description);
            setUpHtmlTextView(t, regular, html);
        }

        // Links

        TextView l = view.findViewById(R.id.event_detail_content_links_section_view);
        t = view.findViewById(R.id.event_detail_content_links_view);
        if (TextUtils.isEmpty(lecture.links)) {
            l.setVisibility(View.GONE);
            t.setVisibility(View.GONE);
        } else {
            l.setTypeface(bold);
            MyApp.LogDebug(LOG_TAG, "show links");
            l.setVisibility(View.VISIBLE);
            String links = lecture.links.replaceAll("\\),", ")<br>");
            links = StringUtils.getHtmlLinkFromMarkdown(links);
            setUpHtmlTextView(t, regular, links);
        }

        // Event online

        final TextView eventOnlineSection = view.findViewById(R.id.event_detail_content_event_online_section_view);
        eventOnlineSection.setTypeface(bold);
        final TextView eventOnlineLink = view.findViewById(R.id.event_detail_content_event_online_view);
        if (WikiEventUtils.containsWikiLink(lecture.links)) {
            eventOnlineSection.setVisibility(View.GONE);
            eventOnlineLink.setVisibility(View.GONE);
        } else {
            String eventUrl = new EventUrlComposer(lecture).getEventUrl();
            if (eventUrl.isEmpty()) {
                eventOnlineSection.setVisibility(View.GONE);
                eventOnlineLink.setVisibility(View.GONE);
            } else {
                eventOnlineSection.setVisibility(View.VISIBLE);
                eventOnlineLink.setVisibility(View.VISIBLE);
                String eventLink = "<a href=\"" + eventUrl + "\">" + eventUrl + "</a>";
                setUpHtmlTextView(eventOnlineLink, regular, eventLink);
            }
        }

        activity.invalidateOptionsMenu();
    }

    private void setUpTextView(@NonNull TextView textView,
                               @NonNull Typeface typeface,
                               @NonNull String text) {
        textView.setTypeface(typeface);
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
    }


    private void setUpHtmlTextView(@NonNull TextView textView,
                                   @NonNull Typeface typeface,
                                   @NonNull String text) {
        textView.setTypeface(typeface);
        textView.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
        textView.setLinkTextColor(ContextCompat.getColor(textView.getContext(), R.color.text_link_color));
        textView.setMovementMethod(new LinkMovementMethod());
        textView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.detailmenu, menu);
        MenuItem item;
        Lecture lecture;
        try {
            lecture = observableLecture.getValue();
        } catch (IllegalStateException e) {
            // Don't worry. Lecture has just not been loaded from database yet.
            return;
        }
        if (lecture.highlight) {
            item = menu.findItem(R.id.menu_item_flag_as_favorite);
            if (item != null) {
                item.setVisible(false);
            }
            item = menu.findItem(R.id.menu_item_unflag_as_favorite);
            if (item != null) {
                item.setVisible(true);
            }
        }
        if (lecture.hasAlarm) {
            item = menu.findItem(R.id.menu_item_set_alarm);
            if (item != null) {
                item.setVisible(false);
            }
            item = menu.findItem(R.id.menu_item_delete_alarm);
            if (item != null) {
                item.setVisible(true);
            }
        }
        item = menu.findItem(R.id.menu_item_feedback);
        String feedbackUrl = new FeedbackUrlComposer(lecture, SCHEDULE_FEEDBACK_URL).getFeedbackUrl();
        if (SHOW_FEEDBACK_MENU_ITEM && !TextUtils.isEmpty(feedbackUrl)) {
            if (item != null) {
                item.setVisible(true);
            }
        } else {
            if (item != null) {
                item.setVisible(false);
            }
        }
        if (sidePane) {
            item = menu.findItem(R.id.menu_item_close_event_details);
            if (item != null) {
                item.setVisible(true);
            }
        }
        item = menu.findItem(R.id.menu_item_navigate);
        if (item != null) {
            boolean isVisible = !getRoomConvertedForC3Nav().isEmpty();
            item.setVisible(isVisible);
        }
        if (BuildConfig.ENABLE_CHAOSFLIX_EXPORT) {
            item = menu.findItem(R.id.menu_item_share_event_menu);
        } else {
            item = menu.findItem(R.id.menu_item_share_event);
        }
        if (item != null) {
            item.setVisible(true);
        }
    }

    @NonNull
    private String getRoomConvertedForC3Nav() {
        Lecture lecture = observableLecture.getValue();
        return RoomForC3NavConverter.convert(lecture.room);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EVENT_DETAIL_FRAGMENT_REQUEST_CODE &&
                resultCode == AlarmTimePickerFragment.ALERT_TIME_PICKED_RESULT_CODE) {
            int alarmTimesIndex = data.getIntExtra(
                    AlarmTimePickerFragment.ALARM_PICKED_INTENT_KEY, 0);
            onAlarmTimesIndexPicked(alarmTimesIndex);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showAlarmTimePicker() {
        AlarmTimePickerFragment.show(this, EVENT_DETAIL_FRAGMENT_REQUEST_CODE);
    }

    private void onAlarmTimesIndexPicked(int alarmTimesIndex) {
        Activity activity = requireActivity();
        Lecture lecture = observableLecture.getValue();
        FahrplanMisc.addAlarm(activity, appRepository, lecture, alarmTimesIndex);
        refreshUI(activity);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Lecture lecture = observableLecture.getValue();
        Activity activity = requireActivity();
        switch (item.getItemId()) {
            case R.id.menu_item_feedback: {
                String feedbackUrl = new FeedbackUrlComposer(lecture, SCHEDULE_FEEDBACK_URL).getFeedbackUrl();
                Uri uri = Uri.parse(feedbackUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            }
            case R.id.menu_item_share_event:
            case R.id.menu_item_share_event_text:
                String formattedLecture = SimpleLectureFormat.format(lecture);
                if (!LectureSharer.shareSimple(activity, formattedLecture)) {
                    Toast.makeText(activity, R.string.share_error_activity_not_found, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.menu_item_share_event_json:
                String jsonLecture = JsonLectureFormat.format(lecture);
                if (!LectureSharer.shareJson(activity, jsonLecture)) {
                    Toast.makeText(activity, R.string.share_error_activity_not_found, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.menu_item_add_to_calendar:
                CalendarSharing.addToCalendar(lecture, activity);
                return true;
            case R.id.menu_item_flag_as_favorite:
                lecture.highlight = true;
                appRepository.updateHighlight(lecture);
                appRepository.updateLecturesLegacy(lecture);
                refreshUI(activity);
                return true;
            case R.id.menu_item_unflag_as_favorite:
                lecture.highlight = false;
                appRepository.updateHighlight(lecture);
                appRepository.updateLecturesLegacy(lecture);
                refreshUI(activity);
                return true;
            case R.id.menu_item_set_alarm:
                showAlarmTimePicker();
                return true;
            case R.id.menu_item_delete_alarm:
                FahrplanMisc.deleteAlarm(activity, appRepository, lecture);
                refreshUI(activity);
                return true;
            case R.id.menu_item_close_event_details:
                closeFragment(activity, FRAGMENT_TAG);
                return true;
            case R.id.menu_item_navigate:
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(BuildConfig.C3NAV_URL + getRoomConvertedForC3Nav()));
                startActivity(intent);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    // TODO Remove once FahrplanFragment observes AppRepository lecture data.
    private void refreshUI(@NonNull Activity activity) {
        activity.invalidateOptionsMenu();
        activity.setResult(Activity.RESULT_OK);
        if (activity instanceof FahrplanFragment.OnRefreshEventMarkers) {
            ((FahrplanFragment.OnRefreshEventMarkers) activity).refreshEventMarkers();
        }
    }

    private void closeFragment(@NonNull Activity activity, @NonNull String fragmentTag) {
        if (activity instanceof OnSidePaneCloseListener) {
            ((OnSidePaneCloseListener) activity).onSidePaneClose(fragmentTag);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MyApp.LogDebug(LOG_TAG, "onDestroy");
    }
}
