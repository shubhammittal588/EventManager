package nerd.tuxmobil.fahrplan.congress.favorites;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import info.metadude.android.eventfahrplan.commons.logging.Logging;
import info.metadude.android.eventfahrplan.commons.temporal.Moment;
import kotlin.Unit;
import nerd.tuxmobil.fahrplan.congress.MyApp;
import nerd.tuxmobil.fahrplan.congress.R;
import nerd.tuxmobil.fahrplan.congress.base.AbstractListFragment;
import nerd.tuxmobil.fahrplan.congress.commons.ObservableType;
import nerd.tuxmobil.fahrplan.congress.contract.BundleKeys;
import nerd.tuxmobil.fahrplan.congress.models.Lecture;
import nerd.tuxmobil.fahrplan.congress.models.Meta;
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepository;
import nerd.tuxmobil.fahrplan.congress.schedule.MainActivity;
import nerd.tuxmobil.fahrplan.congress.sharing.LectureSharer;
import nerd.tuxmobil.fahrplan.congress.sharing.SimpleLectureFormat;
import nerd.tuxmobil.fahrplan.congress.utils.ActivityHelper;
import nerd.tuxmobil.fahrplan.congress.utils.ConfirmationDialog;

import static java.util.Collections.emptyList;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link AbstractListFragment.OnLectureListClick}
 * interface.
 */
