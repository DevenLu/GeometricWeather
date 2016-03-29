package wangdaye.com.geometricweather.View;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import wangdaye.com.geometricweather.Data.MyDatabaseHelper;
import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.Widget.LocationItem;
import wangdaye.com.geometricweather.Widget.LocationItemAdapter;
import wangdaye.com.geometricweather.Widget.MyItemClickListener;

/**
 * A dialog to show and manage locations.
 * */

public class ManageDialog extends DialogFragment
        implements MyItemClickListener {
    // widget
    private EditText searchEditText;

    private LocationItemAdapter locationItemAdapter;

    private SetLocationListener setLocationListener;

    private MyDatabaseHelper databaseHelper;

// life cycle

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_manage, null);
        builder.setView(view);

        this.initData();
        this.initWidget(view);

        return builder.create();
    }

// initialize

    private void initData() {
        List<LocationItem> locationItemList = new ArrayList<>();
        for (int i = 0; i < MainActivity.locationList.size(); i ++) {
            locationItemList.add(new LocationItem(MainActivity.locationList.get(i).location));
        }
        this.locationItemAdapter = new LocationItemAdapter(locationItemList);
        this.locationItemAdapter.setOnItemClickListener(this);
    }

    private void initWidget(View view) {
        this.searchEditText = (EditText) view.findViewById(R.id.dialog_search_editText);
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    SetLocationListener setLocationListener = (SetLocationListener) getActivity();
                    String location = v.getText().toString();
                    searchEditText.setText("");
                    if (location.length() > 0) {
                        setLocationListener.onSetLocation(location, true);
                    } else {
                        setLocationListener.onSetLocation(getString(R.string.search_null), true);
                    } dismiss();
                }
                return false;
            }
        });

        RecyclerView locationView = (RecyclerView) view.findViewById(R.id.dialog_location_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        locationView.setLayoutManager(layoutManager);
        locationView.setAdapter(this.locationItemAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT));
        itemTouchHelper.attachToRecyclerView(locationView);
    }

// interface

    public void addLocationListener(SetLocationListener listener) {
        this.setLocationListener = listener;
    }

    @Override
    public void onItemClick(View view, int position) {
        setLocationListener.onSetLocation(MainActivity.locationList.get(position).location, false);
        dismiss();
    }

    public interface SetLocationListener
    {
        void onSetLocation(String location, boolean siSearch);
    }

// call back

    public class SwipeCallback extends ItemTouchHelper.SimpleCallback {

        public SwipeCallback(int dragDirs, int swipeDirs) {
            super(dragDirs, swipeDirs);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView,
                              RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            MainActivity.locationList.clear();
            MainActivity.locationList = locationItemAdapter.moveData(fromPosition, toPosition);

            initDatabaseHelper();
            SQLiteDatabase database = databaseHelper.getWritableDatabase();
            database.delete(MyDatabaseHelper.TABLE_LOCATION, null, null);
            ContentValues values = new ContentValues();
            for(int i = 0; i < MainActivity.locationList.size(); i ++) {
                values.put(MyDatabaseHelper.COLUMN_LOCATION, MainActivity.locationList.get(i).location);
                database.insert(MyDatabaseHelper.TABLE_LOCATION, null, values);
                values.clear();
            }
            database.close();

            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();

            if (MainActivity.locationList.size() < 2) {
                locationItemAdapter.removeData(position);
                locationItemAdapter.insertData(new LocationItem(MainActivity.locationList.get(position).location), position);
                Toast.makeText(getActivity(),
                        getString(R.string.location_list_cannot_be_null),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String location = MainActivity.locationList.get(position).location;
            initDatabaseHelper();
            deleteLocation(location);

            MainActivity.locationList.remove(position);
            locationItemAdapter.removeData(position);

            Toast.makeText(getActivity(),
                    getString(R.string.delete_succeed),
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onChildDraw(Canvas c,
                                RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder,
                                float dX,
                                float dY,
                                int actionState,
                                boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                final float alpha = 1 - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
                viewHolder.itemView.setAlpha(alpha);
                viewHolder.itemView.setTranslationX(dX);
            } else if(actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder.itemView.setAlpha(0.7f);
            }
        }
    }

// database

    private void initDatabaseHelper() {
        this.databaseHelper = new MyDatabaseHelper(getActivity(),
                MyDatabaseHelper.DATABASE_NAME,
                null,
                MyDatabaseHelper.VERSION_CODE);
    }

    private void deleteLocation(String location) {
        SQLiteDatabase database = this.databaseHelper.getWritableDatabase();
        String[] locationList = new String[] {location};
        database.delete(
                MyDatabaseHelper.TABLE_LOCATION,
                MyDatabaseHelper.COLUMN_LOCATION + " = ?",
                locationList);
        database.close();
    }
}
