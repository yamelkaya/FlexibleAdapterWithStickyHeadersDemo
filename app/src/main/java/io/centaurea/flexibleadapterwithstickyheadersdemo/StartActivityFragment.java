package io.centaurea.flexibleadapterwithstickyheadersdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import org.joda.time.DateTime;

import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;

/**
 * A placeholder fragment containing a simple view.
 */
public class StartActivityFragment extends Fragment {

    private AgendaFlexibleAdapter adapter;
    private RecyclerView items;
    private View loadItemsButton;
    private View addItemButton;

    public StartActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        DatePicker datePicker = (DatePicker) getView().findViewById(R.id.date);
        DateTime now = DateTime.now().withTimeAtStartOfDay();
        datePicker.init(now.getYear(),now.getMonthOfYear() - 1, now.getDayOfMonth(),(picker,y,m,d) -> onDateChanged(picker,y,m,d));

        items = (RecyclerView) getView().findViewById(R.id.items);
        items.setLayoutManager(new SmoothScrollLinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new AgendaFlexibleAdapter(new TaskDbProvider());
        items.setAdapter(adapter);
        adapter.reloadItems(new DateTime());

        loadItemsButton = getView().findViewById(R.id.loadItemsButton);
        loadItemsButton.setOnClickListener(v -> onLoadItemsClick(v));
    }

    private void onLoadItemsClick(View v) {
        adapter.reloadItems(DateTime.now().withTimeAtStartOfDay());
    }

    private void onDateChanged(DatePicker picker, int y, int m, int d) {
        adapter.reloadItems(new DateTime(y,m + 1,d,0,0));
        int position = adapter.getGlobalPositionOfHeader(new DateTime(y,m + 1,d,0,0));
        SmoothScrollLinearLayoutManager layoutManager = (SmoothScrollLinearLayoutManager) items.getLayoutManager();

        layoutManager.smoothScrollToPosition(items,null,position);
    }
}