public class StarredListFragment extends AbstractListFragment implements AbsListView
        .MultiChoiceModeListener {

    private static final String LOG_TAG = "StarredListFragment";
    public static final String FRAGMENT_TAG = "starred";
    private OnLectureListClick mListener;
    private ObservableType<List<? extends Lecture>> observableStarredLectures =
            new ObservableType<>(Logging.Companion.get());
    private AppRepository.StarredLecturesUpdateListener starredLecturesUpdateListener =
            lectures -> observableStarredLectures.setValue(lectures);
    private boolean sidePane = false;

    public static final int DELETE_ALL_FAVORITES_REQUEST_CODE = 19126;

    /**
     * The fragment's ListView/GridView.
     */
    private ListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private LectureArrayAdapter mAdapter;

    public static StarredListFragment newInstance(boolean sidePane) {
        StarredListFragment fragment = new StarredListFragment();
        Bundle args = new Bundle();
        args.putBoolean(BundleKeys.SIDEPANE, sidePane);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public StarredListFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            sidePane = args.getBoolean(BundleKeys.SIDEPANE);
        }
        setHasOptionsMenu(true);

        Context context = requireContext();
        Meta meta = appRepository.readMeta();
        mAdapter = new LectureArrayAdapter(context, emptyList(), meta.getNumDays());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Context contextThemeWrapper = new ContextThemeWrapper(requireContext(),
                R.style.Theme_AppCompat_Light);

        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
        View view;
        View header;
        if (sidePane) {
            view = localInflater.inflate(R.layout.fragment_favorites_list_narrow, container, false);
            mListView = view.findViewById(android.R.id.list);
            header = localInflater.inflate(R.layout.starred_header, null, false);
        } else {
            view = localInflater.inflate(R.layout.fragment_favorites_list, container, false);
            mListView = view.findViewById(android.R.id.list);
            header = localInflater.inflate(R.layout.header_empty, null, false);
        }
        mListView.addHeaderView(header, null, false);
        mListView.setHeaderDividersEnabled(false);
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(this);

        mListView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appRepository.addStarredLecturesUpdateListener(starredLecturesUpdateListener);
    }

    @Override
    public void onDestroyView() {
        appRepository.removeStarredLecturesUpdateListener(starredLecturesUpdateListener);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        jumpOverPastLectures();
    }

    private void jumpOverPastLectures() {
        List<? extends Lecture> starredLectures = observableStarredLectures.getValue();
        if (starredLectures.isEmpty()) return;
        long nowMillis = new Moment().toMilliseconds();

        int i;
        int numSeparators = 0;
        for (i = 0; i < starredLectures.size(); i++) {
            Lecture lecture = starredLectures.get(i);
            if (lecture.dateUTC + lecture.duration * 60000 > nowMillis) {
                numSeparators = lecture.day;
                break;
            }
        }
        if (i > 0 && i < starredLectures.size()) {
            mListView.setSelection(i + 1 + numSeparators);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        observableStarredLectures.addObserver(() -> {
            updateAdapter();
            return Unit.INSTANCE;
        });
        try {
            mListener = (OnLectureListClick) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnLectureListClick");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        observableStarredLectures.deleteObservers();
        mListener = null;
    }

    public void onRefresh() {
        // TODO Remove once MainActivity#refreshFavoriteList is migrated.
    }

    private void updateAdapter() {
        Context context = requireContext();
        Meta meta = appRepository.readMeta();
        List<? extends Lecture> starredLectures = observableStarredLectures.getValue();
        mAdapter = new LectureArrayAdapter(context, starredLectures, meta.getNumDays());
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MyApp.LogDebug(LOG_TAG, "onItemClick");
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            position--;
            List<? extends Lecture> starredLectures = observableStarredLectures.getValue();
            Lecture clicked = starredLectures.get(mAdapter.getItemIndex(position));
            mListener.onLectureListClick(clicked, false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.starred_list_menu, menu);
        MenuItem item = menu.findItem(R.id.menu_item_delete_all_favorites);
        List<? extends Lecture> starredLectures = observableStarredLectures.getValue();
        if (item != null && starredLectures.isEmpty()) {
            item.setVisible(false);
        }
        item = menu.findItem(R.id.menu_item_share_favorites);
        if (item != null) {
            item.setVisible(!starredLectures.isEmpty());
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_share_favorites:
                shareLectures();
                return true;
            case R.id.menu_item_delete_all_favorites:
                askToDeleteAllFavorites();
                return true;
            case android.R.id.home:
                return ActivityHelper.navigateUp(requireActivity());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.starred_list_context_menu, menu);
        mode.setTitle(getString(R.string.choose_to_delete));
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_delete_favorite:
                deleteSelectedItems(mListView.getCheckedItemPositions());
                Activity activity = requireActivity();
                activity.invalidateOptionsMenu();
                refreshViews(activity);
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    private void refreshViews(@NonNull Activity activity) {
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).refreshEventMarkers();
        }
        mAdapter.notifyDataSetChanged();
        activity.setResult(Activity.RESULT_OK);
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }

    private void askToDeleteAllFavorites() {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(ConfirmationDialog.FRAGMENT_TAG);
        if (fragment == null) {
            ConfirmationDialog confirm = ConfirmationDialog.newInstance(
                    R.string.dlg_delete_all_favorites,
                    DELETE_ALL_FAVORITES_REQUEST_CODE);
            confirm.show(fm, ConfirmationDialog.FRAGMENT_TAG);
        }
    }

    @SuppressWarnings("java:S5413")
    public void deleteAllFavorites() {
        MyApp.LogDebug(LOG_TAG, "deleteAllFavorites");
        List<? extends Lecture> starredLectures = observableStarredLectures.getValue();
        if (starredLectures.isEmpty()) {
            return;
        }
        appRepository.deleteAllHighlights();
        // TODO Remove once the FahrplanFragment is wired with the AppRepository.
        int count = starredLectures.size();
        for (int i = 0; i < count; i++) {
            // Always pop the current top most starred lecture.
            Lecture starredLecture = starredLectures.get(0);
            starredLecture.highlight = false;
            appRepository.updateLecturesLegacy(starredLecture);
            starredLectures.remove(0);
        }
        Activity activity = requireActivity();
        activity.invalidateOptionsMenu();
        refreshViews(activity);
    }

    private void deleteSelectedItems(SparseBooleanArray checkedItemPositions) {
        if (checkedItemPositions.size() == 0) {
            return;
        }
        List<? extends Lecture> starredLectures = observableStarredLectures.getValue();
        if (starredLectures.isEmpty()) {
            return;
        }
        List<Lecture> zombies = new ArrayList<>(checkedItemPositions.size());
        for (int id = mListView.getAdapter().getCount() - 1; id >= 0; id--) {
            if (checkedItemPositions.get(id)) {
                int index = mAdapter.getItemIndex(id - 1);
                Lecture zombieLecture = starredLectures.get(index);
                zombieLecture.highlight = false;
                zombies.add(zombieLecture);
            }
        }
        appRepository.updateHighlights(zombies);
        if (MyApp.lectureList != null) {
            return;
        }
        // TODO Remove once the FahrplanFragment is wired with the AppRepository.
        for (Lecture zombie : zombies) {
            //noinspection ConstantConditions
            for (Lecture lecture : MyApp.lectureList) {
                if (lecture.lectureId.equals(zombie.lectureId)) {
                    lecture.highlight = false;
                    break;
                }
            }
        }
    }

    private void shareLectures() {
        List<Lecture> lectures = new ArrayList<>(observableStarredLectures.getValue());
        String formattedLectures = SimpleLectureFormat.format(lectures);
        if (formattedLectures != null) {
            Context context = requireContext();
            if (!LectureSharer.shareSimple(context, formattedLectures)) {
                Toast.makeText(context, R.string.share_error_activity_not_found, Toast.LENGTH_SHORT).show();
            }
        }
    }

}
