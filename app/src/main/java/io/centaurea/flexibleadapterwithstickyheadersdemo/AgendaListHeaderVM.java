package io.centaurea.flexibleadapterwithstickyheadersdemo;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.databinding.library.baseAdapters.BR;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractHeaderItem;
import eu.davidea.viewholders.FlexibleViewHolder;

public class AgendaListHeaderVM extends AbstractHeaderItem<AgendaListHeaderVM.HeaderViewHolder> {

    //TODO:REF refactor to using RecycleView.ViewHolder instead of FlexibleViewHolder when FlexibleAdapter release will be updated
    public static class HeaderViewHolder extends FlexibleViewHolder {

        private final ViewDataBinding _binding;

        public HeaderViewHolder(View view, FlexibleAdapter adapter, boolean stickyHeader) {
            super(view, adapter,stickyHeader);
            _binding = DataBindingUtil.bind(view);
        }
    }

    private final Date date;
    private DateTime dateTime;
    private String label;
    private boolean empty;

    public String getLabel(){
        return label;
    }

    public DateTime getDateTime(){
        return dateTime;
    }

    public AgendaListHeaderVM(Date date,boolean empty) {
        this.dateTime = new DateTime(date).withTimeAtStartOfDay();
        this.date = dateTime.toDate();
        this.label = dateTime.toString("EEEE, MMMM d").toUpperCase();
        this.empty = empty;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof AgendaListHeaderVM){
            return this.date.equals(((AgendaListHeaderVM) o).date);
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return empty ? R.layout.view_agenda_empty_day_header : R.layout.view_agenda_day_header;
        //return R.layout.view_agenda_day_header;
    }

    @Override
    public HeaderViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, getLayoutRes(), parent, false);
        return new HeaderViewHolder(binding.getRoot(), adapter,true);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, HeaderViewHolder holder, int position, List payloads) {
        holder._binding.setVariable(BR.item,this);
    }
}
